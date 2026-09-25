package app.eruditio.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.eruditio.AppContainer
import app.eruditio.core.Entitlement
import app.eruditio.core.Gleaning
import app.eruditio.core.LedgerEntry
import app.eruditio.core.MasteryLevel
import app.eruditio.core.PendingGleaning
import app.eruditio.core.RecallGrade
import app.eruditio.core.SessionRecord
import app.eruditio.core.SessionStart
import app.eruditio.core.TrialGate
import app.eruditio.core.WeekDot
import app.eruditio.intercept.TargetApp
import app.eruditio.intercept.TargetApps
import app.eruditio.ui.theme.Palette
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EruditioViewModel(private val container: AppContainer) : ViewModel() {

    init {
        if (!app.eruditio.BuildConfig.DEBUG) container.billing.connect()
    }

    val texts = container.library.texts

    private val homeState = MutableStateFlow(HomeState())
    val home: StateFlow<HomeState> = homeState

    val palette: StateFlow<Palette> = container.themes.palette

    fun selectTheme(palette: Palette) = container.themes.select(palette)

    /** Ticket 15: togglable margin rail in the reader, persisted on-device. */
    val railEnabled: StateFlow<Boolean> = container.reading.railEnabled

    fun setRailEnabled(enabled: Boolean) = container.reading.setRailEnabled(enabled)

    /** The one unlock rule (TrialGate.isUnlocked), observed reactively so a
     *  completed purchase unlocks the Home card without a manual refresh. */
    val gateLocked: StateFlow<Boolean> =
        (if (app.eruditio.BuildConfig.DEBUG) flowOf(Entitlement(owned = true))
        else container.entitlements.observe())
            .map { !TrialGate.isUnlocked(it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, app.eruditio.BuildConfig.DEBUG)

    private val gateErrorState = MutableStateFlow<String?>(null)
    val gateError: StateFlow<String?> = gateErrorState

    /** Which target apps get intercepted (ticket 04: user-editable with defaults). */
    val targets: List<TargetApp> = TargetApps.defaults()
    private val selectedTargetState = MutableStateFlow(TargetApps.selected(container.appContext).map { it.packageId }.toSet())
    val selectedTargets: StateFlow<Set<String>> = selectedTargetState

    fun toggleTarget(packageId: String) {
        val current = selectedTargetState.value
        val next = if (packageId in current) current - packageId else current + packageId
        TargetApps.save(container.appContext, next)
        selectedTargetState.value = next
    }

    private val reviewState = MutableStateFlow(ReviewState())
    val review: StateFlow<ReviewState> = reviewState

    private val gleaningState = MutableStateFlow<List<Gleaning>>(emptyList())
    val gleanings: StateFlow<List<Gleaning>> = gleaningState

    private val progressState = MutableStateFlow(ProgressState())
    val progress: StateFlow<ProgressState> = progressState

    private val textProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val progressById: StateFlow<Map<String, Int>> = textProgress

    private var refreshJob: Job? = null

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch { refreshOnce() }
    }

    /** One ordered snapshot; an older refresh cannot publish after a newer one. */
    private suspend fun refreshOnce() {
        val review = reviewSnapshot()
        val savedGleanings = container.gleanings.all()
        val pendingGleaning = container.gleanings.latestPending()
        // Only surface a hand-off whose session actually completed; a row left by
        // a crash between hand-off and completion must not nag on Today.
        val pendingIsComplete = pendingGleaning?.let { container.sessions.sessionFor(it.sessionId)?.isCompleted == true }
        val gleaningCount = container.gleanings.count()
        val progress = container.progressById()
        val recent = container.sessions.recentSessions(5)
        val sessionsThisWeek = container.sessions.weeklySessions()
        val minutesToday = container.sessions.minutesToday()
        val resumeTextId = container.sessions.resumeTextId()
        val mastery = container.sessions.masteryCounts()
        val (weekMinutes, totalMinutes) = container.sessions.readingMinutes()
        val textsWithProgress = texts.count { isStarted(progress[it.id] ?: 0) }
        val readThrough = texts.count { isReadThrough(progress[it.id] ?: 0) }
        val reviewItems = review.items.take(3).map { entry ->
            HomeReviewItem(
                conceptId = entry.conceptId,
                cue = container.concepts.cueFor(entry.conceptId),
                mastery = container.sessions.masteryOf(entry),
            )
        }
        val head = review.items.firstOrNull()
        val headCue = head?.let { container.concepts.cueFor(it.conceptId) }
        val weekly = container.sessions.weeklyInterceptions()
        val replaced = container.sessions.weeklyReplaced()
        val streak = container.sessions.currentStreak()
        val weekDots = container.sessions.weekDots()
        currentCoroutineContext().ensureActive()

        textProgress.value = progress
        gleaningState.value = savedGleanings
        homeState.value = HomeState(
            due = review.items.size,
            recent = recent,
            hasUsageAccess = container.hasUsageAccess,
            canDrawOverlays = container.canDrawOverlays,
            sessionsThisWeek = sessionsThisWeek,
            minutesToday = minutesToday,
            resumeTextId = resumeTextId,
            reviewItems = reviewItems,
            reviewIsChallenge = review.isChallenge,
            pendingGleaning = pendingGleaning?.takeIf { pendingIsComplete == true },
        )
        reviewState.value = ReviewState(
            current = head,
            cue = headCue,
            index = 0,
            total = review.items.size,
            isChallenge = review.isChallenge,
        )
        progressState.value = ProgressState(
            textsInProgress = textsWithProgress,
            textsReadThrough = readThrough,
            minutesReadingWeek = weekMinutes,
            minutesReadingTotal = totalMinutes,
            familiar = mastery.familiar,
            proficient = mastery.proficient,
            mastered = mastery.mastered,
            dueCount = review.dueCount,
            gleaningCount = gleaningCount,
            weekly = weekly,
            replaced = replaced,
            streak = streak,
            weekDots = weekDots,
        )
    }

    /** Billing flow; debug builds bypass the gate entirely. */
    fun purchase(activity: Activity) {
        viewModelScope.launch {
            gateErrorState.value = null
            container.billing.launchPurchase(activity) { message -> gateErrorState.value = message }
        }
    }

    fun restorePurchase() {
        viewModelScope.launch {
            gateErrorState.value = null
            container.billing.restore()
        }
    }

    /** Review-tab serving rule: due items first; otherwise surface a challenge. */
    private suspend fun reviewSnapshot(): ReviewSnapshot {
        val due = container.sessions.dueToday()
        val items = due.ifEmpty { container.sessions.masteryChallengeToday() }
        return ReviewSnapshot(
            dueCount = due.size,
            items = items,
            isChallenge = due.isEmpty() && items.isNotEmpty(),
        )
    }

    fun titleOf(textId: String): String = container.library.meta(textId)?.title ?: textId

    fun titleOfConcept(conceptId: String): String = titleOf(conceptId.substringBefore('#'))

    /** Reader pause path (ticket 14): the shell reads and consumes this to land on Home/Today. */
    val pauseToHome: Boolean get() = container.pauseToHome.value

    fun consumePauseToHome() {
        container.pauseToHome.value = false
    }

    fun startReading(textId: String) {
        viewModelScope.launch {
            when (val start = container.sessions.openSession(textId)) {
                is SessionStart.Opened -> openReader(container.appContext, start.sessionId)
                SessionStart.Locked -> refresh() // trial over: the purchase card surfaces on Home
            }
        }
    }

    fun grade(grade: RecallGrade) {
        refreshJob?.cancel()
        viewModelScope.launch {
            val current = reviewState.value.current ?: return@launch
            container.sessions.gradeConcept(current.conceptId, grade)
            val snapshot = reviewSnapshot()
            val head = snapshot.items.firstOrNull()
            reviewState.value = ReviewState(
                current = head,
                cue = head?.let { container.concepts.cueFor(it.conceptId) },
                index = reviewState.value.index + 1,
                total = snapshot.items.size,
                isChallenge = snapshot.isChallenge,
            )
        }
    }
}

/** Reader is its own quiet activity; both entry paths (home, pause) land here. */
fun openReader(context: Context, sessionId: Long) {
    context.startActivity(
        Intent(context, ReaderActivity::class.java)
            .putExtra(ReaderActivity.EXTRA_SESSION_ID, sessionId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

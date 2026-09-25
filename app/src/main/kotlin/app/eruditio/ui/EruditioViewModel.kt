package app.eruditio.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.eruditio.AppContainer
import app.eruditio.core.Entitlement
import app.eruditio.core.LedgerEntry
import app.eruditio.core.MasteryLevel
import app.eruditio.core.RecallGrade
import app.eruditio.core.SessionRecord
import app.eruditio.core.SessionStart
import app.eruditio.core.TrialGate
import app.eruditio.core.WeekDot
import app.eruditio.intercept.TargetApp
import app.eruditio.intercept.TargetApps
import app.eruditio.ui.theme.Palette
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

    data class HomeReviewItem(
        val conceptId: String,
        val cue: String?,
        val mastery: MasteryLevel,
    )

    data class HomeState(
        /** Due cards plus mastery challenges when the due set is empty. */
        val due: Int = 0,
        val recent: List<SessionRecord> = emptyList(),
        val hasUsageAccess: Boolean = false,
        val canDrawOverlays: Boolean = false,
        val sessionsThisWeek: Int = 0,
        val minutesToday: Long = 0L,
        val resumeTextId: String? = null,
        val reviewItems: List<HomeReviewItem> = emptyList(),
        val reviewIsChallenge: Boolean = false,
    )

    data class ReviewState(
        val current: LedgerEntry? = null,
        val cue: String? = null,
        val index: Int = 0,
        val total: Int = 0,
        /** True when the served cards are the mastery challenge (due set empty). */
        val isChallenge: Boolean = false,
    )

    data class ProgressState(
        val textsInProgress: Int = 0,
        val textsReadThrough: Int = 0,
        val minutesReadingWeek: Long = 0L,
        val minutesReadingTotal: Long = 0L,
        val familiar: Int = 0,
        val proficient: Int = 0,
        val mastered: Int = 0,
        val dueCount: Int = 0,
        val weekly: Int = 0,
        val replaced: Int = 0,
        val streak: Int = 0,
        /** Trailing 7 local days, oldest first (ticket 19 week strip). */
        val weekDots: List<WeekDot> = emptyList(),
    )

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

    private val progressState = MutableStateFlow(ProgressState())
    val progress: StateFlow<ProgressState> = progressState

    private val textProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val progressById: StateFlow<Map<String, Int>> = textProgress

    fun refresh() {
        viewModelScope.launch {
            val dueCards = container.sessions.dueToday()
            val challengeCards = if (dueCards.isEmpty()) {
                container.sessions.masteryChallengeToday()
            } else {
                emptyList()
            }
            val servedHomeCards = dueCards.ifEmpty { challengeCards }
            homeState.value = HomeState(
                due = servedHomeCards.size,
                recent = container.sessions.recentSessions(5),
                hasUsageAccess = container.hasUsageAccess,
                canDrawOverlays = container.canDrawOverlays,
                sessionsThisWeek = container.sessions.weeklySessions(),
                minutesToday = container.sessions.minutesToday(),
                resumeTextId = container.sessions.resumeTextId(),
                reviewItems = servedHomeCards.take(3).map { entry ->
                    HomeReviewItem(
                        conceptId = entry.conceptId,
                        cue = container.concepts.cueFor(entry.conceptId),
                        mastery = container.sessions.masteryOf(entry),
                    )
                },
                reviewIsChallenge = dueCards.isEmpty() && servedHomeCards.isNotEmpty(),
            )
            textProgress.value = container.progressById()
            refreshReview()
            refreshProgress()
        }
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

    /** Review-tab serving rule (spec story 14): due cards first; when the due
     *  set is empty, learned-but-aging material resurfaces as the mastery
     *  challenge instead of leaving the tab empty. One home for the rule. */
    private suspend fun servedReviewCards(): Pair<List<LedgerEntry>, Boolean> {
        val due = container.sessions.dueToday()
        val cards = due.ifEmpty { container.sessions.masteryChallengeToday() }
        return cards to (due.isEmpty() && cards.isNotEmpty())
    }

    private fun refreshReview() {
        viewModelScope.launch {
            val (cards, isChallenge) = servedReviewCards()
            val head = cards.firstOrNull()
            reviewState.value = ReviewState(
                current = head,
                cue = head?.let { container.concepts.cueFor(it.conceptId) },
                index = 0,
                total = cards.size,
                isChallenge = isChallenge,
            )
        }
    }

    private fun refreshProgress() {
        viewModelScope.launch {
            val mastery = container.sessions.masteryCounts()
            val textsWithProgress = texts.count { isStarted(progressById.value[it.id] ?: 0) }
            val readThrough = texts.count { isReadThrough(progressById.value[it.id] ?: 0) }
            // readingMinutes() returns MINUTES (the seam divides ms itself).
            val (weekMinutes, totalMinutes) = container.sessions.readingMinutes()
            progressState.value = ProgressState(
                textsInProgress = textsWithProgress,
                textsReadThrough = readThrough,
                minutesReadingWeek = weekMinutes,
                minutesReadingTotal = totalMinutes,
                familiar = mastery.familiar,
                proficient = mastery.proficient,
                mastered = mastery.mastered,
                dueCount = container.sessions.dueToday().size,
                weekly = container.sessions.weeklyInterceptions(),
                replaced = container.sessions.weeklyReplaced(),
                streak = container.sessions.currentStreak(),
                weekDots = container.sessions.weekDots(),
            )
        }
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
        viewModelScope.launch {
            val current = reviewState.value.current ?: return@launch
            container.sessions.gradeConcept(current.conceptId, grade)
            val (cards, isChallenge) = servedReviewCards()
            val head = cards.firstOrNull()
            reviewState.value = ReviewState(
                current = head,
                cue = head?.let { container.concepts.cueFor(it.conceptId) },
                index = reviewState.value.index + 1,
                total = reviewState.value.total,
                isChallenge = isChallenge,
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

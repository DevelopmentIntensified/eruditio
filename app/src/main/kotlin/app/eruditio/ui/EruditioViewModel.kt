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

    data class HomeState(
        val due: Int = 0,
        val recent: List<SessionRecord> = emptyList(),
        val hasUsageAccess: Boolean = false,
        val canDrawOverlays: Boolean = false,
        val sessionsThisWeek: Int = 0,
        val minutesToday: Long = 0L,
    )

    data class ReviewState(
        val current: LedgerEntry? = null,
        val cue: String? = null,
        val index: Int = 0,
        val total: Int = 0,
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
            homeState.value = HomeState(
                due = container.sessions.dueToday().size,
                recent = container.sessionLog.recent(5),
                hasUsageAccess = container.hasUsageAccess,
                canDrawOverlays = container.canDrawOverlays,
                sessionsThisWeek = container.sessions.weeklySessions(),
                minutesToday = container.sessions.minutesToday(),
            )
            textProgress.value = texts.associate { it.id to container.lastReadPercent(it.id) }
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

    private fun refreshReview() {
        viewModelScope.launch {
            val due = container.sessions.dueToday()
            val head = due.firstOrNull()
            reviewState.value = ReviewState(
                current = head,
                cue = head?.let { container.concepts.cueFor(it.conceptId) },
                index = 0,
                total = due.size,
            )
        }
    }

    private fun refreshProgress() {
        viewModelScope.launch {
            val all = container.ledger.all()
            val textsWithProgress = texts.count { (progressById.value[it.id] ?: 0) > 0 }
            val readThrough = texts.count { (progressById.value[it.id] ?: 0) >= 100 }
            // readingMinutes() returns MINUTES (the seam divides ms itself).
            val (weekMinutes, totalMinutes) = container.sessions.readingMinutes()
            progressState.value = ProgressState(
                textsInProgress = textsWithProgress,
                textsReadThrough = readThrough,
                minutesReadingWeek = weekMinutes,
                minutesReadingTotal = totalMinutes,
                familiar = all.count { container.scheduler.effectiveMastery(it, container.today()) == MasteryLevel.FAMILIAR },
                proficient = all.count { container.scheduler.effectiveMastery(it, container.today()) == MasteryLevel.PROFICIENT },
                mastered = all.count { container.scheduler.effectiveMastery(it, container.today()) == MasteryLevel.MASTERED },
                dueCount = container.sessions.dueToday().size,
                weekly = container.sessions.weeklyInterceptions(),
                replaced = container.interceptionLog.weeklyReplaced(container.today()),
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
            val startChar = container.positions.restore(textId)?.charOffset ?: 0L
            when (val start = container.sessions.startSession(textId, startChar)) {
                is SessionStart.Opened -> openReader(container.appContext, start.sessionId)
                SessionStart.Locked -> refresh() // trial over: the purchase card surfaces on Home
            }
        }
    }

    fun grade(grade: RecallGrade) {
        viewModelScope.launch {
            val current = reviewState.value.current ?: return@launch
            container.sessions.gradeConcept(current.conceptId, grade)
            val due = container.sessions.dueToday()
            val head = due.firstOrNull()
            reviewState.value = ReviewState(
                current = head,
                cue = head?.let { container.concepts.cueFor(it.conceptId) },
                index = reviewState.value.index + 1,
                total = reviewState.value.total,
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

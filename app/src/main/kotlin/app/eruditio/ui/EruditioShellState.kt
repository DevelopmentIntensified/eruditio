package app.eruditio.ui

import app.eruditio.core.GleaningRecovery
import app.eruditio.core.LedgerEntry
import app.eruditio.core.MasteryLevel
import app.eruditio.core.PendingGleaning
import app.eruditio.core.SessionRecord
import app.eruditio.core.WeekDot

/**
 * Snapshot value types for the shell. Split out of [EruditioViewModel] so the
 * view model holds orchestration only, not a growing pile of state records.
 */
data class HomeReviewItem(
    val conceptId: String,
    val cue: String?,
    val mastery: MasteryLevel,
)

data class HomeState(
    /** Due review items plus mastery challenges when the due set is empty. */
    val due: Int = 0,
    val recent: List<SessionRecord> = emptyList(),
    val hasUsageAccess: Boolean = false,
    val canDrawOverlays: Boolean = false,
    val sessionsThisWeek: Int = 0,
    val minutesToday: Long = 0L,
    val resumeTextId: String? = null,
    val reviewItems: List<HomeReviewItem> = emptyList(),
    val reviewIsChallenge: Boolean = false,
    /** A hand-off row whose session completed and is still awaiting Keep. */
    val pendingGleaning: PendingGleaning? = null,
    /** A completed session with no row at all — the crash-window case. */
    val recoverableGleaning: GleaningRecovery? = null,
)

data class ReviewState(
    val current: LedgerEntry? = null,
    val cue: String? = null,
    val index: Int = 0,
    val total: Int = 0,
    /** True when the served review items are the mastery challenge. */
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
    val gleaningCount: Int = 0,
    val weekly: Int = 0,
    val replaced: Int = 0,
    val streak: Int = 0,
    /** Trailing 7 local days, oldest first (ticket 19 week strip). */
    val weekDots: List<WeekDot> = emptyList(),
)

internal data class ReviewSnapshot(
    val dueCount: Int,
    val items: List<LedgerEntry>,
    val isChallenge: Boolean,
)

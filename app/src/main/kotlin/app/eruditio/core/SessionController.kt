package app.eruditio.core

/**
 * The session seam (spec): OS event/decision/session lifecycle in pure Kotlin.
 * The UsageStats watcher, the pause overlay and the UI are replaceable details
 * behind this class; fakes implement the small store interfaces below.
 */
enum class InterceptionResult { CONTINUED, REPLACED }

/** What the OS-facing watcher does when a target app reaches the foreground. */
sealed interface InterceptionDecision {
    /** Show the pause screen over this package. */
    data class Intercept(val targetPackage: String) : InterceptionDecision

    /** Friction disabled: pass straight through, record nothing. */
    data object PassThrough : InterceptionDecision
}

sealed interface SessionStart {
    data class Opened(val sessionId: Long) : SessionStart

    /** Trial over and not owned; the core loop is gated (billing ticket). */
    data object Locked : SessionStart
}

/** A completed reading session: what a consumer observes about it. */
data class CompletedSession(
    val sessionId: Long,
    val textId: String,
    val newConcepts: Int,
)

data class SessionRecord(
    val sessionId: Long,
    val textId: String,
    val startChar: Long,
    val endChar: Long?,
    val startedAtMillis: Long,
    val completedAtMillis: Long?,
    val completedEpochDay: Long?,
) {
    val isCompleted: Boolean get() = endChar != null && completedAtMillis != null
}

class SessionController(
    private val scheduler: ReviewScheduler,
    private val ledger: LedgerStore,
    private val interceptionLog: InterceptionLog,
    private val sessionLog: SessionLog,
    private val positionStore: PositionStore,
    private val concepts: ConceptSource,
    private val gate: TrialGate,
    private val clock: Clock,
) {
    var frictionEnabled: Boolean = true

    /**
     * A target app opened. Friction on -> intercept; friction off -> the user
     * goes straight in and the event is not recorded (nothing happened to record).
     */
    suspend fun onTargetAppOpened(targetPackage: String): InterceptionDecision =
        if (frictionEnabled) InterceptionDecision.Intercept(targetPackage) else InterceptionDecision.PassThrough

    /** User chose "continue into the app" on the pause screen. */
    suspend fun continueToApp(targetPackage: String) {
        interceptionLog.record(targetPackage, InterceptionResult.CONTINUED, clock.epochDay)
    }

    /** User chose "read instead": record the outcome and open a real session at the text's stored position. */
    suspend fun replaceWithSession(targetPackage: String, textId: String): SessionStart {
        interceptionLog.record(targetPackage, InterceptionResult.REPLACED, clock.epochDay)
        return openSession(textId)
    }

    /**
     * Open a reading session, resuming at the text's stored position (0 where
     * none exists). The resume-at-last-position policy lives behind PositionStore.
     */
    suspend fun openSession(textId: String): SessionStart =
        if (gate.unlocked()) {
            SessionStart.Opened(
                sessionLog.start(
                    textId,
                    positionStore.restoreOffset(textId) ?: 0L,
                    clock.epochDay,
                    clock.epochMillis,
                ),
            )
        } else {
            SessionStart.Locked
        }

    /**
     * Check-in: the segment read between the session's start position and
     * `endChar` registers a completion, credits the trial gate, and enrolls
     * the segment's concepts (new ones only — grading existing ones is the
     * user's recall, never automatic).
     */
    suspend fun completeSession(sessionId: Long, endChar: Long): CompletedSession? {
        val session = sessionLog.get(sessionId) ?: return null
        if (session.isCompleted) return null
        sessionLog.complete(sessionId, endChar, clock.epochMillis, clock.epochDay)
        gate.markSessionCompleted()
        val conceptIds = concepts.conceptsFor(session.textId, session.startChar, endChar)
        val known = ledger.conceptIds()
        var enrolled = 0
        for (id in conceptIds) {
            if (id !in known) {
                ledger.upsert(scheduler.enroll(id, clock.epochDay))
                enrolled++
            }
        }
        return CompletedSession(sessionId, session.textId, enrolled)
    }

    suspend fun dueToday(limit: Int = 10): List<LedgerEntry> =
        scheduler.due(ledger.all(), clock.epochDay, limit)

    /** User-graded recall on a due card. */
    suspend fun gradeConcept(conceptId: String, grade: RecallGrade) {
        val entry = ledger.get(conceptId) ?: return
        ledger.upsert(scheduler.review(entry, grade, clock.epochDay))
    }

    suspend fun masteryChallengeToday(limit: Int = 5): List<LedgerEntry> =
        scheduler.masteryChallenge(ledger.all(), clock.epochDay, limit)

    suspend fun currentStreak(): Int =
        Streak.current(sessionLog.completedEpochDays().toSet(), clock.epochDay)

    /** The trailing 7 local days as week-strip dots (ticket 19): filled on
     *  days with a completed session; today flagged so the UI can render it
     *  pending (gold ring) until a session completes today. */
    suspend fun weekDots(): List<WeekDot> {
        val completed = sessionLog.completedEpochDays()
        val today = clock.epochDay
        return trailingWeek.dayRange.map { day ->
            WeekDot(epochDay = day, hasCompletedSession = day in completed, today = day == today)
        }
    }

    /** The most recent completed sessions (Home's history strip). */
    suspend fun recentSessions(limit: Int = 5): List<SessionRecord> = sessionLog.recent(limit)

    /** The session record behind a reader, as a core value — never an entity. */
    suspend fun sessionFor(sessionId: Long): SessionRecord? = sessionLog.get(sessionId)

    /** Time spent in the reading surface, accumulated for the open session. */
    suspend fun addReadingTime(sessionId: Long, millis: Long) = sessionLog.addReadingTime(sessionId, millis)

    /** Rough reading progress as a percent. The caller supplies the text's
     *  character length (a library concern); the seam only owns the stored
     *  offset. 0 when unread, capped at 100. */
    suspend fun readPercent(textId: String, textLength: Int): Int {
        val offset = positionStore.restoreOffset(textId) ?: return 0
        if (offset <= 0 || textLength <= 0) return 0
        return ((offset * 100) / textLength).toInt().coerceIn(0, 100)
    }

    /** Concepts per mastery level as of today, decay applied at query time. */
    suspend fun masteryCounts(): MasteryCounts {
        val today = clock.epochDay
        val byLevel = ledger.all().groupingBy { scheduler.effectiveMastery(it, today) }.eachCount()
        return MasteryCounts(
            familiar = byLevel[MasteryLevel.FAMILIAR] ?: 0,
            proficient = byLevel[MasteryLevel.PROFICIENT] ?: 0,
            mastered = byLevel[MasteryLevel.MASTERED] ?: 0,
        )
    }

    /** The trailing week as one value, the single source of window bounds. */
    private val trailingWeek: TrailingWeek get() = TrailingWeek.of(clock)

    suspend fun weeklyInterceptions(): Int = interceptionLog.weeklyCount(trailingWeek)

    /** Of this week's interceptions, how many became reading sessions. */
    suspend fun weeklyReplaced(): Int = interceptionLog.weeklyReplaced(trailingWeek)

    /** Completed sessions in the trailing 7 days. */
    suspend fun weeklySessions(): Int = sessionLog.countCompletedSince(trailingWeek.firstEpochDay)

    /** Minutes actually spent reading, this trailing week and all time. */
    suspend fun readingMinutes(): Pair<Long, Long> =
        sessionLog.readMillisSince(trailingWeek.startOfDayMillis).millisToMinutes() to
            sessionLog.totalReadMillis().millisToMinutes()

    suspend fun minutesToday(): Long =
        sessionLog.readMillisSince(clock.startOfDayMillis()).millisToMinutes()
}

/** Floor millis to whole minutes: progress surfaces report minutes, never ms. */
private fun Long.millisToMinutes(): Long = this / 60_000L

/* ----- stores behind the seam (implemented in the data layer, faked in tests) ----- */

interface LedgerStore {
    suspend fun all(): List<LedgerEntry>
    suspend fun get(conceptId: String): LedgerEntry?
    suspend fun upsert(entry: LedgerEntry)
    suspend fun conceptIds(): Set<String>
}

interface InterceptionLog {
    suspend fun record(packageId: String, result: InterceptionResult, epochDay: Long)

    /** Openings intercepted in the trailing 7 calendar days (continued + replaced). */
    suspend fun weeklyCount(week: TrailingWeek): Int

    /** Of those, how many became reading sessions (the *replaced* path). */
    suspend fun weeklyReplaced(week: TrailingWeek): Int
}

/** The reader's resume-at-last-position policy: an offset primitive, never an entity. */
interface PositionStore {
    suspend fun restoreOffset(textId: String): Long?
}

interface SessionLog {
    suspend fun start(textId: String, startChar: Long, epochDay: Long, atMillis: Long): Long
    suspend fun get(sessionId: Long): SessionRecord?
    suspend fun complete(sessionId: Long, endChar: Long, atMillis: Long, epochDay: Long)
    suspend fun recent(limit: Int): List<SessionRecord>
    suspend fun completedEpochDays(): Set<Long>
    suspend fun countCompletedSince(fromEpochDay: Long): Int

    /** Time actually spent in the reading surface, accumulated on leave. */
    suspend fun addReadingTime(sessionId: Long, deltaMillis: Long)
    suspend fun totalReadMillis(): Long

    /** Reading time across sessions started at/after `sinceMillis`. */
    suspend fun readMillisSince(sinceMillis: Long): Long
}

/** Segment-derived review concepts (spec: review items derive from completed segments). */
interface ConceptSource {
    suspend fun conceptsFor(textId: String, startChar: Long, endChar: Long): List<String>
    suspend fun cueFor(conceptId: String): String?
}

/** Injectable time; epochDay is days-since-epoch in the device zone (data layer computes it). */
interface Clock {
    val epochDay: Long
    val epochMillis: Long

    /** Local start-of-day `daysAgo` days back, in the clock's zone. Reading-time
     *  windows bucket by local day, never by epochDay arithmetic (which would
     *  drift against real timestamps by the device's UTC offset). */
    fun startOfDayMillis(daysAgo: Int = 0): Long
}

/**
 * The trailing 7 local days, the ONE definition of that window: epochDay bounds
 * for per-day records, millis bounds for read-time sums. No consumer derives
 * `today - 6` itself; everything goes through this value.
 */
data class TrailingWeek(
    val firstEpochDay: Long,
    val todayEpochDay: Long,
    val startOfDayMillis: Long,
    val nowMillis: Long,
) {
    /** Today-6..today, inclusive; exactly 7 calendar days. */
    val dayRange: LongRange get() = firstEpochDay..todayEpochDay

    companion object {
        /** Built from the injectable clock so zone-aware day bucketing stays in one place. */
        fun of(clock: Clock): TrailingWeek = TrailingWeek(
            firstEpochDay = clock.epochDay - 6,
            todayEpochDay = clock.epochDay,
            startOfDayMillis = clock.startOfDayMillis(6),
            nowMillis = clock.epochMillis,
        )
    }
}

/** One local day in the trailing week (ticket 19): the Progress strip's per-day state. */
data class WeekDot(
    val epochDay: Long,
    val hasCompletedSession: Boolean,
    val today: Boolean,
)

object Streak {
    /**
     * Consecutive days with at least one completed session, ending today or
     * yesterday (today still counts as "in progress" until midnight).
     * Streak rewards completing sessions, never opening the app; it is stored
     * and displayed separately from mastery (spec).
     */
    fun current(completedDays: Set<Long>, todayEpochDay: Long): Int {
        var cursor = if (todayEpochDay in completedDays) todayEpochDay else todayEpochDay - 1
        var streak = 0
        while (cursor in completedDays) {
            streak++
            cursor--
        }
        return streak
    }
}

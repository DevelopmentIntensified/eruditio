package app.eruditio.core

/**
 * The retention engine at the review-scheduler seam (spec: pure logic, no Android
 * dependencies). Completion events go in; due-review sets and mastery transitions
 * come out. Deterministic everywhere: due sets sort by due day then concept id,
 * challenge sets sort by last-reviewed day then concept id.
 *
 * Mastery model (Khan-style levels over an SM-2-family interval ladder):
 *  - repetitions >= 6 -> MASTERED, >= 3 -> PROFICIENT, >= 1 -> FAMILIAR
 *  - mastery decays at query time when a concept is neglected:
 *    FAMILIAR > 30d, PROFICIENT > 45d, MASTERED > 90d since last review
 *  - re-mastering after decay is just grading well again: `review` refreshes
 *    lastEpochDay, which restores the full level; AGAIN drops the repetition count.
 */
enum class MasteryLevel { UNSEEN, FAMILIAR, PROFICIENT, MASTERED }

enum class RecallGrade { AGAIN, HARD, GOOD, EASY }

data class LedgerEntry(
    val conceptId: String,
    val repetitions: Int = 0,
    val intervalDays: Int = 0,
    val lastEpochDay: Long = 0L,
    val dueEpochDay: Long = 0L,
) {
    val isEnrolled: Boolean get() = repetitions > 0
}

class ReviewScheduler {

    /**
     * Puts a concept on the schedule. Just-read material is NOT due today;
     * the first recall challenge lands the next day.
     */
    fun enroll(conceptId: String, todayEpochDay: Long): LedgerEntry =
        LedgerEntry(
            conceptId = conceptId,
            repetitions = 0,
            intervalDays = 1,
            lastEpochDay = todayEpochDay,
            dueEpochDay = todayEpochDay + 1,
        )

    fun review(entry: LedgerEntry, grade: RecallGrade, todayEpochDay: Long): LedgerEntry =
        when (grade) {
            RecallGrade.AGAIN ->
                // Forgot it: fall back to the start of the ladder, one day out.
                LedgerEntry(entry.conceptId, 0, 1, todayEpochDay, todayEpochDay + 1)
            RecallGrade.HARD ->
                // Progress stalls but the interval survives; minimum one day out.
                LedgerEntry(
                    entry.conceptId,
                    entry.repetitions,
                    maxOf(1, entry.intervalDays),
                    todayEpochDay,
                    todayEpochDay + maxOf(1, entry.intervalDays),
                )
            RecallGrade.GOOD -> advance(entry, todayEpochDay) { reps -> intervalLadder(reps) }
            RecallGrade.EASY -> advance(entry, todayEpochDay) { reps -> easyIntervalLadder(reps) }
        }

    /** Concepts due today, oldest-due first; stable and deterministic. */
    fun due(entries: List<LedgerEntry>, todayEpochDay: Long, limit: Int = 10): List<LedgerEntry> =
        entries
            .filter { it.dueEpochDay <= todayEpochDay }
            .sortedWith(compareBy({ it.dueEpochDay }, { it.conceptId }))
            .take(limit)

    /** Ledger level ignoring neglect (what the ledger last recorded). */
    fun ledgerMastery(entry: LedgerEntry): MasteryLevel =
        when {
            entry.repetitions >= MASTERED_REPS -> MasteryLevel.MASTERED
            entry.repetitions >= PROFICIENT_REPS -> MasteryLevel.PROFICIENT
            entry.repetitions >= FAMILIAR_REPS -> MasteryLevel.FAMILIAR
            else -> MasteryLevel.UNSEEN
        }

    /** Level as of `todayEpochDay`, applying decay for neglected concepts. */
    fun effectiveMastery(entry: LedgerEntry, todayEpochDay: Long): MasteryLevel {
        val idle = todayEpochDay - entry.lastEpochDay
        return when (ledgerMastery(entry)) {
            MasteryLevel.MASTERED -> if (idle > MASTERED_DECAY_DAYS) MasteryLevel.PROFICIENT else MasteryLevel.MASTERED
            MasteryLevel.PROFICIENT -> if (idle > PROFICIENT_DECAY_DAYS) MasteryLevel.FAMILIAR else MasteryLevel.PROFICIENT
            MasteryLevel.FAMILIAR -> if (idle > FAMILIAR_DECAY_DAYS) MasteryLevel.UNSEEN else MasteryLevel.FAMILIAR
            MasteryLevel.UNSEEN -> MasteryLevel.UNSEEN
        }
    }

    /**
     * Mastery-challenge mode: learned-but-aging concepts first — previously
     * learned (reviewed at least once) and long since last reviewed, including
     * decayed ones (that is the point of a challenge). Ordering: oldest
     * last-review first; stable and deterministic.
     */
    fun masteryChallenge(entries: List<LedgerEntry>, todayEpochDay: Long, limit: Int = 5): List<LedgerEntry> =
        entries
            .filter {
                it.repetitions >= FAMILIAR_REPS &&
                    todayEpochDay - it.lastEpochDay >= CHALLENGE_AGING_DAYS
            }
            .sortedWith(compareBy({ it.lastEpochDay }, { it.conceptId }))
            .take(limit)

    private inline fun advance(
        entry: LedgerEntry,
        todayEpochDay: Long,
        intervalFor: (Int) -> Int,
    ): LedgerEntry {
        val reps = entry.repetitions + 1
        val interval = intervalFor(reps)
        return LedgerEntry(entry.conceptId, reps, interval, todayEpochDay, todayEpochDay + interval)
    }

    private fun intervalLadder(reps: Int): Int =
        when {
            reps <= 1 -> 1
            reps == 2 -> 3
            reps == 3 -> 7
            reps == 4 -> 16
            reps == 5 -> 35
            else -> 70
        }

    private fun easyIntervalLadder(reps: Int): Int =
        when {
            reps <= 1 -> 2
            reps == 2 -> 5
            reps == 3 -> 11
            reps == 4 -> 25
            reps == 5 -> 50
            else -> 90
        }

    companion object {
        const val FAMILIAR_REPS = 1
        const val PROFICIENT_REPS = 3
        const val MASTERED_REPS = 6
        const val FAMILIAR_DECAY_DAYS = 30L
        const val PROFICIENT_DECAY_DAYS = 45L
        const val MASTERED_DECAY_DAYS = 90L
        const val CHALLENGE_AGING_DAYS = 7L
    }
}

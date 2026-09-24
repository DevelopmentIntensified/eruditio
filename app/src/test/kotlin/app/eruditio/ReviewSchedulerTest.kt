package app.eruditio

import app.eruditio.core.LedgerEntry
import app.eruditio.core.MasteryLevel
import app.eruditio.core.RecallGrade
import app.eruditio.core.ReviewScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Seam tests for the review scheduler (spec): sequences of completion events ->
 * due sets; mastery transitions and decay; challenge selection; boundary cases.
 * Deterministic and table-driven; no Android dependencies.
 */
class ReviewSchedulerTest {

    private val scheduler = ReviewScheduler()

    private fun graded(id: String, enrollDay: Long, reviews: List<Pair<Long, RecallGrade>>): LedgerEntry {
        var entry = scheduler.enroll(id, enrollDay)
        for ((day, grade) in reviews) entry = scheduler.review(entry, grade, day)
        return entry
    }

    @Test
    fun `empty ledger produces empty due set`() {
        assertTrue(scheduler.due(emptyList(), todayEpochDay = 100).isEmpty())
        assertTrue(scheduler.masteryChallenge(emptyList(), todayEpochDay = 100).isEmpty())
    }

    @Test
    fun `enrollment is not due today and is due tomorrow`() {
        val entry = scheduler.enroll("demo:apology#c0", todayEpochDay = 10)
        assertTrue(scheduler.due(listOf(entry), todayEpochDay = 10).isEmpty())
        assertEquals(listOf(entry.conceptId), scheduler.due(listOf(entry), todayEpochDay = 11).map { it.conceptId })
    }

    @Test
    fun `mastery table - good grades climb familiar, proficient, mastered`() {
        val cases = listOf(
            1 to MasteryLevel.FAMILIAR,
            3 to MasteryLevel.PROFICIENT,
            6 to MasteryLevel.MASTERED,
            7 to MasteryLevel.MASTERED,
        )
        for ((reps, expected) in cases) {
            val grades = List(reps) { i -> (10L + i) to RecallGrade.GOOD }
            val entry = graded("demo:m#c0", enrollDay = 10, reviews = grades)
            assertEquals("reps=$reps", expected, scheduler.ledgerMastery(entry))
        }
    }

    @Test
    fun `hard stalls progress at the current level`() {
        val before = graded("demo:h#c0", 10, listOf(11L to RecallGrade.GOOD, 14L to RecallGrade.GOOD))
        val after = scheduler.review(before, RecallGrade.HARD, todayEpochDay = 20)
        assertEquals(before.repetitions, after.repetitions)
        assertEquals(before.intervalDays, after.intervalDays)
        assertEquals(20L + before.intervalDays, after.dueEpochDay)
    }

    @Test
    fun `again falls back to the bottom of the ladder`() {
        val before = gradedProficient()
        val after = scheduler.review(before, RecallGrade.AGAIN, todayEpochDay = 50)
        assertEquals(0, after.repetitions)
        assertEquals(1, after.intervalDays)
        assertEquals(51L, after.dueEpochDay)
        assertEquals(MasteryLevel.UNSEEN, scheduler.ledgerMastery(after))
    }

    @Test
    fun `easy stretches the interval beyond good`() {
        val good = scheduler.review(scheduler.enroll("demo:e#c0", 10), RecallGrade.GOOD, 11)
        val easy = scheduler.review(scheduler.enroll("demo:e2#c0", 10), RecallGrade.EASY, 11)
        assertTrue(easy.intervalDays > good.intervalDays)
        assertTrue(easy.dueEpochDay > good.dueEpochDay)
    }

    @Test
    fun `decay demotes neglected levels at query time`() {
        val mastered = graded("demo:mc#c0", 10, List(6) { i -> (11L + i) to RecallGrade.GOOD })
        assertEquals(MasteryLevel.MASTERED, scheduler.effectiveMastery(mastered, todayEpochDay = mastered.lastEpochDay + 90))
        assertEquals(MasteryLevel.PROFICIENT, scheduler.effectiveMastery(mastered, todayEpochDay = mastered.lastEpochDay + 91))

        val proficient = graded("demo:pc#c0", 10, listOf(11L, 14L, 21L).map { it to RecallGrade.GOOD })
        assertEquals(MasteryLevel.PROFICIENT, scheduler.effectiveMastery(proficient, todayEpochDay = 66))
        assertEquals(MasteryLevel.FAMILIAR, scheduler.effectiveMastery(proficient, todayEpochDay = 67))

        val familiar = graded("demo:fc#c0", 10, listOf(11L to RecallGrade.GOOD))
        assertEquals(MasteryLevel.FAMILIAR, scheduler.effectiveMastery(familiar, todayEpochDay = 41))
        assertEquals(MasteryLevel.UNSEEN, scheduler.effectiveMastery(familiar, todayEpochDay = 42))
    }

    @Test
    fun `decayed concept re-masters by grading well again`() {
        val mastered = graded("demo:rm#c0", 10, List(6) { i -> (11L + i) to RecallGrade.GOOD })
        // 91 days idle -> decayed to proficient
        val decayedDay = mastered.lastEpochDay + 91
        assertEquals(MasteryLevel.PROFICIENT, scheduler.effectiveMastery(mastered, todayEpochDay = decayedDay))
        // recall it again: the review refreshes the level to full
        val again = scheduler.review(mastered, RecallGrade.GOOD, todayEpochDay = decayedDay)
        assertEquals(MasteryLevel.MASTERED, scheduler.effectiveMastery(again, todayEpochDay = decayedDay))
    }

    @Test
    fun `missed days leave the concept due`() {
        val entry = graded("demo:missed#c0", 10, listOf(11L to RecallGrade.GOOD))
        // walked away for 20 days; interval was 3 -> long overdue
        assertEquals(entry.conceptId, scheduler.due(listOf(entry), todayEpochDay = 40).single().conceptId)
        val afterMiss = scheduler.review(entry, RecallGrade.AGAIN, todayEpochDay = 40)
        assertEquals(41L, afterMiss.dueEpochDay)
    }

    @Test
    fun `due set is deterministic - due day then concept id`() {
        val a = LedgerEntry("demo:a#c0", 1, 1, 10, 12)
        val b = LedgerEntry("demo:b#c0", 1, 1, 10, 12)
        val c = LedgerEntry("demo:c#c0", 1, 1, 9, 11)
        val due = scheduler.due(listOf(a, b, c), todayEpochDay = 12)
        assertEquals(listOf("demo:c#c0", "demo:a#c0", "demo:b#c0"), due.map { it.conceptId })
    }

    @Test
    fun `due set respects the limit`() {
        val entries = (1..15).map { i ->
            LedgerEntry("demo:t${i.toString().padStart(2, '0')}#c0", 0, 1, 10, 11)
        }
        val due = scheduler.due(entries, todayEpochDay = 20, limit = 5)
        assertEquals(5, due.size)
        assertEquals((1..5).map { i -> "demo:t${i.toString().padStart(2, '0')}#c0" }, due.map { it.conceptId })
    }

    @Test
    fun `challenge surfaces learned-but-aging concepts, oldest last-review first`() {
        val fresh = graded("demo:fresh#c0", 10, listOf(998L to RecallGrade.GOOD)) // reviewed 2 days ago
        val aging = graded("demo:aging#c0", 10, listOf(980L to RecallGrade.GOOD, 983L to RecallGrade.GOOD))
        val unseen = scheduler.enroll("demo:unseen#c0", 995)
        val challenge = scheduler.masteryChallenge(listOf(fresh, aging, unseen), todayEpochDay = 1000)
        assertEquals(listOf("demo:aging#c0"), challenge.map { it.conceptId })
    }

    @Test
    fun `challenge respects the limit and orders by last review`() {
        val first = graded("demo:older#c0", 10, listOf(900L to RecallGrade.GOOD))
        val second = graded("demo:mid#c0", 10, listOf(920L to RecallGrade.GOOD))
        val third = graded("demo:newest#c0", 10, listOf(940L to RecallGrade.GOOD))
        val challenge = scheduler.masteryChallenge(listOf(third, first, second), todayEpochDay = 1000, limit = 2)
        assertEquals(listOf("demo:older#c0", "demo:mid#c0"), challenge.map { it.conceptId })
    }

    private fun gradedProficient() = graded("demo:again#c0", 10, listOf(
        11L to RecallGrade.GOOD, 14L to RecallGrade.GOOD, 21L to RecallGrade.GOOD,
    ))
}

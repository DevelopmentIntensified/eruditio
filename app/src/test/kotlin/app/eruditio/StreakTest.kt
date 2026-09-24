package app.eruditio

import app.eruditio.core.HandoffShelf
import app.eruditio.core.Streak
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreakTest {
    private val days = mutableSetOf<Long>()

    private fun streak(today: Long) = Streak.current(days.toSet(), today)

    @Test
    fun `empty history has no streak`() = assertEquals(0, streak(today = 10))

    @Test
    fun `today alone counts`() {
        days += 10
        assertEquals(1, streak(today = 10))
    }

    @Test
    fun `yesterday alone counts - today still open`() {
        days += 9
        assertEquals(1, streak(today = 10))
    }

    @Test
    fun `consecutive days count back from today`() {
        days += listOf(8, 9, 10)
        assertEquals(3, streak(today = 10))
    }

    @Test
    fun `idle today keeps yesterday's chain`() {
        days += listOf(9)
        assertEquals(1, streak(today = 10))
        days += listOf(8)
        assertEquals(2, streak(today = 10))
    }

    @Test
    fun `a missed day breaks the streak`() {
        days += listOf(8)
        assertEquals(0, streak(today = 10)) // day 9 and 10 both idle
    }

    @Test
    fun `older history without today does not count`() {
        days += listOf(5, 6, 7)
        assertEquals(0, streak(today = 10))
    }
}

class HandoffShelfTest {
    @Test
    fun `ancient texts hand off to the world history course`() {
        val link = HandoffShelf.suggest("ancient")
        assertEquals("https://www.khanacademy.org/humanities/world-history/world-history-beginnings", link?.url)
    }

    @Test
    fun `christian texts hand off to grammar`() {
        assertEquals("https://www.khanacademy.org/humanities/grammar", HandoffShelf.suggest("christian")?.url)
    }

    @Test
    fun `biographies hand off to an exercism track`() {
        assertEquals("https://exercism.org/tracks/kotlin", HandoffShelf.suggest("biography")?.url)
    }

    @Test
    fun `unknown groups produce no suggestion`() {
        assertNull(HandoffShelf.suggest("unknown"))
        assertNull(HandoffShelf.suggest(null))
    }
}

package app.eruditio

import app.eruditio.core.TrailingWeek
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/** Pins the trailing-week window: exactly 7 local days and 6 local midnights. */
class TrailingWeekTest {
    private val zone = ZoneId.of("America/New_York")

    @Test
    fun `window spans exactly 7 days inclusive regardless of zone`() {
        val clock = ZoneClock(Instant.parse("2026-01-15T08:30:00Z").toEpochMilli(), zone)
        val week = TrailingWeek.of(clock)
        assertEquals(7, week.dayRange.count())
        assertEquals(week.todayEpochDay - 6, week.firstEpochDay)
        assertEquals(week.todayEpochDay, week.dayRange.last)
    }

    @Test
    fun `millis window opens at local midnight six days back`() {
        val clock = ZoneClock(Instant.parse("2026-01-15T08:30:00Z").toEpochMilli(), zone)
        val week = TrailingWeek.of(clock)
        // 2026-01-15T08:30Z is 03:30 in NY: today is Jan 15, so start of Jan 9 local = 05:00Z.
        assertEquals(Instant.parse("2026-01-09T05:00:00Z").toEpochMilli(), week.startOfDayMillis)
        assertEquals(week.todayEpochDay - 6, Instant.ofEpochMilli(week.startOfDayMillis).atZone(zone).toLocalDate().toEpochDay())
        assertEquals(clock.epochMillis, week.nowMillis)
    }
}

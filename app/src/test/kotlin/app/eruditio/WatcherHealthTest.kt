package app.eruditio

import app.eruditio.intercept.InterceptionFault
import app.eruditio.intercept.WatcherHealth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The watcher-liveness decision (ticket 24): a silent watcher means the pause
 * screen never appears, so each fault must be reported honestly.
 */
class WatcherHealthTest {

    private val now = 1_700_000_000_000L
    private val freshBeat = now - 5_000L

    private fun evaluate(
        lastBeat: Long = freshBeat,
        hasUsageAccess: Boolean = true,
        targets: Int = 3,
        batteryOptimized: Boolean = false,
    ) = WatcherHealth.evaluate(
        nowMillis = now,
        lastBeatMillis = lastBeat,
        hasUsageAccess = hasUsageAccess,
        selectedTargetCount = targets,
        batteryOptimized = batteryOptimized,
    )

    @Test
    fun `a fresh beat with access and targets is watching`() {
        val status = evaluate()
        assertEquals(InterceptionFault.NONE, status.fault)
        assertTrue(status.isWatching)
        assertEquals(freshBeat, status.lastBeatMillis)
    }

    @Test
    fun `missing usage access outranks everything else`() {
        val status = evaluate(lastBeat = 0L, hasUsageAccess = false)
        assertEquals(InterceptionFault.NO_USAGE_ACCESS, status.fault)
        assertFalse(status.isWatching)
    }

    @Test
    fun `a stale beat means the watcher is not running`() {
        val status = evaluate(lastBeat = now - WatcherHealth.STALE_AFTER_MILLIS - 1L)
        assertEquals(InterceptionFault.NOT_RUNNING, status.fault)
    }

    @Test
    fun `never having beat is not running, not fresh`() {
        assertEquals(InterceptionFault.NOT_RUNNING, evaluate(lastBeat = 0L).fault)
    }

    @Test
    fun `a beat exactly at the threshold still counts as running`() {
        val status = evaluate(lastBeat = now - WatcherHealth.STALE_AFTER_MILLIS)
        assertEquals(InterceptionFault.NONE, status.fault)
    }

    @Test
    fun `no selected targets is idle, not a fault of the watcher`() {
        val status = evaluate(targets = 0)
        assertEquals(InterceptionFault.NO_TARGETS, status.fault)
        assertFalse(status.isWatching)
    }

    @Test
    fun `battery risk is an advisory, never the fault`() {
        val status = evaluate(batteryOptimized = true)
        assertEquals(InterceptionFault.NONE, status.fault)
        assertTrue(status.batteryOptimized)
    }

    @Test
    fun `stale is measured from the last beat, not from now`() {
        assertFalse(WatcherHealth.isStale(nowMillis = now, lastBeatMillis = now - 1L))
        assertFalse(WatcherHealth.isStale(nowMillis = now, lastBeatMillis = now))
        assertTrue(WatcherHealth.isStale(nowMillis = now, lastBeatMillis = 0L))
    }
}

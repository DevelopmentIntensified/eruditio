package app.eruditio.intercept

/** Why interception is inert right now, in the order the user must fix things. */
enum class InterceptionFault {
    NONE,
    NO_USAGE_ACCESS,
    NOT_RUNNING,
    NO_TARGETS,
}

/**
 * One honest answer to "is the pause screen armed?". [batteryOptimized] is an
 * advisory, not a fault: the watcher may be alive now but at risk of being
 * dozed later, and only the user can grant the exemption.
 */
data class WatcherStatus(
    val fault: InterceptionFault,
    val batteryOptimized: Boolean,
    val lastBeatMillis: Long,
) {
    val isWatching: Boolean get() = fault == InterceptionFault.NONE
}

/**
 * The watcher-liveness decision as a pure predicate (ADR-0002: pure decisions
 * are testable; the Android probes arrive as arguments). The running service
 * beats every [BEAT_EVERY_POLLS] polls; silence past [STALE_AFTER_MILLIS]
 * means the process is gone.
 */
object WatcherHealth {

    const val STALE_AFTER_MILLIS = 120_000L
    const val BEAT_EVERY_POLLS = 15

    fun isStale(nowMillis: Long, lastBeatMillis: Long): Boolean =
        lastBeatMillis <= 0L || nowMillis - lastBeatMillis > STALE_AFTER_MILLIS

    fun evaluate(
        nowMillis: Long,
        lastBeatMillis: Long,
        hasUsageAccess: Boolean,
        selectedTargetCount: Int,
        batteryOptimized: Boolean,
    ): WatcherStatus {
        val fault = when {
            !hasUsageAccess -> InterceptionFault.NO_USAGE_ACCESS
            isStale(nowMillis, lastBeatMillis) -> InterceptionFault.NOT_RUNNING
            selectedTargetCount <= 0 -> InterceptionFault.NO_TARGETS
            else -> InterceptionFault.NONE
        }
        return WatcherStatus(fault, batteryOptimized, lastBeatMillis)
    }
}

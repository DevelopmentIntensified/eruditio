package app.eruditio.intercept

import android.app.AppOpsManager
import android.app.ForegroundServiceStartNotAllowedException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.PowerManager
import android.os.Process

/**
 * The Android-facing half of watcher liveness: the heartbeat the running
 * service drops, the OS probes, and the one place that arms "always on"
 * (the service plus its watchdog). The decision itself lives in
 * [WatcherHealth] so it stays pure and testable.
 */
object WatcherRuntime {

    private const val PREFS_FILE = "watcher_state"
    private const val KEY_BEAT = "last_beat_millis"

    /** The running service calls this; silence past the stale threshold means it is gone. */
    fun beat(context: Context) {
        prefs(context).edit().putLong(KEY_BEAT, System.currentTimeMillis()).apply()
    }

    fun status(context: Context, nowMillis: Long = System.currentTimeMillis()): WatcherStatus =
        WatcherHealth.evaluate(
            nowMillis = nowMillis,
            lastBeatMillis = prefs(context).getLong(KEY_BEAT, 0L),
            hasUsageAccess = hasUsageAccess(context),
            selectedTargetCount = TargetApps.selected(context).size,
            batteryOptimized = isBatteryOptimized(context),
        )

    fun hasUsageAccess(context: Context): Boolean = try {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        ops.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        ) == AppOpsManager.MODE_ALLOWED
    } catch (e: Exception) {
        false
    }

    /**
     * True when the OS may doze the watcher in the background, i.e. the user
     * has not exempted the app. Unknowable (no PowerManager) counts as at risk.
     */
    fun isBatteryOptimized(context: Context): Boolean {
        val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return !power.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Start the watcher if the OS allows a foreground-service start right now.
     * False means the platform refused (background start restrictions); the
     * caller decides whether to retry or surface it.
     */
    fun tryStartWatcher(context: Context): Boolean = try {
        context.startForegroundService(Intent(context, UsageStatsWatcherService::class.java))
        true
    } catch (e: ForegroundServiceStartNotAllowedException) {
        false
    } catch (e: IllegalStateException) {
        false
    } catch (e: SecurityException) {
        false
    }

    /** Arm "always on": the watcher itself plus the watchdog that revives it. */
    fun arm(context: Context) {
        tryStartWatcher(context)
        WatcherWatchdog.schedule(context)
    }

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
}

package app.eruditio.intercept

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * The self-heal: every 15 minutes, if the watcher's heartbeat has gone stale,
 * try to start it again. WorkManager jobs are NOT an exemption from the
 * background foreground-service start restrictions, so a refusal is expected
 * (the user opening the app, or exempting battery optimisation, opens a
 * window) — retry with backoff rather than crash.
 */
class WatcherWatchdog(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val context = applicationContext
        if (WatcherRuntime.status(context).fault != InterceptionFault.NOT_RUNNING) return Result.success()
        return if (WatcherRuntime.tryStartWatcher(context)) Result.success() else Result.retry()
    }

    companion object {
        private const val UNIQUE_NAME = "eruditio-watcher-watchdog"
        private const val INTERVAL_MINUTES = 15L
        private const val BACKOFF_MINUTES = 2L

        /** Idempotent: the OS keeps one watchdog no matter how often we arm. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WatcherWatchdog>(INTERVAL_MINUTES, TimeUnit.MINUTES)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_MINUTES, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}

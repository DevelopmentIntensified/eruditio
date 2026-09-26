package app.eruditio.intercept

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Brings the watcher back after a reboot or an update. Both actions are
 * documented exemptions from the background foreground-service start
 * restrictions, so this is the one place Eruditio can re-arm with the user
 * nowhere in sight.
 */
class WatcherBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED ->
                WatcherRuntime.arm(context)
        }
    }
}

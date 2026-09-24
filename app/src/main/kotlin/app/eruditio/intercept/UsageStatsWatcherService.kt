package app.eruditio.intercept

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.SystemClock
import kotlinx.coroutines.runBlocking

/**
 * UsageStatsManager-based foreground detection (spec: no AccessibilityService).
 * A 1 Hz poll over recent UsageEvents detects a target app reaching the
 * foreground and invokes the session seam for the friction decision.
 * The pause screen appears over the target; the user can always continue.
 */
class UsageStatsWatcherService : Service() {

    private val thread = HandlerThread("usage-watcher")
    private var handler: Handler? = null
    private var lastEventWindow = System.currentTimeMillis()
    private var lastForegroundPackage = ""
    private val recentIntercepts = mutableMapOf<String, Long>()

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        thread.start()
        handler = Handler(thread.looper)
        createChannel()
        startForeground(NOTIF_ID, notification())
        scheduleNextPoll()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        handler?.removeCallbacksAndMessages(null)
        thread.quitSafely()
        super.onDestroy()
    }

    private fun scheduleNextPoll() {
        handler?.postDelayed(
            {
                poll()
                scheduleNextPoll()
            },
            POLL_INTERVAL_MILLIS,
        )
    }

    private fun poll() {
        val now = System.currentTimeMillis()
        val pkg = foregroundPackage(lastEventWindow, now) ?: lastForegroundPackage
        lastEventWindow = now
        if (pkg != lastForegroundPackage) {
            lastForegroundPackage = pkg
            if (pkg != packageName && TargetApps.isTarget(this, pkg) && !justIntercepted(pkg)) {
                intercept(pkg)
            }
        }
    }

    private fun foregroundPackage(from: Long, to: Long): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        val events = usm.queryEvents(from, to)
        val ev = UsageEvents.Event()
        var candidate: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(ev)
            val resumed =
                if (Build.VERSION.SDK_INT >= 29) ev.eventType == UsageEvents.Event.ACTIVITY_RESUMED
                else ev.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            if (resumed && ev.packageName != packageName) candidate = ev.packageName
        }
        return candidate
    }

    private fun justIntercepted(pkg: String): Boolean {
        val last = recentIntercepts[pkg] ?: return false
        return SystemClock.elapsedRealtime() - last < REINTERCEPT_COOLDOWN_MILLIS
    }

    private fun intercept(pkg: String) {
        recentIntercepts[pkg] = SystemClock.elapsedRealtime()
        when (val decision = runBlocking { container.sessions.onTargetAppOpened(pkg) }) {
            is app.eruditio.core.InterceptionDecision.Intercept -> {
                val intent = Intent(this, PauseActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(PauseActivity.EXTRA_TARGET, decision.targetPackage)
                startActivity(intent)
            }
            app.eruditio.core.InterceptionDecision.PassThrough -> Unit
        }
    }

    private val container: app.eruditio.AppContainer
        get() = (applicationContext as app.eruditio.EruditioApp).container

    private fun createChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Interception watcher", NotificationManager.IMPORTANCE_MIN),
        )
    }

    private fun notification(): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Eruditio is watching for scroll moments")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()

    companion object {
        private const val POLL_INTERVAL_MILLIS = 1000L
        private const val REINTERCEPT_COOLDOWN_MILLIS = 15_000L
        private const val NOTIF_ID = 4711
        private const val CHANNEL_ID = "eruditio-watcher"

        fun ensureRunning(context: Context) {
            context.startForegroundService(Intent(context, UsageStatsWatcherService::class.java))
        }
    }
}

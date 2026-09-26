package app.eruditio.intercept

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
    private var pollsSinceBeat = 0

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        thread.start()
        handler = Handler(thread.looper)
        createChannel()
        startForeground(NOTIF_ID, notification())
        seedForegroundState()
        WatcherRuntime.beat(this)
        // A service the OS killed mid-session comes back through the watchdog,
        // not through the user opening the app.
        WatcherWatchdog.schedule(this)
        scheduleNextPoll()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        handler?.removeCallbacksAndMessages(null)
        thread.quitSafely()
        // START_STICKY covers the ordinary restart; the watchdog covers the
        // case where the system declines to.
        WatcherWatchdog.schedule(this)
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
        if (++pollsSinceBeat >= WatcherHealth.BEAT_EVERY_POLLS) {
            pollsSinceBeat = 0
            WatcherRuntime.beat(this)
        }
        if (pkg != lastForegroundPackage) {
            lastForegroundPackage = pkg
            if (pkg != packageName && TargetApps.isTarget(this, pkg) && !justIntercepted(pkg)) {
                intercept(pkg)
            }
        }
    }

    /**
     * A service (re)started while a target app is already foreground never sees
     * that app's resume event, so learn it from the recent past. This only
     * seeds state — it never intercepts the app the user is already in.
     */
    private fun seedForegroundState() {
        val now = System.currentTimeMillis()
        lastForegroundPackage = foregroundPackage(now - SEED_LOOKBACK_MILLIS, now).orEmpty()
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
            .setContentIntent(launchIntent())
            .setOngoing(true)
            .build()

    /** Tapping the ongoing notification shows where the watcher stands. */
    private fun launchIntent(): android.app.PendingIntent {
        val intent = Intent(this, app.eruditio.MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return android.app.PendingIntent.getActivity(
            this,
            NOTIF_ID,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val POLL_INTERVAL_MILLIS = 1000L
        private const val REINTERCEPT_COOLDOWN_MILLIS = 15_000L
        private const val SEED_LOOKBACK_MILLIS = 30_000L
        private const val NOTIF_ID = 4711
        private const val CHANNEL_ID = "eruditio-watcher"

        fun ensureRunning(context: Context) {
            WatcherRuntime.arm(context)
        }
    }
}

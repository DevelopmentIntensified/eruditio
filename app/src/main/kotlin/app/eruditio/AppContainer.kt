package app.eruditio

import android.app.Application
import android.content.Context
import app.eruditio.core.ConceptSource
import app.eruditio.core.LedgerStore
import app.eruditio.core.ReviewScheduler
import app.eruditio.core.SessionController
import app.eruditio.core.SessionLog
import app.eruditio.core.TrialGate
import app.eruditio.data.DeviceClock
import app.eruditio.data.EruditioDatabase
import app.eruditio.data.PlayBillingGate
import app.eruditio.data.EntitlementRepository
import app.eruditio.data.RoomInterceptionLog
import app.eruditio.data.RoomLedgerStore
import app.eruditio.data.RoomSessionLog
import app.eruditio.data.ReadingPositions
import app.eruditio.data.SegmentConcepts
import app.eruditio.data.TextLibrary
import app.eruditio.data.ThemeStore

/**
 * Composition root. Debug builds bypass the billing gate (billing ticket);
 * the two test seams (`SessionController`, `ReviewScheduler`) stay Android-free.
 */
class AppContainer(private val app: Application) {
    val library = TextLibrary(app)

    private val db = EruditioDatabase.build(app)
    private val clock = DeviceClock()
    val entitlements = EntitlementRepository(db.entitlementDao())
    val trial = TrialGate(entitlements, isDebug = BuildConfig.DEBUG)

    val appContext: Context get() = app
    val hasUsageAccess: Boolean get() = MainActivity.hasUsageAccess(app)
    val canDrawOverlays: Boolean get() = MainActivity.canDrawOverlays(app)
    fun today(): Long = clock.epochDay

    val themes = ThemeStore(app)
    val billing = PlayBillingGate(app, trial)
    val positions = ReadingPositions(db.readingPositionDao())
    val interceptionLog = RoomInterceptionLog(db.interceptionDao())
    val scheduler = ReviewScheduler()
    val ledger: LedgerStore = RoomLedgerStore(db.ledgerDao())
    val concepts: ConceptSource = SegmentConcepts(library)
    val sessionLog: SessionLog = RoomSessionLog(db.sessionDao())

    val sessions: SessionController = SessionController(
        scheduler = scheduler,
        ledger = ledger,
        interceptionLog = interceptionLog,
        sessionLog = sessionLog,
        concepts = concepts,
        gate = trial,
        clock = clock,
    )

    private val textLengths = mutableMapOf<String, Int>()

    /** Rough reading progress for library rows, as a percent (0 when unread). */
    suspend fun lastReadPercent(textId: String): Int {
        val position = positions.restore(textId) ?: return 0
        if (position.charOffset <= 0) return 0
        val length = textLength(textId)
        if (length <= 0) return 0
        return ((position.charOffset * 100) / length).toInt().coerceIn(0, 100)
    }

    fun textLength(textId: String): Int =
        textLengths.getOrPut(textId) { library.text(textId).length }
}

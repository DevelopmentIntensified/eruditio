package app.eruditio

import android.app.Application
import android.content.Context
import app.eruditio.core.ConceptSource
import app.eruditio.core.AnnotationStore
import app.eruditio.core.GleaningContextSource
import app.eruditio.core.GleaningStore
import app.eruditio.core.LedgerStore
import app.eruditio.core.PositionStore
import app.eruditio.core.ReviewScheduler
import app.eruditio.core.SessionController
import app.eruditio.core.SessionLog
import app.eruditio.core.TrialGate
import app.eruditio.data.AssetGleaningContextSource
import app.eruditio.data.RoomAnnotationStore
import app.eruditio.data.DeviceClock
import app.eruditio.data.EruditioDatabase
import app.eruditio.data.PlayBillingGate
import app.eruditio.data.ReadingSettings
import app.eruditio.data.EntitlementRepository
import app.eruditio.data.GleaningCoordinator
import app.eruditio.data.RoomGleaningStore
import app.eruditio.data.RoomInterceptionLog
import app.eruditio.data.RoomLedgerStore
import app.eruditio.data.RoomSessionLog
import app.eruditio.data.ReadingPositions
import app.eruditio.data.SegmentConcepts
import app.eruditio.data.TextLibrary
import app.eruditio.data.ThemeStore
import androidx.compose.runtime.mutableStateOf

/**
 * Composition root. Debug builds bypass the billing gate (billing ticket);
 * the two test seams (`SessionController`, `ReviewScheduler`) stay Android-free.
 */
class AppContainer(private val app: Application) {
    val library = TextLibrary(app)

    private val db = EruditioDatabase.build(app)
    private val clock = DeviceClock()
    val entitlements = EntitlementRepository(db.entitlementDao())
    // FOSS flavor unlocks the core loop like debug: no Play Billing on F-Droid.
    val trial = TrialGate(entitlements, isDebug = BuildConfig.DEBUG || BuildConfig.FLAVOR == "foss")

    val appContext: Context get() = app
    val hasUsageAccess: Boolean get() = MainActivity.hasUsageAccess(app)
    val canDrawOverlays: Boolean get() = MainActivity.canDrawOverlays(app)

    /**
     * Set by the reader's checkpoint pause (ticket 14) while the shell activity
     * is stopped; the shell consumes it on return to land on Home/Today (tab 0)
     * instead of the tab the user left. Snapshot state so the shell observes it
     * without polling.
     */
    val pauseToHome = mutableStateOf(false)
    fun today(): Long = clock.epochDay

    val themes = ThemeStore(app)
    val reading = ReadingSettings(app)
    val billing = PlayBillingGate(app, trial)
    val positions = ReadingPositions(db.readingPositionDao(), clock)
    val interceptionLog = RoomInterceptionLog(db.interceptionDao())
    val scheduler = ReviewScheduler()
    val ledger: LedgerStore = RoomLedgerStore(db.ledgerDao())
    /** Dormant review-candidate source; completion does not auto-enroll it. */
    val concepts: ConceptSource = SegmentConcepts(library)
    val sessionLog: SessionLog = RoomSessionLog(db.sessionDao())
    val gleanings: GleaningStore = RoomGleaningStore(db.gleaningDao(), clock)
    val gleaningContexts: GleaningContextSource = AssetGleaningContextSource(app)
    val annotations: AnnotationStore = RoomAnnotationStore(db.annotationDao(), clock)

    /** Seam adapter: offset primitive only — Room entities never cross into the core. */
    val positionStore: PositionStore = object : PositionStore {
        override suspend fun restoreOffset(textId: String): Long? =
            positions.restore(textId)?.charOffset

        override suspend fun lastReadTextId(): String? = positions.lastReadTextId()
    }

    val sessions: SessionController = SessionController(
        scheduler = scheduler,
        ledger = ledger,
        interceptionLog = interceptionLog,
        sessionLog = sessionLog,
        positionStore = positionStore,
        gate = trial,
        clock = clock,
    )

    val gleaningCoordinator = GleaningCoordinator(sessions, library, gleanings, gleaningContexts)

    private val textLengths = mutableMapOf<String, Int>()

    fun textLength(textId: String): Int =
        textLengths.getOrPut(textId) { library.text(textId).length }

    /** Library progress map (percent by text id), assembled once: a
     *  composition-root query aggregation — owns library order, text lengths
     *  and the per-text readPercent seam call, so no consumer rebuilds it. */
    suspend fun progressById(): Map<String, Int> =
        library.texts.associate { it.id to sessions.readPercent(it.id, textLength(it.id)) }
}

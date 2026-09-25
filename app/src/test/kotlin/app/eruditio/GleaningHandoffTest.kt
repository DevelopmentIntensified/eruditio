package app.eruditio

import app.eruditio.core.ConceptSource
import app.eruditio.core.GleaningContextSource
import app.eruditio.core.GleaningHandoff
import app.eruditio.core.PendingGleaningDraft
import app.eruditio.core.ReviewScheduler
import app.eruditio.core.SessionController
import app.eruditio.core.SessionStart
import app.eruditio.core.SourceTextReader
import app.eruditio.core.TrialGate
import app.eruditio.data.GleaningCoordinator
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The hand-off rule (ADR-0006): a completed session always owes a reflection,
 * and a hand-off row that outlived its session must never wedge the reader.
 */
class GleaningHandoffTest {

    private val ledger = FakeLedgerStore()
    private val interceptions = FakeInterceptionLog()
    private val sessionLog = FakeSessionLog()
    private val entitlement = FakeEntitlementStore()
    private val store = FakeGleaningStore()
    private val concepts = FakeConceptSource()

    private val controller = SessionController(
        scheduler = ReviewScheduler(),
        ledger = ledger,
        interceptionLog = interceptions,
        sessionLog = sessionLog,
        positionStore = FakePositionStore(),
        gate = TrialGate(entitlement, isDebug = false),
        clock = TestClock(100),
    )

    private val coordinator = GleaningCoordinator(
        sessions = controller,
        texts = SourceTextReader { "Some passage text to glean from." },
        store = store,
        contexts = GleaningContextSource { _, _ -> null },
    )

    private suspend fun openSession() =
        (controller.openSession("demo:apology") as SessionStart.Opened).sessionId

    @Test
    fun `hand-off is written before the session completes`() = runTest {
        val sessionId = openSession()

        val result = coordinator.preparePending(sessionId, endChar = 25)

        assertEquals(GleaningHandoff.PREPARED, result)
        // The row exists while the session is still open — that ordering is
        // what makes a crash between the two steps recoverable.
        assertEquals(sessionId, store.pending(sessionId)?.sessionId)
        assertEquals(false, sessionLog.get(sessionId)?.isCompleted)
    }

    @Test
    fun `a crashed row on an open session does not wedge the reader`() = runTest {
        val sessionId = openSession()
        // Simulate: hand-off written, then the process died before completing.
        store.prepare(
            PendingGleaningDraft(
                sessionId = sessionId,
                textId = "demo:apology",
                startChar = 0,
                endChar = 25,
                sourceContext = "stale",
                curatedContext = null,
            ),
        )

        // Re-reading the same open session must still be able to check in.
        val result = coordinator.preparePending(sessionId, endChar = 20)

        assertEquals(GleaningHandoff.PREPARED, result)
        assertEquals(20L, store.pending(sessionId)?.endChar)
    }

    @Test
    fun `a completed session with a row is already handled`() = runTest {
        val sessionId = openSession()
        coordinator.preparePending(sessionId, endChar = 25)
        controller.completeSession(sessionId, endChar = 25)

        assertEquals(
            GleaningHandoff.ALREADY_HANDLED,
            coordinator.preparePending(sessionId, endChar = 50),
        )
    }

    @Test
    fun `a discarded session stays handled`() = runTest {
        val sessionId = openSession()
        coordinator.preparePending(sessionId, endChar = 25)
        controller.completeSession(sessionId, endChar = 25)
        store.discard(sessionId)

        assertEquals(
            GleaningHandoff.ALREADY_HANDLED,
            coordinator.preparePending(sessionId, endChar = 50),
        )
    }

    @Test
    fun `an unknown session is unavailable, not prepared`() = runTest {
        assertEquals(GleaningHandoff.UNAVAILABLE, coordinator.preparePending(999L, endChar = 25))
    }
}

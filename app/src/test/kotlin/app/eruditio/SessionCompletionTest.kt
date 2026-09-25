package app.eruditio

import app.eruditio.core.ReviewScheduler
import app.eruditio.core.SessionController
import app.eruditio.core.SessionStart
import app.eruditio.core.TrialGate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionCompletionTest {
    private val ledger = FakeLedgerStore()
    private val interceptions = FakeInterceptionLog()
    private val sessions = FakeSessionLog()
    private val entitlement = FakeEntitlementStore()

    private fun controller() = SessionController(
        scheduler = ReviewScheduler(),
        ledger = ledger,
        interceptionLog = interceptions,
        sessionLog = sessions,
        positionStore = FakePositionStore(),
        gate = TrialGate(entitlement, isDebug = false),
        clock = TestClock(100),
    )

    @Test
    fun `completion rejects a range with no forward progress`() = runTest {
        val controller = controller()
        val session = controller.openSession("demo:apology") as SessionStart.Opened

        assertEquals(null, controller.completeSession(session.sessionId, endChar = 0))
        assertTrue(sessions.get(session.sessionId)?.isCompleted != true)
    }

    @Test
    fun `a session cannot complete twice`() = runTest {
        val controller = controller()
        val session = controller.openSession("demo:apology") as SessionStart.Opened

        assertTrue(controller.completeSession(session.sessionId, 900) != null)
        assertEquals(null, controller.completeSession(session.sessionId, 1800))
    }
}

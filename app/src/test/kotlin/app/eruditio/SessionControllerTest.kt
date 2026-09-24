package app.eruditio

import app.eruditio.core.InterceptionDecision
import app.eruditio.core.InterceptionResult
import app.eruditio.core.ReviewScheduler
import app.eruditio.core.SessionController
import app.eruditio.core.SessionStart
import app.eruditio.core.TrialGate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Seam tests for the session controller (spec): interception decisions, outcome
 * recording, replace -> real session, completion -> due set changes, streak and
 * gate behaviour. The OS watcher and overlay are fakes behind this seam.
 */
class SessionControllerTest {

    private val ledger = FakeLedgerStore()
    private val interceptLog = FakeInterceptionLog()
    private val sessionLog = FakeSessionLog()
    private val concepts = FakeConceptSource()
    private val entitlement = FakeEntitlementStore()
    private val scheduler = ReviewScheduler()

    private fun makeController(
        day: Long = 100,
        friction: Boolean = true,
        isDebug: Boolean = false,
    ): Pair<TestClock, SessionController> {
        val clock = TestClock(day)
        val controller = SessionController(
            scheduler = scheduler,
            ledger = ledger,
            interceptionLog = interceptLog,
            sessionLog = sessionLog,
            concepts = concepts,
            gate = TrialGate(entitlement, isDebug = isDebug),
            clock = clock,
        )
        controller.frictionEnabled = friction
        return clock to controller
    }

    @Test
    fun `target opening with friction on intercepts`() = runTest {
        val controller = makeController().second
        assertEquals(
            InterceptionDecision.Intercept("com.instagram.android"),
            controller.onTargetAppOpened("com.instagram.android"),
        )
    }

    @Test
    fun `friction off passes through and records nothing`() = runTest {
        val controller = makeController(friction = false).second
        assertEquals(InterceptionDecision.PassThrough, controller.onTargetAppOpened("com.instagram.android"))
        assertTrue(interceptLog.all.isEmpty())
    }

    @Test
    fun `continue records continued and starts no session`() = runTest {
        val controller = makeController().second
        controller.continueToApp("com.instagram.android")
        assertEquals(listOf(InterceptionResult.CONTINUED), interceptLog.all.map { it.result })
        assertEquals(0, sessionLog.completedEpochDays().size)
        assertEquals(0, ledger.all().size) // no review items from a continued opening
    }

    @Test
    fun `replace records replaced and opens a real session at the last position`() = runTest {
        val controller = makeController().second
        val start = controller.replaceWithSession("com.instagram.android", "demo:republic", startChar = 4321)
        assertTrue(start is SessionStart.Opened)
        assertEquals(listOf(InterceptionResult.REPLACED), interceptLog.all.map { it.result })
        val record = sessionLog.get((start as SessionStart.Opened).sessionId)!!
        assertEquals("demo:republic", record.textId)
        assertEquals(4321L, record.startChar)
    }

    @Test
    fun `completing a session changes the due-review set`() = runTest {
        val (clock, controller) = makeController()
        val opened = controller.startSession("demo:apology") as SessionStart.Opened
        assertTrue(controller.dueToday().isEmpty()) // nothing due today
        val completed = controller.completeSession(opened.sessionId, endChar = 900)
        assertEquals(1, completed?.newConcepts) // the completed segment feeds the engine
        clock.advance(1)
        assertEquals(listOf("demo:apology#c0"), controller.dueToday().map { it.conceptId })
    }

    @Test
    fun `re-reading the same segment enrolls concepts once`() = runTest {
        val controller = makeController().second
        val s1 = controller.startSession("demo:apology") as SessionStart.Opened
        controller.completeSession(s1.sessionId, 900)
        val s2 = controller.startSession("demo:apology") as SessionStart.Opened
        val completed2 = controller.completeSession(s2.sessionId, 1800)
        assertEquals(0, completed2?.newConcepts)
        assertEquals(1, ledger.all().size)
    }

    @Test
    fun `weekly counts include continued and replaced outcomes`() = runTest {
        val (clock, controller) = makeController()
        controller.continueToApp("com.instagram.android")
        controller.replaceWithSession("com.reddit.frontpage", "demo:apology")
        assertEquals(2, controller.weeklyInterceptions())
        assertEquals(1, controller.weeklyReplaced())
        clock.advance(8)
        assertEquals(0, controller.weeklyInterceptions())
    }

    @Test
    fun `streak counts completed-session days, never openings`() = runTest {
        val (clock, controller) = makeController()
        val s1 = controller.replaceWithSession("com.instagram.android", "demo:apology") as SessionStart.Opened
        controller.completeSession(s1.sessionId, 900)
        assertEquals(1, controller.currentStreak())
        clock.advance(1)
        val s2 = controller.startSession("demo:apology") as SessionStart.Opened
        controller.completeSession(s2.sessionId, 1800)
        assertEquals(2, controller.currentStreak())
        clock.advance(1)
        assertEquals(2, controller.currentStreak()) // idle today, completed yesterday
        clock.advance(2)
        assertEquals(0, controller.currentStreak())
    }

    @Test
    fun `week dots cover the trailing seven days - today pending until completed`() = runTest {
        val (clock, controller) = makeController()
        val s = controller.replaceWithSession("com.instagram.android", "demo:apology") as SessionStart.Opened
        controller.completeSession(s.sessionId, 900)
        val dots = controller.weekDots()
        assertEquals(7, dots.size)
        assertEquals(clock.epochDay - 6, dots.first().epochDay)
        assertTrue(dots.dropLast(1).none { it.hasCompletedSession || it.today })
        assertTrue(dots.last().today && dots.last().hasCompletedSession) // session completed today: filled
        clock.advance(1) // a new day with no session yet: today stays pending
        val next = controller.weekDots()
        assertTrue(next.last().today && !next.last().hasCompletedSession)
        assertEquals(true, next[5].hasCompletedSession) // yesterday keeps its dot
    }

    @Test
    fun `reading minutes are minutes, not raw millis`() = runTest {
        val controller = makeController().second
        val opened = controller.startSession("crito", 0L)
        val sessionId = (opened as SessionStart.Opened).sessionId
        // ~3 minutes of real reading time lands as millis on the session log.
        sessionLog.addReadingTime(sessionId, 181_759L)
        assertEquals(3L, controller.minutesToday())
        assertEquals(3L to 3L, controller.readingMinutes())
    }

    @Test
    fun `gate - trial ends, purchase unlocks`() = runTest {
        val (clock, controller) = makeController()
        repeat(TrialGate.FREE_SESSION_LIMIT) {
            val s = controller.startSession("demo:apology") as SessionStart.Opened
            controller.completeSession(s.sessionId, 900)
            clock.advance(1)
        }
        assertEquals(SessionStart.Locked, controller.startSession("demo:apology"))
        TrialGate(entitlement, isDebug = false).markOwned()
        assertTrue(controller.startSession("demo:apology") is SessionStart.Opened)
    }

    @Test
    fun `gate - debug builds bypass the lock`() = runTest {
        val controller = makeController(isDebug = true).second
        assertTrue(controller.startSession("demo:apology") is SessionStart.Opened)
    }
}

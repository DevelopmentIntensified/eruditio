package app.eruditio

import app.eruditio.core.InterceptionDecision
import app.eruditio.core.InterceptionResult
import app.eruditio.core.LedgerEntry
import app.eruditio.core.MasteryCounts
import app.eruditio.core.PositionStore
import app.eruditio.core.ReviewScheduler
import app.eruditio.core.SessionController
import app.eruditio.core.SessionStart
import app.eruditio.core.TrialGate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

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
        positionStore: PositionStore = FakePositionStore(),
    ): Pair<TestClock, SessionController> {
        val clock = TestClock(day)
        val controller = SessionController(
            scheduler = scheduler,
            ledger = ledger,
            interceptionLog = interceptLog,
            sessionLog = sessionLog,
            positionStore = positionStore,
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
        val controller = makeController(
            positionStore = FakePositionStore(mapOf("demo:republic" to 4321L)),
        ).second
        val start = controller.replaceWithSession("com.instagram.android", "demo:republic")
        assertTrue(start is SessionStart.Opened)
        assertEquals(listOf(InterceptionResult.REPLACED), interceptLog.all.map { it.result })
        val record = sessionLog.get((start as SessionStart.Opened).sessionId)!!
        assertEquals("demo:republic", record.textId)
        assertEquals(4321L, record.startChar)
    }

    @Test
    fun `completing a session changes the due-review set`() = runTest {
        val (clock, controller) = makeController()
        val opened = controller.openSession("demo:apology") as SessionStart.Opened
        assertTrue(controller.dueToday().isEmpty()) // nothing due today
        val completed = controller.completeSession(opened.sessionId, endChar = 900)
        assertEquals(1, completed?.newConcepts) // the completed segment feeds the engine
        clock.advance(1)
        assertEquals(listOf("demo:apology#c0"), controller.dueToday().map { it.conceptId })
    }

    @Test
    fun `re-reading the same segment enrolls concepts once`() = runTest {
        val controller = makeController().second
        val s1 = controller.openSession("demo:apology") as SessionStart.Opened
        controller.completeSession(s1.sessionId, 900)
        val s2 = controller.openSession("demo:apology") as SessionStart.Opened
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
        val s2 = controller.openSession("demo:apology") as SessionStart.Opened
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
        val opened = controller.openSession("crito")
        val sessionId = (opened as SessionStart.Opened).sessionId
        // ~3 minutes of real reading time lands as millis on the session log.
        sessionLog.addReadingTime(sessionId, 181_759L)
        assertEquals(3L, controller.minutesToday())
        assertEquals(3L to 3L, controller.readingMinutes())
    }

    @Test
    fun `reading minutes bucket by local day, not UTC midnight`() = runTest {
        // UTC+05:30 device, local 01:00: the whole first 5.5h of the local day
        // sit BEFORE the local date's UTC midnight — the drift the epochDay
        // arithmetic used to introduce (early-morning reading excluded).
        val zone = ZoneId.of("UTC+05:30")
        val today = LocalDate.of(2026, 9, 24)
        val clock = ZoneClock(today.atTime(1, 0).atZone(zone).toInstant().toEpochMilli(), zone)
        val controller = SessionController(
            scheduler = scheduler,
            ledger = ledger,
            interceptionLog = interceptLog,
            sessionLog = sessionLog,
            positionStore = FakePositionStore(),
            concepts = concepts,
            gate = TrialGate(entitlement, isDebug = false),
            clock = clock,
        )
        // A session inside local day today-6, also inside the UTC blind spot.
        clock.instantMillis = today.minusDays(6).atTime(0, 30).atZone(zone).toInstant().toEpochMilli()
        val early = controller.openSession("crito") as SessionStart.Opened
        sessionLog.addReadingTime(early.sessionId, 60_000L)
        // A session at local 01:00 today — after local midnight, before UTC midnight.
        clock.instantMillis = today.atTime(1, 0).atZone(zone).toInstant().toEpochMilli()
        val late = controller.openSession("demo:apology") as SessionStart.Opened
        sessionLog.addReadingTime(late.sessionId, 120_000L)

        assertEquals(2L, controller.minutesToday()) // only today's local-day session
        assertEquals(3L to 3L, controller.readingMinutes()) // both local days, not raw UTC days
    }

    @Test
    fun `gate - trial ends, purchase unlocks`() = runTest {
        val (clock, controller) = makeController()
        repeat(TrialGate.FREE_SESSION_LIMIT) {
            val s = controller.openSession("demo:apology") as SessionStart.Opened
            controller.completeSession(s.sessionId, 900)
            clock.advance(1)
        }
        assertEquals(SessionStart.Locked, controller.openSession("demo:apology"))
        TrialGate(entitlement, isDebug = false).markOwned()
        assertTrue(controller.openSession("demo:apology") is SessionStart.Opened)
    }

    @Test
    fun `gate - debug builds bypass the lock`() = runTest {
        val controller = makeController(isDebug = true).second
        assertTrue(controller.openSession("demo:apology") is SessionStart.Opened)
    }

    @Test
    fun `openSession resumes at the stored offset`() = runTest {
        val controller = makeController(
            positionStore = FakePositionStore(mapOf("demo:republic" to 4321L)),
        ).second
        val opened = controller.openSession("demo:republic") as SessionStart.Opened
        assertEquals(4321L, sessionLog.get(opened.sessionId)!!.startChar)
    }

    @Test
    fun `openSession with no stored position starts at the beginning`() = runTest {
        val controller = makeController().second
        val opened = controller.openSession("demo:republic") as SessionStart.Opened
        assertEquals(0L, sessionLog.get(opened.sessionId)!!.startChar)
    }

    @Test
    fun `recentSessions returns at most the requested number of completed sessions`() = runTest {
        val controller = makeController().second
        repeat(3) {
            val opened = controller.openSession("demo:apology") as SessionStart.Opened
            controller.completeSession(opened.sessionId, 900)
        }
        assertEquals(2, controller.recentSessions(2).size)
        assertEquals(3, controller.recentSessions(5).size)
        assertEquals(0, controller.recentSessions(0).size)
    }

    @Test
    fun `sessionFor returns the stored session record`() = runTest {
        val controller = makeController().second
        val opened = controller.openSession("demo:republic") as SessionStart.Opened
        val record = controller.sessionFor(opened.sessionId)!!
        assertEquals("demo:republic", record.textId)
        assertEquals(null, controller.sessionFor(404L))
    }

    @Test
    fun `addReadingTime via the seam lands in the reading-minute totals`() = runTest {
        val controller = makeController().second
        val sessionId = (controller.openSession("crito") as SessionStart.Opened).sessionId
        controller.addReadingTime(sessionId, 120_000L)
        assertEquals(2L, controller.minutesToday())
    }

    @Test
    fun `readPercent reflects the stored offset, capped at 100`() = runTest {
        val controller = makeController(
            positionStore = FakePositionStore(mapOf("crito" to 4321L, "over" to 10_500L)),
        ).second
        assertEquals(43, controller.readPercent("crito", 10_000))
        assertEquals(100, controller.readPercent("over", 10_000))
        assertEquals(0, controller.readPercent("never-read", 10_000)) // no stored position
        assertEquals(0, controller.readPercent("crito", 0)) // degenerate text length
    }

    @Test
    fun `masteryCounts buckets the ledger by decayed mastery`() = runTest {
        val (clock, controller) = makeController()
        val today = clock.epochDay
        ledger.upsert(LedgerEntry(conceptId = "m1", repetitions = 6, lastEpochDay = today)) // mastered, fresh
        ledger.upsert(LedgerEntry(conceptId = "m2", repetitions = 6, lastEpochDay = today - 91)) // MASTERED decayed -> PROFICIENT
        ledger.upsert(LedgerEntry(conceptId = "p1", repetitions = 3, lastEpochDay = today)) // proficient, fresh
        ledger.upsert(LedgerEntry(conceptId = "f1", repetitions = 1, lastEpochDay = today - 31)) // FAMILIAR decayed -> UNSEEN
        ledger.upsert(LedgerEntry(conceptId = "f2", repetitions = 1, lastEpochDay = today)) // familiar, fresh
        assertEquals(MasteryCounts(familiar = 1, proficient = 2, mastered = 1), controller.masteryCounts())
    }

    @Test
    fun `replaceWithSession records REPLACED and resumes at the stored offset`() = runTest {
        val controller = makeController(
            positionStore = FakePositionStore(mapOf("demo:republic" to 4321L)),
        ).second
        val start = controller.replaceWithSession("com.instagram.android", "demo:republic") as SessionStart.Opened
        assertEquals(listOf(InterceptionResult.REPLACED), interceptLog.all.map { it.result })
        assertEquals(4321L, sessionLog.get(start.sessionId)!!.startChar)
    }
}

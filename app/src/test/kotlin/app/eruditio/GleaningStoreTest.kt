package app.eruditio

import app.eruditio.core.GleaningDraft
import app.eruditio.core.PendingGleaningDraft
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The durability contract behind Variant C: pending, saved, and discarded are distinct. */
class GleaningStoreTest {

    private val store = FakeGleaningStore()
    private val recovery = FakeGleaningRecovery(store)

    private fun pending(sessionId: Long = 1L) = PendingGleaningDraft(
        sessionId = sessionId,
        textId = "euthyphro",
        startChar = 0L,
        endChar = 5L,
        sourceContext = "Piety",
        curatedContext = null,
    )

    private fun saved(sessionId: Long = 1L, takeaway: String = "A claim can hide a circle.") = GleaningDraft(
        sessionId = sessionId,
        textId = "euthyphro",
        startChar = 0L,
        endChar = 5L,
        takeaway = takeaway,
        sourceContext = "Piety",
        curatedContext = "A local lens",
    )

    @Test
    fun `prepared hand-off is pending, not collected`() = runTest {
        store.prepare(pending())

        assertEquals(1L, store.pending(1L)?.sessionId)
        assertEquals(1L, store.latestPending()?.sessionId)
        assertTrue(store.isHandled(1L))
        assertTrue(store.all().isEmpty())
        assertEquals(0, store.count())
    }

    @Test
    fun `keeping promotes the hand-off into the collection`() = runTest {
        store.prepare(pending())
        val kept = store.save(saved())

        assertEquals("A claim can hide a circle.", kept.takeaway)
        assertNull(store.pending(1L))
        assertNull(store.latestPending())
        assertEquals(listOf(1L), store.all().map { it.sessionId })
        assertEquals(1, store.count())
    }

    @Test
    fun `explicit discard is terminal and never resurrects`() = runTest {
        store.prepare(pending())
        store.complete(1L)
        store.discard(1L)

        assertNull(store.pending(1L))
        assertNull(store.latestPending())
        assertTrue(store.all().isEmpty())
        // The tombstone is what makes it terminal: the recovery scan, which is
        // how Today re-offers an unresolved completion, must skip it.
        assertTrue(store.isHandled(1L))
        assertNull("a discarded session must not be recoverable", recovery.firstUnhandled())
    }

    @Test
    fun `a completed session with no row is recoverable`() = runTest {
        store.complete(7L)
        store.complete(8L)

        assertEquals(7L, recovery.firstUnhandled()?.sessionId)
    }

    @Test
    fun `recovering a session then discarding it clears the queue`() = runTest {
        store.complete(7L)
        val recovered = recovery.firstUnhandled()!!
        store.prepare(pending(recovered.sessionId))
        store.discard(recovered.sessionId)

        assertNull(recovery.firstUnhandled())
    }

    @Test
    fun `latest pending survives process death for the home surface`() = runTest {
        store.prepare(pending(sessionId = 1L))
        store.prepare(pending(sessionId = 2L))
        store.save(saved(sessionId = 1L))

        assertEquals(2L, store.latestPending()?.sessionId)
        assertEquals(listOf(1L), store.all().map { it.sessionId })
    }

    @Test
    fun `saving a recovered session twice keeps one row`() = runTest {
        store.prepare(pending(sessionId = 3L))
        store.save(saved(sessionId = 3L, takeaway = "first"))
        store.save(saved(sessionId = 3L, takeaway = "second"))

        assertEquals(1, store.count())
        assertEquals("second", store.all().single().takeaway)
        assertFalse(store.isHandled(99L))
    }
}

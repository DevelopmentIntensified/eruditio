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
        assertTrue(store.contains(1L))
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
        store.discard(1L)

        assertNull(store.pending(1L))
        assertNull(store.latestPending())
        assertFalse(store.contains(1L))
        assertTrue(store.all().isEmpty())
    }

    @Test
    fun `latest pending survives process death for the home surface`() = runTest {
        store.prepare(pending(sessionId = 1L))
        store.prepare(pending(sessionId = 2L))
        store.save(saved(sessionId = 1L))

        assertEquals(2L, store.latestPending()?.sessionId)
        assertEquals(listOf(1L), store.all().map { it.sessionId })
    }
}

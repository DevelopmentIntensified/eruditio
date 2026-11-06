package app.eruditio

import app.eruditio.core.AnnotationDraft
import app.eruditio.core.AnnotationKind
import app.eruditio.core.wordRangeAt
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The marks contract behind variant A: bookmarks are points, notes carry words. */
class AnnotationStoreTest {

    private val store = FakeAnnotationStore()

    @Test
    fun `for text lists marks by anchor position, per text`() = runTest {
        val late = store.keep(AnnotationDraft("crito", AnnotationKind.BOOKMARK, 900L, 900L))
        val early = store.keep(AnnotationDraft("crito", AnnotationKind.HIGHLIGHT, 24L, 39L))
        store.keep(AnnotationDraft("apology", AnnotationKind.BOOKMARK, 7L, 7L))

        val crito = store.forText("crito")
        assertEquals(listOf(early.id, late.id), crito.map { it.id })
        assertEquals(1, store.forText("apology").size)
    }

    @Test
    fun `a bookmark survives restarts as a point the reader can restore`() = runTest {
        val kept = store.keep(AnnotationDraft("crito", AnnotationKind.BOOKMARK, 240L, 240L))
        val again = store.forText("crito").single()
        assertEquals(kept.startChar, again.startChar)
        assertTrue(again.startChar == again.endChar)
    }

    @Test
    fun `notes carry words and rewrite in place`() = runTest {
        val note = store.keep(
            AnnotationDraft(
                "crito",
                AnnotationKind.NOTE,
                12L,
                26L,
                "never willingly do wrong",
            ),
        )
        val edited = store.rewrite(note.id, "hold this against the city's claim")
        assertEquals(note.id, edited?.id)
        assertEquals("hold this against the city's claim", edited?.noteText)

        assertNull(store.rewrite(999L, "nobody home"))
    }

    @Test
    fun `blank words nowhere (not on keep, not on rewrite)`() = runTest {
        try {
            store.keep(AnnotationDraft("crito", AnnotationKind.NOTE, 1L, 5L, "   "))
            org.junit.Assert.fail("blank note draft must fail")
        } catch (expected: IllegalArgumentException) { }

        val note = store.keep(AnnotationDraft("crito", AnnotationKind.NOTE, 1L, 5L, "word"))
        try {
            store.rewrite(note.id, "")
            org.junit.Assert.fail("blank rewrite must fail")
        } catch (expected: IllegalArgumentException) { }
    }

    @Test
    fun `a highlight is a real range, never a point`() = runTest {
        try {
            store.keep(AnnotationDraft("crito", AnnotationKind.HIGHLIGHT, 10L, 10L))
            org.junit.Assert.fail("point highlight must fail")
        } catch (expected: IllegalArgumentException) { }
    }

    @Test
    fun `drop removes only the dropped mark`() = runTest {
        val a = store.keep(AnnotationDraft("crito", AnnotationKind.BOOKMARK, 10L, 10L))
        val b = store.keep(AnnotationDraft("crito", AnnotationKind.HIGHLIGHT, 12L, 30L))
        store.drop(a.id)

        val left = store.forText("crito")
        assertEquals(listOf(b.id), left.map { it.id })
    }

    @Test
    fun `rewrite on a non-note row is a miss, not a corrupt value`() = runTest {
        val bookmark = store.keep(AnnotationDraft("crito", AnnotationKind.BOOKMARK, 3L, 3L))
        assertNull(store.rewrite(bookmark.id, "attempted"))
        assertEquals(AnnotationKind.BOOKMARK, store.forText("crito").single().kind)
    }
}

/** The pure placement rules the reader depends on for variant A. */
class AnnotationPlacementTest {

    @Test
    fun `long press on a letter lands the enclosing word`() {
        val text = "Men of Athens, I am surprised"
        assertEquals(4..5, wordRangeAt(text, 5))    // "of"
        assertEquals(7..12, wordRangeAt(text, 6))   // space snaps to "Athens"
        assertEquals(0..2, wordRangeAt(text, 0))    // "Men"
    }

    @Test
    fun `long press on or past whitespace snaps forward, then back to the last word`() {
        val text = "Men of Athens "
        assertEquals(4..5, wordRangeAt(text, 3))           // space snaps forward to "of"
        assertEquals(7..12, wordRangeAt(text, 6))          // space before "Athens" snaps to it
        assertEquals(7..12, wordRangeAt(text, text.length)) // trailing: last word
    }

    @Test
    fun `the last word comes back whole at the trailing edge`() {
        val text = "Men of Athens "
        assertEquals(7..12, wordRangeAt(text, text.length))
    }

    @Test
    fun `blank text has no word to anchor`() {
        assertNull(wordRangeAt("   ", 1))
        assertNull(wordRangeAt("", 0))
    }

    @Test
    fun `the out-of-range offsets still clamp to a real word`() {
        val text = "Crito"
        assertNotNull(wordRangeAt(text, 99))
        assertFalse(wordRangeAt(text, 99)!!.isEmpty())
    }
}

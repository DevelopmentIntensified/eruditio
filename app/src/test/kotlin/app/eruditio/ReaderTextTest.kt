package app.eruditio

import app.eruditio.core.Groups
import app.eruditio.core.ReaderText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Seam tests for the reader's segment/checkpoint math (deepening candidate
 * 2): what the reading surface observes through ReaderText — checkpoint
 * rhythm, restore mapping, segment bounds, plate-offset list mapping, and
 * the oversized-block line fallback. Deterministic and table-driven; no
 * Android dependencies. The plate counts as the beginning; persisted
 * offsets stay in TEXT-item space.
 */
class ReaderTextTest {

    /** 12 paragraphs -> items 0..11, item 11 is the FIRST checkpoint. */
    private fun twelve() = ReaderText.of(
        List(12) { "para $it" }.joinToString("\n\n"),
        Groups.BIOGRAPHY,
    )

    // -- checkpoint rhythm -----------------------------------------------

    @Test
    fun `every twelfth text item is a checkpoint - item 11 is the first`() {
        val rt = ReaderText.of(
            List(25) { "para $it" }.joinToString("\n\n"),
            Groups.BIOGRAPHY,
        )
        assertTrue(rt.isCheckpoint(11))
        assertTrue(rt.isCheckpoint(23))
        assertFalse(rt.isCheckpoint(10))
        assertFalse(rt.isCheckpoint(12))
        assertFalse(rt.isCheckpoint(24))
    }

    @Test
    fun `checkpoint rhythm counts text items - the plate shifts nothing`() {
        val without = ReaderText.of(List(12) { "p$it" }.joinToString("\n\n"), Groups.BIOGRAPHY)
        val with = ReaderText.of(List(12) { "p$it" }.joinToString("\n\n"), Groups.ANCIENT)
        assertEquals(1, with.plateItems)
        assertEquals(0, without.plateItems)
        for (i in 0..11) assertEquals(without.isCheckpoint(i), with.isCheckpoint(i))
        // Same text space, only the list mapping differs.
        assertEquals(without.listIndexOf(11) + 1, with.listIndexOf(11))
    }

    // -- oversized-block line fallback ------------------------------------

    @Test
    fun `oversized block splits per line with running offsets`() {
        // 300 numbered lines joined by single newlines: one block with no
        // blank-line separators, well over MAX_ITEM_CHARS -> per-line items.
        val line = "0123456789"
        val big = List(300) { "line$it " + line }.joinToString("\n")
        val rt = ReaderText.of(big, Groups.BIOGRAPHY)
        assertTrue(rt.items.size > 1)
        // Offsets are cumulative and the first item starts at 0.
        assertEquals(0L, rt.items.first().startChar)
        for (i in 1 until rt.items.size) {
            assertTrue(rt.items[i].startChar > rt.items[i - 1].startChar)
            assertFalse(rt.items[i].startsParagraph)
        }
        // Last item's start sits within the text.
        assertTrue(rt.items.last().startChar < rt.textLength)
    }

    @Test
    fun `blank lines preserve offsets between paragraph items`() {
        val rt = ReaderText.of("aaa\n\nbbb\n\nccc", Groups.BIOGRAPHY)
        assertEquals(listOf(0L, 5L, 10L), rt.items.map { it.startChar })
        assertEquals(listOf("aaa", "bbb", "ccc"), rt.items.map { it.text })
        rt.items.forEach { assertTrue(it.startsParagraph) }
    }

    // -- itemIndexAt restore mapping ---------------------------------------

    @Test
    fun `itemIndexAt maps offsets to containing item - first and last boundaries`() {
        val rt = twelve() // 12 items, "para n" = 6 chars (7 from "para 10") + 2 separators
        assertEquals(0, rt.itemIndexAt(0L))
        assertEquals(0, rt.itemIndexAt(5L))
        // Item n covers [n*8, n*8+len).
        assertEquals(1, rt.itemIndexAt(8L))
        assertEquals(11, rt.itemIndexAt(11L * 8 + 1)) // item 11's start (89; two-digit shift)
        assertEquals(11, rt.itemIndexAt(rt.textLength - 1))
    }

    @Test
    fun `itemIndexAt past the end maps to the last item`() {
        val rt = twelve()
        assertEquals(11, rt.itemIndexAt((rt.textLength + 1000)))
    }

    // -- segmentEndAfter (both pause-at-checkpoint and check-in) -----------

    @Test
    fun `segment end at a checkpoint is the next item's start`() {
        val rt = twelve()
        // Item 11 is a checkpoint; the next item (12, absent here) ... use item 0.
        assertEquals(rt.items[1].startChar, rt.segmentEndAfter(0))
        // Checkpoint boundary: pause AT item 11 waits for a non-existent 12 -> text end only in midlist
        val rt24 = ReaderText.of(
            List(13) { "p$it" }.joinToString("\n\n"),
            Groups.BIOGRAPHY,
        )
        assertEquals(rt24.items[12].startChar, rt24.segmentEndAfter(11))
    }

    @Test
    fun `segment end after the last item is the text length`() {
        val rt = twelve()
        assertEquals(rt.textLength, rt.segmentEndAfter(11))
        assertEquals(rt.textLength, rt.segmentEndAfter(100))
    }

    // -- text <-> list mapping (plate round-trip) ---------------------------

    @Test
    fun `text to list and back round-trips with and without a plate`() {
        for (group in listOf(Groups.BIOGRAPHY, Groups.ANCIENT)) {
            val rt = ReaderText.of(List(30) { "p$it" }.joinToString("\n\n"), group)
            for (t in 0 until rt.items.size) {
                assertEquals(t, rt.textIndexOf(rt.listIndexOf(t)))
            }
            // Clamped above the plate: list index under the offset maps to 0.
            if (rt.plateItems > 0) assertEquals(0, rt.textIndexOf(rt.plateItems - 1))
        }
    }

    @Test
    fun `list count equals text items plus the plate`() {
        val without = ReaderText.of(List(30) { "p$it" }.joinToString("\n\n"), Groups.BIOGRAPHY)
        val with = ReaderText.of(List(30) { "p$it" }.joinToString("\n\n"), Groups.ANCIENT)
        assertEquals(without.listItemCount + 1, with.listItemCount)
        assertEquals(30, with.items.size)
    }

    // -- empty-text edge -----------------------------------------------------

    @Test
    fun `empty text yields one blank item anchored at zero`() {
        val rt = ReaderText.of("", Groups.BIOGRAPHY)
        assertEquals(1, rt.items.size)
        assertEquals(0L, rt.items[0].startChar)
        assertEquals("", rt.items[0].text)
        assertEquals(0, rt.itemIndexAt(0L))
        assertNull(rt.startCharAt(1))
    }

    // -- startCharAt ----------------------------------------------------------

    @Test
    fun `startCharAt gives item starts and null past the end`() {
        val rt = twelve()
        assertEquals(0L, rt.startCharAt(0))
        assertEquals(8L, rt.startCharAt(1))
        assertNull(rt.startCharAt(12))
    }
}

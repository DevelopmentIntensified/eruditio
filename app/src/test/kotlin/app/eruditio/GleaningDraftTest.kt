package app.eruditio

import app.eruditio.core.GleaningDraft
import app.eruditio.core.canonicalProse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GleaningDraftTest {

    @Test
    fun `draft trims prose and keeps its source anchor`() {
        val draft = GleaningDraft(
            sessionId = 7L,
            textId = "euthyphro",
            startChar = 120L,
            endChar = 480L,
            takeaway = "  A confident answer can still be circular.  ",
            sourceContext = "  Piety, then, is that which is dear to the gods.  ",
            curatedContext = "  Notice what the answer treats as already settled.  ",
        )

        assertEquals("A confident answer can still be circular.", draft.takeaway)
        assertEquals("  Piety, then, is that which is dear to the gods.  ", draft.sourceContext)
        assertEquals("Notice what the answer treats as already settled.", draft.curatedContext)
        assertEquals(120L, draft.startChar)
        assertEquals(480L, draft.endChar)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `draft rejects a blank takeaway`() {
        GleaningDraft(1L, "text", 0L, 10L, " \n", "source", "lens")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `draft rejects an empty source range`() {
        GleaningDraft(1L, "text", 10L, 10L, "takeaway", "source", "lens")
    }

    @Test
    fun `source builder preserves the exact local excerpt`() {
        val excerpt = app.eruditio.core.GleaningSourceBuilder.build(
            text = "  first\n second   third  ",
            requestedStart = 2L,
            requestedEnd = 25L,
        )

        assertEquals(2L, excerpt?.startChar)
        assertEquals("first\n second   third", excerpt?.text)
    }

    @Test
    fun `anchor round trips, so the slice re-slices to the stored source`() {
        // The anchor is the citation. Trimming the edges must move the offsets
        // with them, so `text[start, end)` is always exactly what is displayed.
        val text = "  first\n second   third  "
        val excerpt = app.eruditio.core.GleaningSourceBuilder.build(text, 0L, text.length.toLong())!!

        val start = excerpt.startChar.toInt()
        val end = excerpt.endChar.toInt()
        assertEquals(excerpt.text, text.substring(start, end))
        assertEquals("first\n second   third", excerpt.text)
    }

    @Test
    fun `pending draft captures source before the reader writes`() {
        val pending = app.eruditio.core.GleaningDraftFactory.pending(
            sessionId = 4L,
            textId = "apology",
            startChar = 0L,
            endChar = 17L,
            text = "  first thought  ",
            contexts = app.eruditio.core.GleaningContextSource { _, _ -> null },
        )

        assertEquals("first thought", pending?.sourceContext)
        assertEquals(null, pending?.curatedContext)
    }

    @Test
    fun `curated context is supplied by an offline source, never fabricated`() {
        val contexts = app.eruditio.core.GleaningContextSource { textId, source ->
            if (textId == "euthyphro" && source.contains("Piety")) "A local lens" else null
        }
        val material = app.eruditio.core.GleaningDraftFactory.source(
            textId = "euthyphro",
            startChar = 0L,
            endChar = 5L,
            text = "Piety",
            contexts = contexts,
        )
        assertEquals("Piety", material?.sourceContext)
        assertEquals("A local lens", material?.curatedContext)
        assertEquals(null, contexts.forSource("apology", "A passage"))
    }

    @Test
    fun `anchors match across the corpus line wrapping`() {
        val wrapped = "and that the\nunexamined life is not worth living, you are"
        val anchor = "the unexamined life is not worth living"

        // Both sides are canonicalised, so a hard-wrapped corpus line still
        // matches a single-line catalog anchor (this is why the second catalog
        // entry can exist at all).
        assertTrue(canonicalProse(wrapped).contains(canonicalProse(anchor)))
    }
}

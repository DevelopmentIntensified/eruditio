package app.eruditio

import app.eruditio.core.GleaningDraft
import org.junit.Assert.assertEquals
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
        assertEquals("first\n second   third  ", excerpt?.text)
    }

    @Test
    fun `pending draft captures source before the reader writes`() {
        val pending = app.eruditio.core.GleaningDraftFactory.pending(
            sessionId = 4L,
            textId = "apology",
            startChar = 0L,
            endChar = 17L,
            text = "  first thought  ",
        )

        assertEquals("  first thought  ", pending?.sourceContext)
        assertEquals(null, pending?.curatedContext)
    }

    @Test
    fun `curated lenses are optional and passage-specific`() {
        assertEquals(
            "Euthyphro’s answer sounds exact, but it hides a circle: the action is called pious because the gods approve it, while approval is inferred from the action’s piety.",
            app.eruditio.core.GleaningLens.forSource(
                "euthyphro",
                "Piety, then, is that which is dear to the gods",
            ),
        )
        assertEquals(null, app.eruditio.core.GleaningLens.forSource("apology", "A passage"))
    }
}

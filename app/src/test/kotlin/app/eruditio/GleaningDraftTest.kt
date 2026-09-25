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
        assertEquals("Piety, then, is that which is dear to the gods.", draft.sourceContext)
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
    fun `bundled lenses stay local to the content group`() {
        assertEquals(
            "What does this passage invite you to practice, not merely admire?",
            app.eruditio.core.GleaningLens.forGroup("christian"),
        )
        assertEquals(
            "What does this passage ask you to notice—and question?",
            app.eruditio.core.GleaningLens.forGroup("ancient"),
        )
    }
}

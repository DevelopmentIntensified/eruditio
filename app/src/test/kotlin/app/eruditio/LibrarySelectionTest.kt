package app.eruditio

import app.eruditio.data.TextMeta
import app.eruditio.ui.LibraryFilter
import app.eruditio.ui.ProgressMode
import app.eruditio.ui.Segment
import app.eruditio.ui.isUnstarted
import app.eruditio.ui.suggestedNext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Consumer-behavior tests for the shared library-selection predicates: the
 * three filter axes composed together, the percent-mode boundaries, and the
 * check-in "next reading" pick. The "N of M" match count is the caller's
 * concern — these tests assert the filtered list itself.
 */
class LibrarySelectionTest {

    private fun meta(id: String, title: String, author: String, group: String) =
        TextMeta(id, title, author, "$id.txt", group)

    private val ancient1 = meta("aeneid", "Aeneid", "Virgil", "ancient")
    private val ancient2 = meta("odyssey", "The Odyssey", "Homer", "ancient")
    private val christian = meta("confessions", "Confessions", "Augustine", "christian")
    private val lives = meta("livingstone", "Livingstone", "Thomas Hughes", "biography")
    private val library = listOf(ancient1, christian, ancient2, lives)

    /** progressById keyed the way the VM and the reader build it. */
    private val progress = mapOf(
        "aeneid" to 40, // in progress
        "confessions" to 0, // unread
        "odyssey" to 100, // read
        // livingstone missing from the map — reads as 0 (unstarted)
    )

    @Test
    fun `three axes compose together`() {
        // Segment ancient ∧ query matching two ancients ∧ in-progress keeps only one.
        val filter = LibraryFilter(Segment.Classics, "e", ProgressMode.InProgress)
        assertEquals(listOf(ancient1), filter.apply(library, progress))

        // Same segment+query, unread mode, picks the unstarted ancient only.
        val unstartedAncients = mapOf("aeneid" to 40, "odyssey" to 0, "confessions" to 0, "livingstone" to 0)
        val unread = filter.copy(progress = ProgressMode.Unread)
        assertEquals(listOf(ancient2), unread.apply(library, unstartedAncients))
    }

    @Test
    fun `query matches title and author case-insensitively`() {
        val title = LibraryFilter(null, "AENE", ProgressMode.Any)
        assertEquals(listOf(ancient1), title.apply(library, progress))

        val author = LibraryFilter(null, "augustine", ProgressMode.Any)
        assertEquals(listOf(christian), author.apply(library, progress))

        val noMatch = LibraryFilter(null, "plato", ProgressMode.Any)
        assertTrue(noMatch.apply(library, progress).isEmpty())
    }

    @Test
    fun `blank query keeps every segment-and-progress match in library order`() {
        val all = LibraryFilter(null, "  ", ProgressMode.Any)
        assertEquals(library, all.apply(library, progress))
    }

    @Test
    fun `segment selects its group, null or All keeps all groups`() {
        val classics = LibraryFilter(Segment.Classics, "", ProgressMode.Any)
        assertEquals(listOf(ancient1, ancient2), classics.apply(library, progress))

        val biography = LibraryFilter(Segment.Lives, "", ProgressMode.Any)
        assertEquals(listOf(lives), biography.apply(library, progress))

        val noSegment = LibraryFilter(null, "", ProgressMode.Any)
        assertEquals(library, noSegment.apply(library, progress))

        val allSegment = LibraryFilter(Segment.All, "", ProgressMode.Any)
        assertEquals(library, allSegment.apply(library, progress))
    }

    @Test
    fun `progress boundaries - unread is 0 only, read excluded, missing reads as 0`() {
        val unread = LibraryFilter(null, "", ProgressMode.Unread).apply(library, progress)
        // 0 (confessions) and missing-from-map (livingstone) are unread;
        // 100 (odyssey) is read and excluded, 40 (aeneid) is started.
        assertEquals(listOf(christian, lives), unread)

        val inProgress = LibraryFilter(null, "", ProgressMode.InProgress).apply(library, progress)
        assertEquals(listOf(ancient1), inProgress)

        val any = LibraryFilter(null, "", ProgressMode.Any).apply(library, progress)
        assertEquals(library, any)

        // A fully read text never reappears as unread, and a text at 1 is no
        // longer unread.
        val edge = mapOf("aeneid" to 1, "confessions" to 100)
        val filters = LibraryFilter(null, "", ProgressMode.Unread).apply(library, edge)
        assertEquals(listOf(ancient2, lives), filters)
    }

    @Test
    fun `isUnstarted is the one threshold - 0 yes, 1 and 100 no`() {
        assertTrue(isUnstarted(0))
        assertFalse(isUnstarted(1))
        assertFalse(isUnstarted(100))
    }

    @Test
    fun `suggestedNext prefers same group, unstarted, in library order`() {
        // Both ancients unstarted except current: skips current, keeps same group.
        val p = mapOf("aeneid" to 0, "odyssey" to 0, "confessions" to 0, "livingstone" to 0)
        val next = suggestedNext(library, p, "aeneid", "ancient")
        assertEquals(ancient2, next)
    }

    @Test
    fun `suggestedNext falls back to any-group unstarted, order preserved`() {
        // Ancients all started; first unstarted anywhere wins (christian before lives).
        val p = mapOf("aeneid" to 40, "odyssey" to 100, "confessions" to 0, "livingstone" to 0)
        assertEquals(christian, suggestedNext(library, p, "aeneid", "ancient"))
    }

    @Test
    fun `suggestedNext never suggests the current text`() {
        val p = mapOf("aeneid" to 0, "odyssey" to 40, "confessions" to 40, "livingstone" to 40)
        // Only the current text is unstarted -> nothing to suggest.
        assertNull(suggestedNext(library, p, "aeneid", "ancient"))
    }

    @Test
    fun `suggestedNext returns null when everything has progress`() {
        val p = mapOf("aeneid" to 40, "odyssey" to 40, "confessions" to 40, "livingstone" to 100)
        assertNull(suggestedNext(library, p, "aeneid", "ancient"))
    }

    @Test
    fun `suggestedNext treats a missing progress entry as unstarted`() {
        val p = emptyMap<String, Int>()
        assertEquals(ancient1, suggestedNext(library, p, "odyssey", "ancient"))
    }
}

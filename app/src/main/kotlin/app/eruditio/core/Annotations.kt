package app.eruditio.core

/**
 * Reading marks on-device (tickets 26→27): bookmark, highlight, note
 * (owner-picked prototype variant A — "selection" marks). Nothing here is
 * collected or synced (ADR-0006 posture): marks stay in their own table and
 * never enter the review ledger.
 *
 * Anchors live in the normalized character space — the same space reading
 * positions and sessions already use (each asset is normalized exactly once,
 * in TextLibrary) — so a mark survives restarts, app updates and Room
 * schema-less behavior across the checkpoint math.
 */
enum class AnnotationKind {
    BOOKMARK,
    HIGHLIGHT,
    NOTE,
}

/**
 * One kept mark. A bookmark is a point (start == end); a highlight and a
 * note are a real character range. A note carries the reader's own words in
 * [noteText]; the other kinds keep it null.
 */
data class Annotation(
    val id: Long,
    val textId: String,
    val kind: AnnotationKind,
    val startChar: Long,
    val endChar: Long,
    val noteText: String? = null,
) {
    init {
        require(id >= 0L) { "An annotation needs a row id" }
        require(textId.isNotBlank()) { "An annotation needs its source text" }
        require(startChar >= 0L && endChar >= startChar) { "An annotation needs a valid range" }
        require(kind != AnnotationKind.BOOKMARK || startChar == endChar) {
            "A bookmark anchors a point"
        }
        require(kind == AnnotationKind.BOOKMARK || endChar > startChar) {
            "A highlight or note needs a real range"
        }
        require(kind != AnnotationKind.NOTE || !noteText.isNullOrBlank()) {
            "A note needs the reader's own words"
        }
    }
}

/** Value before it has a row: the store allocates the id and the timestamp. */
data class AnnotationDraft(
    val textId: String,
    val kind: AnnotationKind,
    val startChar: Long,
    val endChar: Long,
    val noteText: String? = null,
)

/** The one seam behind all reader marks (variant A). */
interface AnnotationStore {
    /** Persists a draft and returns the stored annotation. */
    suspend fun keep(draft: AnnotationDraft): Annotation

    /** Replaces the reader's own words on an existing NOTE row; null when unknown. */
    suspend fun rewrite(id: Long, noteText: String): Annotation?

    /** Every kept mark for one text, ordered by anchor position. */
    suspend fun forText(textId: String): List<Annotation>

    /** Removes the mark. */
    suspend fun drop(id: Long)
}

/**
 * The word the long-press landed in (variant A's highlight/note target when
 * there is no drag selection): the run of non-whitespace characters around
 * `offset`. Offset on whitespace snaps forward to the next word; at the very
 * end it snaps back to the last word. Returns null when the text has no word
 * at all. Indices are in the same space [Annotation.startChar] uses once the
 * caller adds the paragraph's `startChar`.
 */
fun wordRangeAt(text: String, offset: Int): IntRange? {
    if (text.isBlank()) return null
    val o = offset.coerceIn(0, text.length)
    // Punctuation does not extend a word: a trailing comma belongs to the
    // sentence, not to the highlighted span. Simplified word rule; drag
    // selection can subsume it later.
    val isWord = { i: Int -> text[i].isLetterOrDigit() }
    fun wordStartFrom(i: Int): Int {
        var s = i
        while (s > 0 && isWord(s - 1)) s--
        return s
    }
    // On a word: it encloses the offset. On whitespace: snap forward to the
    // next word; past the last word: the trailing run falls back to the last
    // word in the text.
    var start = if (o < text.length && isWord(o)) o else {
        var probe = o
        while (probe < text.length && !isWord(probe)) probe++
        probe
    }
    if (start >= text.length) {
        var last = text.length - 1
        while (last >= 0 && !isWord(last)) last--
        if (last < 0) return null
        start = last
    }
    val wordStart = wordStartFrom(start)
    var end = start
    while (end + 1 < text.length && isWord(end + 1)) end++
    return wordStart..end
}

/** Where a mark sits in reading order, for labels: 1-based TEXT-item index. */
fun annotationItemIndex(annotation: Annotation, reader: ReaderText): Int =
    reader.itemIndexAt(annotation.startChar)

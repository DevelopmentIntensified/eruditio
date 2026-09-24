package app.eruditio.core

/**
 * One reading item: a paragraph (block split on blank lines), or a single
 * line of an oversized block.
 */
data class ReaderItem(
    val text: String,
    val startChar: Long,
    val startsParagraph: Boolean,
)

/**
 * The reader's character checkpoint math (deepening candidate 2): the
 * TEXT-segment arithmetic behind the reading surface, extracted as a pure
 * module — no Android or Compose imports.
 *
 * Callers speak TEXT-item space only: checkpoints, segment bounds, restore
 * mapping, and every persisted offset live in text-item space (the store
 * contract, ticket 21). The list-space plate offset stays INTERNAL —
 * `plateItems` bounds the frontispiece region, and `listIndexOf` /
 * `textIndexOf` exist solely for the LazyList scroll boundaries
 * (firstVisibleItemIndex reads, scrollToItem restores, rail ticks).
 *
 * Plate frontispiece (ticket 21): ancient-track texts prepend ONE plate item
 * before all text items, like a book frontispiece. Checkpoint math counts
 * TEXT items — the plate shifts the list index but never the rhythm, and the
 * first visible item IS still the place, including when it is the plate.
 */
class ReaderText private constructor(
    /** Every text item, in reading order. */
    val items: List<ReaderItem>,
    /** Total character count of the source text. */
    val textLength: Long,
    /** Number of leading scroll-list items that are the plate (1 ancient, else 0). */
    val plateItems: Int,
) {
    companion object {
        /** Checkpoint spacing (ticket 14, variant A widened): an item whose
         * index sits at the rhythm's end (every 12th) is a checkpoint — a
         * gutter dot, and a quiet "Pause here" chip while one is on screen. */
        private const val CHECKPOINT_SPACING = 12

        /** Blocks larger than this are cut into per-line items. */
        private const val MAX_ITEM_CHARS = 2000

        /** Normalize a source text: split into items with cumulative char offsets. */
        fun of(text: String, group: String): ReaderText = ReaderText(
            items = splitItems(text),
            textLength = text.length.toLong(),
            plateItems = if (group == Groups.ANCIENT) 1 else 0,
        )

        /**
         * Split with cumulative character offsets for exact restore. Blocks
         * split on blank lines keep their offsets; a block larger than a
         * couple of screens (verse runs with no blank lines) is cut into
         * per-line items so progress and restore stay granular.
         */
        private fun splitItems(text: String): List<ReaderItem> {
            val items = mutableListOf<ReaderItem>()
            var start = 0L
            for (block in text.split("\n\n")) {
                val blockStart = start
                start += block.length.toLong() + 2
                if (block.isBlank()) continue
                val trimmed = block.trim()
                if (trimmed.length <= MAX_ITEM_CHARS) {
                    items += ReaderItem(trimmed, blockStart, true)
                } else {
                    var idx = 0
                    var first = true
                    for (line in block.split("\n")) {
                        val t = line.trim()
                        if (t.isNotEmpty()) {
                            items += ReaderItem(t, blockStart + idx, first)
                            first = false
                        }
                        idx += line.length + 1
                    }
                }
            }
            if (items.isEmpty()) items += ReaderItem(text.trim(), 0L, true)
            return items
        }
    }

    /** Is this TEXT-item index a checkpoint (sits at the spacing rhythm's end)? */
    fun isCheckpoint(textItemIndex: Int): Boolean =
        textItemIndex % CHECKPOINT_SPACING == CHECKPOINT_SPACING - 1

    /**
     * Bounds of what was read when this TEXT-item is the top visible item:
     * the next item's start, or the text end. Single home for the
     * segment-end arithmetic (the pause-at-checkpoint and check-in paths
     * both read through here).
     */
    fun segmentEndAfter(textItemIndex: Int): Long =
        if (textItemIndex + 1 < items.size) items[textItemIndex + 1].startChar else textLength

    /** Start char of this TEXT-item, or null past the end. */
    fun startCharAt(textItemIndex: Int): Long? = items.getOrNull(textItemIndex)?.startChar

    /** Restore mapping: character offset -> TEXT-item index that contains it. */
    fun itemIndexAt(charOffset: Long): Int {
        var best = 0
        for ((index, item) in items.withIndex()) {
            if (item.startChar <= charOffset) best = index else break
        }
        return best
    }

    /** TEXT-item index -> scroll-list index (the plate shift is applied here). */
    fun listIndexOf(textItemIndex: Int): Int = textItemIndex + plateItems

    /** Scroll-list index -> TEXT-item index, clamped above the plate. */
    fun textIndexOf(listItemIndex: Int): Int = (listItemIndex - plateItems).coerceAtLeast(0)

    /** Scroll-list item count: every text item plus the plate items. */
    val listItemCount: Int get() = items.size + plateItems
}

package app.eruditio.ui

import app.eruditio.core.Groups
import app.eruditio.data.TextMeta

/**
 * Pure selection predicates for the library surface, extracted from the
 * Compose call sites so the axes (group segment, text query, progress mode)
 * and the "next reading" picks are testable on the JVM and live in one place.
 * Percent thresholds have exactly one definition here; call sites never write
 * bare percent literals.
 */

/**
 * The one unstarted predicate: percent 0. Ticket 13 semantics — "read"
 * percent is 0..100, so 0 is unstarted, 100 is fully read, 1..99 is in
 * progress. A read text (100) is never unread.
 */
fun isUnstarted(percent: Int): Boolean = percent == 0

/** Library segment bar: content groups, cycled by tap (variant B). */
enum class Segment(val label: String, val group: String?) {
    All("All", null),
    Classics("Classics", Groups.ANCIENT),
    Christian("Christian", Groups.CHRISTIAN),
    Lives("Lives", Groups.BIOGRAPHY),
}

/** Progress pill modes: read percent (0..100) decides inclusion. */
enum class ProgressMode(val label: String, val matches: (Int) -> Boolean) {
    Any("any progress", { _ -> true }),
    InProgress("in progress", { pct -> pct in 1..99 }),
    Unread("unread", ::isUnstarted),
}

/**
 * The library compound filter: segment group ∧ text query (title or author,
 * case-insensitive) ∧ progress mode, composed exactly as the Library screen
 * has always composed them. [apply] preserves library order; the "N of M"
 * match count is the caller's concern, derived from the filtered list size.
 */
data class LibraryFilter(
    val segment: Segment?,
    val query: String,
    val progress: ProgressMode,
) {
    fun apply(texts: List<TextMeta>, progressById: Map<String, Int>): List<TextMeta> =
        texts.filter { t ->
            (segment?.group == null || t.group == segment.group) &&
                (query.isBlank() ||
                    t.title.contains(query, ignoreCase = true) ||
                    t.author.contains(query, ignoreCase = true)) &&
                progress.matches(progressById[t.id] ?: 0)
        }
}

/**
 * Recommended next reading after a check-in: another unstarted classic, same
 * content group first (in-app momentum over feeds), else any unstarted text;
 * library order preserved, the text just finished never suggests itself.
 * Returns null when nothing is unstarted.
 */
fun suggestedNext(
    library: List<TextMeta>,
    progressById: Map<String, Int>,
    currentTextId: String,
    currentGroup: String,
): TextMeta? =
    library.firstOrNull {
        it.id != currentTextId && it.group == currentGroup && isUnstarted(progressById[it.id] ?: 0)
    } ?: library.firstOrNull {
        it.id != currentTextId && isUnstarted(progressById[it.id] ?: 0)
    }

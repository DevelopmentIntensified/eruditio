package app.eruditio.core

/**
 * The content groups: the one vocabulary for text grouping, carried by the
 * asset index (`TextMeta.group` parses the header token). Definition sites
 * (segment bars, handoff registry, plate headers) reference these constants —
 * never bare literals.
 */
object Groups {
    const val ANCIENT = "ancient"
    const val CHRISTIAN = "christian"
    const val BIOGRAPHY = "biography"
}

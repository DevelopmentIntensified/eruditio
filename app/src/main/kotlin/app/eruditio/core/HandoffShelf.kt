package app.eruditio.core

/**
 * The handoff shelf (spec): a small static registry of stable deep links
 * (Khan Academy, Exercism) shown post-session only. If this grows beyond a
 * trivial list of links, it gets cut from MVP rather than expanded.
 */
data class HandoffLink(val title: String, val url: String)

object HandoffShelf {
    // Content groups carried by the text index; one curated destination each.
    private const val GROUP_ANCIENT = "ancient"
    private const val GROUP_CHRISTIAN = "christian"
    private const val GROUP_BIOGRAPHY = "biography"

    private val byGroup = mapOf(
        GROUP_ANCIENT to HandoffLink(
            "World History: Beginnings",
            "https://www.khanacademy.org/humanities/world-history/world-history-beginnings",
        ),
        GROUP_CHRISTIAN to HandoffLink(
            "Grammar foundations",
            "https://www.khanacademy.org/humanities/grammar",
        ),
        GROUP_BIOGRAPHY to HandoffLink(
            "Kotlin track on Exercism",
            "https://exercism.org/tracks/kotlin",
        ),
    )

    /** One relevant next step per text group; null for unknown groups. */
    fun suggest(contentGroup: String?): HandoffLink? = contentGroup?.let { byGroup[it] }
}

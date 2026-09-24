package app.eruditio.core

/**
 * The handoff shelf (spec): a small static registry of stable deep links
 * (Khan Academy, Exercism) shown post-session only. If this grows beyond a
 * trivial list of links, it gets cut from MVP rather than expanded.
 */
data class HandoffLink(val title: String, val url: String)

object HandoffShelf {
    // One curated destination per content group (tokens defined in Groups).
    private val byGroup = mapOf(
        Groups.ANCIENT to HandoffLink(
            "World History: Beginnings",
            "https://www.khanacademy.org/humanities/world-history/world-history-beginnings",
        ),
        Groups.CHRISTIAN to HandoffLink(
            "Grammar foundations",
            "https://www.khanacademy.org/humanities/grammar",
        ),
        Groups.BIOGRAPHY to HandoffLink(
            "Kotlin track on Exercism",
            "https://exercism.org/tracks/kotlin",
        ),
    )

    /** One relevant next step per text group; null for unknown groups. */
    fun suggest(contentGroup: String?): HandoffLink? = contentGroup?.let { byGroup[it] }
}

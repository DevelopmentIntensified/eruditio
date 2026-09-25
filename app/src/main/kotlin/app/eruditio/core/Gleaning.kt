package app.eruditio.core

import java.util.Locale

/** The user's source-linked takeaway after a reading session. */
class GleaningDraft(
    val sessionId: Long,
    val textId: String,
    val startChar: Long,
    val endChar: Long,
    takeaway: String,
    sourceContext: String,
    curatedContext: String,
) {
    val takeaway: String = takeaway.trim()
    val sourceContext: String = sourceContext.trim()
    val curatedContext: String = curatedContext.trim()

    init {
        require(sessionId > 0L) { "A gleaning needs a session" }
        require(textId.isNotBlank()) { "A gleaning needs a source text" }
        require(startChar >= 0L && endChar > startChar) { "A gleaning needs a source range" }
        require(this.takeaway.isNotBlank()) { "A gleaning needs the reader's takeaway" }
        require(this.sourceContext.isNotBlank()) { "A gleaning needs source context" }
        require(this.curatedContext.isNotBlank()) { "A gleaning needs a curated lens" }
    }
}

/** A saved gleaning, with the source anchor retained for later browsing. */
data class Gleaning(
    val sessionId: Long,
    val textId: String,
    val startChar: Long,
    val endChar: Long,
    val takeaway: String,
    val sourceContext: String,
    val curatedContext: String,
    val createdAtMillis: Long,
)

/** Local editorial prompts used to compare a takeaway with its source. */
object GleaningLens {
    fun forGroup(group: String): String = when (group.lowercase(Locale.ROOT)) {
        Groups.CHRISTIAN -> "What does this passage invite you to practice, not merely admire?"
        Groups.BIOGRAPHY -> "What choice or habit does this life make visible?"
        else -> "What does this passage ask you to notice—and question?"
    }
}

data class SourceExcerpt(
    val startChar: Long,
    val endChar: Long,
    val text: String,
)

/** Deterministic source snapshot preparation; no Android or UI dependencies. */
object GleaningSourceBuilder {
    fun build(
        text: String,
        requestedStart: Long,
        requestedEnd: Long,
    ): SourceExcerpt? {
        val textLength = text.length.toLong()
        if (textLength == 0L || requestedStart < 0L || requestedEnd > textLength || requestedEnd <= requestedStart) {
            return null
        }
        val start = requestedStart.toInt()
        val end = requestedEnd.toInt()
        val excerpt = text.substring(start, end)
        if (excerpt.isBlank()) return null
        return SourceExcerpt(start.toLong(), end.toLong(), excerpt)
    }
}

/** Local persistence seam for gleanings; no scheduler or grading state. */
interface GleaningStore {
    suspend fun save(draft: GleaningDraft): Gleaning
    suspend fun all(): List<Gleaning>
    suspend fun count(): Int
}

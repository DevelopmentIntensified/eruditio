package app.eruditio.core

/** The user's source-linked takeaway after a reading session. */
class GleaningDraft(
    val sessionId: Long,
    val textId: String,
    val startChar: Long,
    val endChar: Long,
    takeaway: String,
    sourceContext: String,
) {
    val takeaway: String = takeaway.trim()
    val sourceContext: String = sourceContext.trim()

    init {
        require(sessionId > 0L) { "A gleaning needs a session" }
        require(textId.isNotBlank()) { "A gleaning needs a source text" }
        require(startChar >= 0L && endChar > startChar) { "A gleaning needs a source range" }
        require(this.takeaway.isNotBlank()) { "A gleaning needs the reader's takeaway" }
        require(this.sourceContext.isNotBlank()) { "A gleaning needs source context" }
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
    val createdAtMillis: Long,
)

/** Local persistence seam for gleanings; no scheduler or grading state. */
interface GleaningStore {
    suspend fun save(draft: GleaningDraft): Gleaning
    suspend fun recent(limit: Int): List<Gleaning>
}

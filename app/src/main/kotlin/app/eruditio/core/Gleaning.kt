package app.eruditio.core

/** The user's source-linked takeaway after a reading session. */
class GleaningDraft(
    val sessionId: Long,
    val textId: String,
    val startChar: Long,
    val endChar: Long,
    takeaway: String,
    sourceContext: String,
    curatedContext: String?,
) {
    val takeaway: String = takeaway.trim()
    val sourceContext: String = sourceContext
    val curatedContext: String? = curatedContext?.trim()?.takeIf { it.isNotEmpty() }

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
    val curatedContext: String?,
    val createdAtMillis: Long,
)

/** A durable completion hand-off waiting for the reader's takeaway. */
class PendingGleaningDraft(
    val sessionId: Long,
    val textId: String,
    val startChar: Long,
    val endChar: Long,
    sourceContext: String,
    curatedContext: String?,
) {
    val sourceContext: String = sourceContext
    val curatedContext: String? = curatedContext?.trim()?.takeIf { it.isNotEmpty() }

    init {
        require(sessionId > 0L)
        require(textId.isNotBlank())
        require(startChar >= 0L && endChar > startChar)
        require(this.sourceContext.isNotBlank())
    }
}

data class GleaningRecovery(
    val sessionId: Long,
    val textId: String,
    val startChar: Long,
    val endChar: Long,
)

data class PendingGleaning(
    val sessionId: Long,
    val textId: String,
    val startChar: Long,
    val endChar: Long,
    val sourceContext: String,
    val curatedContext: String?,
    val createdAtMillis: Long,
)

fun interface GleaningContextSource {
    fun forSource(textId: String, sourceContext: String): String?
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
        var start = requestedStart.toInt()
        var end = requestedEnd.toInt()
        while (start < end && text[start].isWhitespace()) start++
        while (end > start && text[end - 1].isWhitespace()) end--
        if (start >= end) return null
        return SourceExcerpt(start.toLong(), end.toLong(), text.substring(start, end))
    }
}

data class GleaningSourceMaterial(
    val startChar: Long,
    val endChar: Long,
    val sourceContext: String,
    val curatedContext: String?,
)

object GleaningDraftFactory {
    fun source(
        textId: String,
        startChar: Long,
        endChar: Long,
        text: String,
        curatedContext: String? = null,
    ): GleaningSourceMaterial? {
        val excerpt = GleaningSourceBuilder.build(text, startChar, endChar) ?: return null
        return GleaningSourceMaterial(
            startChar = excerpt.startChar,
            endChar = excerpt.endChar,
            sourceContext = excerpt.text,
            curatedContext = curatedContext,
        )
    }

    fun pending(
        sessionId: Long,
        textId: String,
        startChar: Long,
        endChar: Long,
        text: String,
        curatedContext: String? = null,
    ): PendingGleaningDraft? {
        val source = source(textId, startChar, endChar, text, curatedContext) ?: return null
        return PendingGleaningDraft(
            sessionId = sessionId,
            textId = textId,
            startChar = source.startChar,
            endChar = source.endChar,
            sourceContext = source.sourceContext,
            curatedContext = source.curatedContext,
        )
    }
}

/** Local persistence seam for gleanings; no scheduler or grading state. */
interface GleaningStore {
    suspend fun prepare(draft: PendingGleaningDraft): PendingGleaning
    suspend fun pending(sessionId: Long): PendingGleaning?
    suspend fun latestPending(): PendingGleaning?
    suspend fun discard(sessionId: Long)
    suspend fun contains(sessionId: Long): Boolean
    suspend fun save(draft: GleaningDraft): Gleaning
    suspend fun all(): List<Gleaning>
    suspend fun count(): Int
}

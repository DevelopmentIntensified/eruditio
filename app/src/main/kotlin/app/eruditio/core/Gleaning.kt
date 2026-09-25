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

/**
 * The outcome of a hand-off attempt. Modelled as a result rather than a Boolean
 * so callers must distinguish "already resolved" (navigate away) from "could
 * not persist" (stay put, leave the session open).
 */
enum class GleaningHandoff { PREPARED, ALREADY_HANDLED, UNAVAILABLE }

/**
 * Finds a completed session the reader has not yet resolved. Implemented as one
 * join so recovery is neither bounded by a page size nor an N+1 scan.
 */
interface GleaningRecoverySource {
    suspend fun firstUnhandled(): GleaningRecovery?
}

data class PendingGleaning(
    val sessionId: Long,
    val textId: String,
    val startChar: Long,
    val endChar: Long,
    val sourceContext: String,
    val curatedContext: String?,
    val createdAtMillis: Long,
)

/**
 * Resolves a locally bundled editorial lens for a source slice.
 *
 * Matching is whitespace-insensitive: the corpus is hard-wrapped, so an anchor
 * that spans a line break would otherwise silently never match.
 */
fun interface GleaningContextSource {
    fun forSource(textId: String, sourceContext: String): String?
}

/** Collapses runs of whitespace so wrapped anchors match the source slice. */
fun canonicalProse(value: String): String =
    value.replace(WHITESPACE_RUN, " ").trim()

private val WHITESPACE_RUN = Regex("\\s+")

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

/**
 * Builds a source material and its hand-off in one place, so the coordinator
 * and the flow view model cannot drift on how a slice is derived or resolved.
 */
object GleaningDraftFactory {
    fun source(
        textId: String,
        startChar: Long,
        endChar: Long,
        text: String,
        contexts: GleaningContextSource,
    ): GleaningSourceMaterial? {
        val excerpt = GleaningSourceBuilder.build(text, startChar, endChar) ?: return null
        return GleaningSourceMaterial(
            startChar = excerpt.startChar,
            endChar = excerpt.endChar,
            sourceContext = excerpt.text,
            curatedContext = contexts.forSource(textId, excerpt.text),
        )
    }

    fun pending(
        sessionId: Long,
        textId: String,
        startChar: Long,
        endChar: Long,
        text: String,
        contexts: GleaningContextSource,
    ): PendingGleaningDraft? {
        val source = source(textId, startChar, endChar, text, contexts) ?: return null
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
    suspend fun isHandled(sessionId: Long): Boolean
    suspend fun save(draft: GleaningDraft): Gleaning
    suspend fun all(): List<Gleaning>
    suspend fun count(): Int
}

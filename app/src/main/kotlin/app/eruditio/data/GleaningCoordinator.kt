package app.eruditio.data

import app.eruditio.core.GleaningContextSource
import app.eruditio.core.GleaningDraftFactory
import app.eruditio.core.GleaningStore
import app.eruditio.core.PendingGleaningDraft
import app.eruditio.core.SessionController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Coordinates the durable completion hand-off behind data/store ports, not UI. */
class GleaningCoordinator(
    private val sessions: SessionController,
    private val library: TextLibrary,
    private val store: GleaningStore,
    private val contexts: GleaningContextSource,
) {
    suspend fun preparePending(sessionId: Long, endChar: Long): Boolean =
        try {
            val session = sessions.sessionFor(sessionId) ?: return false
            val text = withContext(Dispatchers.IO) { library.text(session.textId) }
            val material = GleaningDraftFactory.source(
                textId = session.textId,
                startChar = session.startChar,
                endChar = endChar,
                text = text,
            ) ?: return false
            val draft = PendingGleaningDraft(
                sessionId = session.sessionId,
                textId = session.textId,
                startChar = material.startChar,
                endChar = material.endChar,
                sourceContext = material.sourceContext,
                curatedContext = contexts.forSource(session.textId, material.sourceContext),
            )
            store.prepare(draft)
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // No source text is logged; the caller keeps the session open.
            false
        }
}

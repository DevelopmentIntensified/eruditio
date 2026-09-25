package app.eruditio.data

import app.eruditio.core.GleaningContextSource
import app.eruditio.core.GleaningDraftFactory
import app.eruditio.core.GleaningHandoff
import app.eruditio.core.GleaningStore
import app.eruditio.core.SessionController
import app.eruditio.core.SourceTextReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Coordinates the durable completion hand-off behind data/store ports, not UI. */
class GleaningCoordinator(
    private val sessions: SessionController,
    private val texts: SourceTextReader,
    private val store: GleaningStore,
    private val contexts: GleaningContextSource,
) {
    suspend fun preparePending(sessionId: Long, endChar: Long): GleaningHandoff {
        val session = sessions.sessionFor(sessionId) ?: return GleaningHandoff.UNAVAILABLE
        // Only a COMPLETED session that already has a row is genuinely resolved.
        // A row on a still-open session is a hand-off from an attempt that died
        // before completing, so it must be re-prepared rather than block the
        // reader from ever checking in again.
        if (session.isCompleted && store.isHandled(sessionId)) return GleaningHandoff.ALREADY_HANDLED
        return try {
            val text = withContext(Dispatchers.IO) { texts.text(session.textId) }
            val draft = GleaningDraftFactory.pending(
                sessionId = session.sessionId,
                textId = session.textId,
                startChar = session.startChar,
                endChar = endChar,
                text = text,
                contexts = contexts,
            ) ?: return GleaningHandoff.UNAVAILABLE
            store.prepare(draft)
            GleaningHandoff.PREPARED
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // No source text is logged; the caller keeps the session open.
            GleaningHandoff.UNAVAILABLE
        }
    }
}

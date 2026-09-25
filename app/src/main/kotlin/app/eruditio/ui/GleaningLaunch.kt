package app.eruditio.ui

import app.eruditio.AppContainer
import app.eruditio.core.GleaningDraftFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Persist the hand-off before navigation so process death cannot lose the prompt. */
internal suspend fun preparePendingGleaning(
    container: AppContainer,
    sessionId: Long,
    endChar: Long,
): Boolean =
    try {
        val session = container.sessions.sessionFor(sessionId) ?: return false
        val text = withContext(Dispatchers.IO) { container.library.text(session.textId) }
        val draft = GleaningDraftFactory.pending(
            sessionId = session.sessionId,
            textId = session.textId,
            startChar = session.startChar,
            endChar = endChar,
            text = text,
        ) ?: return false
        container.gleanings.prepare(draft)
        true
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        // The completed session can still open the flow; no source text is logged.
        false
    }

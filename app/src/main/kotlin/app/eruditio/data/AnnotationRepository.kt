package app.eruditio.data

import app.eruditio.core.Annotation
import app.eruditio.core.AnnotationDraft
import app.eruditio.core.AnnotationStore
import app.eruditio.core.Clock

/** Room adapter for the on-device reading marks (variant A, tickets 26→27). */
class RoomAnnotationStore(
    private val dao: AnnotationDao,
    private val clock: Clock,
) : AnnotationStore {

    override suspend fun keep(draft: AnnotationDraft): Annotation {
        val entity = draft.toEntity(clock.epochMillis)
        val id = dao.keep(entity)
        return entity.copy(id = id).toAnnotation()
    }

    override suspend fun rewrite(id: Long, noteText: String): Annotation? {
        if (noteText.isBlank()) {
            throw IllegalArgumentException("A note needs the reader's own words")
        }
        // The kind guard lives in the query; an unknown id or a non-note row
        // changed nothing, so report it as a miss rather than a stale value.
        if (dao.rewriteNote(id, noteText) == 0) return null
        return dao.get(id)?.toAnnotation()
    }

    override suspend fun forText(textId: String): List<Annotation> =
        dao.forText(textId).map { it.toAnnotation() }

    override suspend fun drop(id: Long) {
        dao.drop(id)
    }
}

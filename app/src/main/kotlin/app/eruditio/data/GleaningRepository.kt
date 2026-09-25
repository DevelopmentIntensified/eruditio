package app.eruditio.data

import app.eruditio.core.Clock
import app.eruditio.core.Gleaning
import app.eruditio.core.GleaningDraft
import app.eruditio.core.GleaningStore
import app.eruditio.core.PendingGleaning
import app.eruditio.core.PendingGleaningDraft

/** Room adapter for the local, source-linked gleaning collection. */
class RoomGleaningStore(
    private val dao: GleaningDao,
    private val clock: Clock,
) : GleaningStore {

    override suspend fun prepare(draft: PendingGleaningDraft): PendingGleaning {
        val entity = draft.toEntity(clock.epochMillis)
        dao.upsert(entity)
        return entity.toPending()
    }

    override suspend fun pending(sessionId: Long): PendingGleaning? =
        dao.pending(sessionId)?.toPending()

    override suspend fun latestPending(): PendingGleaning? = dao.latestPending()?.toPending()

    override suspend fun discard(sessionId: Long) {
        dao.discard(sessionId)
    }

    override suspend fun contains(sessionId: Long): Boolean = dao.contains(sessionId)

    override suspend fun save(draft: GleaningDraft): Gleaning {
        val entity = draft.toEntity(clock.epochMillis)
        dao.upsert(entity)
        return entity.toGleaning()
    }

    override suspend fun all(): List<Gleaning> = dao.all().map { it.toGleaning() }

    override suspend fun count(): Int = dao.count()
}

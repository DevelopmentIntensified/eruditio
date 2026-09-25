package app.eruditio.data

import app.eruditio.core.Clock
import app.eruditio.core.Gleaning
import app.eruditio.core.GleaningDraft
import app.eruditio.core.GleaningStore

/** Room adapter for the local, source-linked gleaning collection. */
class RoomGleaningStore(
    private val dao: GleaningDao,
    private val clock: Clock,
) : GleaningStore {

    override suspend fun save(draft: GleaningDraft): Gleaning {
        val entity = draft.toEntity(clock.epochMillis)
        dao.upsert(entity)
        return entity.toGleaning()
    }

    override suspend fun recent(limit: Int): List<Gleaning> =
        dao.recent(limit.coerceAtLeast(0)).map { it.toGleaning() }
}

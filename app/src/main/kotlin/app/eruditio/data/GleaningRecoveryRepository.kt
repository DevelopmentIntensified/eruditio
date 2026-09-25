package app.eruditio.data

import app.eruditio.core.GleaningRecovery
import app.eruditio.core.GleaningRecoverySource
import androidx.room.Dao
import androidx.room.Query

/**
 * One indexed join answers "which completed reading still owes the reader a
 * reflection?" — a row in `gleanings` of any status (pending, saved, or the
 * discarded tombstone) means handled, so an explicit discard is terminal.
 */
@Dao
interface GleaningRecoveryDao {

    @Query(
        "SELECT s.sessionId AS sessionId, s.textId AS textId, " +
            "s.startChar AS startChar, s.endChar AS endChar " +
            "FROM sessions s " +
            "LEFT JOIN gleanings g ON g.sessionId = s.sessionId " +
            "WHERE s.endChar IS NOT NULL AND g.sessionId IS NULL " +
            "ORDER BY s.completedAtMillis DESC LIMIT 1",
    )
    suspend fun firstUnhandled(): GleaningRecovery?
}

class RoomGleaningRecovery(private val dao: GleaningRecoveryDao) : GleaningRecoverySource {
    override suspend fun firstUnhandled(): GleaningRecovery? = dao.firstUnhandled()
}

package app.eruditio.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import app.eruditio.core.Entitlement
import app.eruditio.core.Gleaning
import app.eruditio.core.GleaningDraft
import app.eruditio.core.PendingGleaning
import app.eruditio.core.PendingGleaningDraft
import app.eruditio.core.LedgerEntry
import app.eruditio.core.InterceptionResult
import app.eruditio.core.SessionRecord
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "reading_positions")
data class ReadingPositionEntity(
    @PrimaryKey val textId: String,
    val charOffset: Long,
    val updatedAtMillis: Long,
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val sessionId: Long = 0,
    val textId: String,
    val startChar: Long,
    val endChar: Long? = null,
    val startedAtMillis: Long,
    val completedAtMillis: Long? = null,
    val completedEpochDay: Long? = null,
    val readMillis: Long = 0,
)

@Entity(tableName = "gleanings")
data class GleaningEntity(
    @PrimaryKey val sessionId: Long,
    val textId: String,
    val startChar: Long,
    val endChar: Long,
    @ColumnInfo(defaultValue = "saved") val status: String = "saved",
    /** Empty is allowed only while a durable hand-off is pending. */
    val takeaway: String,
    val sourceContext: String,
    @ColumnInfo(defaultValue = "") val curatedContext: String,
    val createdAtMillis: Long,
)

@Entity(tableName = "interceptions")
data class InterceptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageId: String,
    val result: String, // InterceptionResult name
    val epochDay: Long,
    val atMillis: Long,
)

@Entity(tableName = "ledger")
data class LedgerEntity(
    @PrimaryKey val conceptId: String,
    val repetitions: Int,
    val intervalDays: Int,
    val lastEpochDay: Long,
    val dueEpochDay: Long,
)

@Entity(tableName = "entitlement")
data class EntitlementEntity(
    @PrimaryKey val id: Long = 0,
    val owned: Boolean = false,
    val freeSessionsUsed: Int = 0,
)

fun LedgerEntity.toEntry() = LedgerEntry(conceptId, repetitions, intervalDays, lastEpochDay, dueEpochDay)

fun LedgerEntry.toEntity() = LedgerEntity(conceptId, repetitions, intervalDays, lastEpochDay, dueEpochDay)

fun GleaningEntity.toGleaning() = Gleaning(
    sessionId = sessionId,
    textId = textId,
    startChar = startChar,
    endChar = endChar,
    takeaway = takeaway,
    sourceContext = sourceContext,
    curatedContext = curatedContext.takeIf { it.isNotBlank() },
    createdAtMillis = createdAtMillis,
)

fun GleaningEntity.toPending() = PendingGleaning(
    sessionId = sessionId,
    textId = textId,
    startChar = startChar,
    endChar = endChar,
    sourceContext = sourceContext,
    curatedContext = curatedContext.takeIf { it.isNotBlank() },
    createdAtMillis = createdAtMillis,
)

fun GleaningDraft.toEntity(createdAtMillis: Long) = GleaningEntity(
    sessionId = sessionId,
    textId = textId,
    startChar = startChar,
    endChar = endChar,
    status = "saved",
    takeaway = takeaway,
    sourceContext = sourceContext,
    curatedContext = curatedContext.orEmpty(),
    createdAtMillis = createdAtMillis,
)

fun PendingGleaningDraft.toEntity(createdAtMillis: Long) = GleaningEntity(
    sessionId = sessionId,
    textId = textId,
    startChar = startChar,
    endChar = endChar,
    status = "pending",
    takeaway = "",
    sourceContext = sourceContext,
    curatedContext = curatedContext.orEmpty(),
    createdAtMillis = createdAtMillis,
)

fun EntitlementEntity.toEntitlement() = Entitlement(owned, freeSessionsUsed)

@Dao
interface ReadingPositionDao {
    @Query("SELECT * FROM reading_positions WHERE textId = :textId")
    suspend fun get(textId: String): ReadingPositionEntity?

    @Query("SELECT * FROM reading_positions ORDER BY updatedAtMillis DESC LIMIT 1")
    suspend fun latest(): ReadingPositionEntity?

    @Upsert
    suspend fun upsert(position: ReadingPositionEntity)
}

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Query("SELECT * FROM sessions WHERE sessionId = :id")
    suspend fun get(id: Long): SessionEntity?

    @Query("SELECT * FROM sessions WHERE endChar IS NOT NULL ORDER BY startedAtMillis DESC LIMIT :limit")
    suspend fun recentCompleted(limit: Int): List<SessionEntity>

    @Query(
        "UPDATE sessions SET endChar = :endChar, completedAtMillis = :atMillis, completedEpochDay = :epochDay " +
            "WHERE sessionId = :id AND endChar IS NULL",
    )
    suspend fun complete(id: Long, endChar: Long, atMillis: Long, epochDay: Long): Int

    @Query("SELECT DISTINCT completedEpochDay FROM sessions WHERE completedEpochDay IS NOT NULL")
    suspend fun completedEpochDays(): List<Long>

    @Query("SELECT COUNT(*) FROM sessions WHERE completedEpochDay >= :fromEpochDay")
    suspend fun countCompletedSince(fromEpochDay: Long): Int

    @Query("UPDATE sessions SET readMillis = readMillis + :delta WHERE sessionId = :id")
    suspend fun addReadingTime(id: Long, delta: Long)

    @Query("SELECT COALESCE(SUM(readMillis), 0) FROM sessions")
    suspend fun totalReadMillis(): Long

    @Query("SELECT COALESCE(SUM(readMillis), 0) FROM sessions WHERE startedAtMillis >= :sinceMillis")
    suspend fun readMillisSince(sinceMillis: Long): Long
}

@Dao
interface GleaningDao {
    @Upsert
    suspend fun upsert(gleaning: GleaningEntity)

    @Query("SELECT * FROM gleanings WHERE status = 'saved' ORDER BY createdAtMillis DESC")
    suspend fun all(): List<GleaningEntity>

    @Query("SELECT COUNT(*) FROM gleanings WHERE status = 'saved'")
    suspend fun count(): Int

    @Query("SELECT * FROM gleanings WHERE sessionId = :sessionId AND status = 'pending'")
    suspend fun pending(sessionId: Long): GleaningEntity?

    @Query("SELECT * FROM gleanings WHERE status = 'pending' ORDER BY createdAtMillis DESC LIMIT 1")
    suspend fun latestPending(): GleaningEntity?

    /**
     * A row in any status — including the `discarded` tombstone — means the
     * reader has already been offered this session. The tombstone is what makes
     * an explicit discard terminal rather than resurrectable.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM gleanings WHERE sessionId = :sessionId)")
    suspend fun isHandled(sessionId: Long): Boolean

    @Query("UPDATE gleanings SET status = 'discarded' WHERE sessionId = :sessionId AND status = 'pending'")
    suspend fun discard(sessionId: Long)
}

@Dao
interface InterceptionDao {
    @Insert
    suspend fun insert(record: InterceptionEntity)

    @Query(
        "SELECT COUNT(*) FROM interceptions " +
            "WHERE epochDay BETWEEN :fromEpochDay AND :todayEpochDay",
    )
    suspend fun weeklyCount(fromEpochDay: Long, todayEpochDay: Long): Int

    @Query(
        "SELECT COUNT(*) FROM interceptions " +
            "WHERE epochDay BETWEEN :fromEpochDay AND :todayEpochDay AND result = :result",
    )
    suspend fun weeklyCountByResult(fromEpochDay: Long, todayEpochDay: Long, result: String): Int
}

@Dao
interface LedgerDao {
    @Query("SELECT * FROM ledger")
    suspend fun all(): List<LedgerEntity>

    @Query("SELECT conceptId FROM ledger")
    suspend fun conceptIds(): List<String>

    @Query("SELECT * FROM ledger WHERE conceptId = :conceptId")
    suspend fun get(conceptId: String): LedgerEntity?

    @Upsert
    suspend fun upsert(entry: LedgerEntity)
}

@Dao
interface EntitlementDao {
    @Query("SELECT * FROM entitlement WHERE id = 0")
    suspend fun get(): EntitlementEntity?

    @Query("SELECT * FROM entitlement WHERE id = 0")
    fun observe(): Flow<EntitlementEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entitlement: EntitlementEntity)
}

fun InterceptionResult.toDbString() = name

fun String.toInterceptionResult(): InterceptionResult = InterceptionResult.valueOf(this)

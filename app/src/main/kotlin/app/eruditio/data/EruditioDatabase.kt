package app.eruditio.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import app.eruditio.core.Entitlement
import app.eruditio.core.LedgerEntry
import app.eruditio.core.InterceptionResult
import app.eruditio.core.SessionRecord
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "reading_positions")
data class ReadingPositionEntity(
    @PrimaryKey val textId: String,
    val charOffset: Long,
    val paragraphIndex: Int,
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
    suspend fun complete(id: Long, endChar: Long, atMillis: Long, epochDay: Long)

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

package app.eruditio.data

import app.eruditio.core.Clock
import app.eruditio.core.Entitlement
import app.eruditio.core.EntitlementStore
import app.eruditio.core.InterceptionLog
import app.eruditio.core.InterceptionResult
import app.eruditio.core.LedgerEntry
import app.eruditio.core.LedgerStore
import app.eruditio.core.ConceptSource
import app.eruditio.core.SessionLog
import app.eruditio.core.SessionRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

class DeviceClock(private val zone: ZoneId = ZoneId.systemDefault()) : Clock {
    override val epochMillis: Long get() = System.currentTimeMillis()
    override val epochDay: Long
        get() = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate().toEpochDay()

    /** Local start-of-today, for "minutes today" style stats. */
    fun startOfDayMillis(): Long =
        java.time.LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
}

class RoomLedgerStore(private val dao: LedgerDao) : LedgerStore {
    override suspend fun all(): List<LedgerEntry> = dao.all().map { it.toEntry() }
    override suspend fun get(conceptId: String): LedgerEntry? = dao.get(conceptId)?.toEntry()
    override suspend fun upsert(entry: LedgerEntry) = dao.upsert(entry.toEntity())
    override suspend fun conceptIds(): Set<String> = dao.conceptIds().toSet()
}

class RoomInterceptionLog(private val dao: InterceptionDao) : InterceptionLog {
    override suspend fun record(packageId: String, result: InterceptionResult, epochDay: Long) {
        dao.insert(InterceptionEntity(packageId = packageId, result = result.name, epochDay = epochDay, atMillis = System.currentTimeMillis()))
    }

    override suspend fun weeklyCount(todayEpochDay: Long): Int = dao.weeklyCount(todayEpochDay - 6, todayEpochDay)

    override suspend fun weeklyReplaced(todayEpochDay: Long): Int =
        dao.weeklyCountByResult(todayEpochDay - 6, todayEpochDay, InterceptionResult.REPLACED.name)
}

class RoomSessionLog(private val dao: SessionDao) : SessionLog {
    override suspend fun start(textId: String, startChar: Long, epochDay: Long, atMillis: Long): Long =
        dao.insert(SessionEntity(textId = textId, startChar = startChar, startedAtMillis = atMillis))

    override suspend fun get(sessionId: Long): SessionRecord? =
        dao.get(sessionId)?.let { s ->
            SessionRecord(s.sessionId, s.textId, s.startChar, s.endChar, s.startedAtMillis, s.completedAtMillis, s.completedEpochDay)
        }

    override suspend fun recent(limit: Int): List<SessionRecord> =
        dao.recentCompleted(limit).map { s ->
            SessionRecord(s.sessionId, s.textId, s.startChar, s.endChar, s.startedAtMillis, s.completedAtMillis, s.completedEpochDay)
        }

    override suspend fun completedEpochDays(): Set<Long> = dao.completedEpochDays().toSet()

    override suspend fun countCompletedSince(fromEpochDay: Long): Int = dao.countCompletedSince(fromEpochDay)

    override suspend fun addReadingTime(sessionId: Long, deltaMillis: Long) {
        if (deltaMillis > 0) dao.addReadingTime(sessionId, deltaMillis)
    }

    override suspend fun totalReadMillis(): Long = dao.totalReadMillis()

    override suspend fun readMillisSince(sinceMillis: Long): Long = dao.readMillisSince(sinceMillis)

    override suspend fun complete(sessionId: Long, endChar: Long, atMillis: Long, epochDay: Long) =
        dao.complete(sessionId, endChar, atMillis, epochDay)
}

/**
 * Segment-derived review concepts: the text is cut into fixed windows and every
 * window overlapping a completed session's range yields one concept whose recall
 * cue is the window's opening excerpt. Deterministic and content-agnostic.
 */
class SegmentConcepts(
    private val library: TextLibrary,
    private val windowChars: Int = 2000,
    private val minContentChars: Int = 40,
    private val cueChars: Int = 180,
) : ConceptSource {

    override suspend fun conceptsFor(textId: String, startChar: Long, endChar: Long): List<String> =
        windows(textId)
            .filter { it.startChar < endChar && it.endChar > startChar }
            .map { conceptId(textId, it.index) }

    override suspend fun cueFor(conceptId: String): String? {
        val textId = conceptId.substringBefore('#')
        return windows(textId)
            .firstOrNull { conceptId(textId, it.index) == conceptId }
            ?.let { excerpt(it) }
    }

    private fun excerpt(w: Window): String {
        val raw = library.text(w.textId).substring(w.startChar.toInt(), w.endChar.toInt())
        val compact = raw.replace(Regex("\\s+"), " ").trim()
        return if (compact.length <= cueChars) compact else compact.take(cueChars).trimEnd() + "…"
    }

    private fun windows(textId: String): List<Window> = cache.getOrPut(textId) {
        val text = library.text(textId)
        val total = text.length.toLong()
        val out = mutableListOf<Window>()
        var start = 0L
        var index = 0
        while (start < total) {
            val end = minOf(start + windowChars, total)
            val content = text.substring(start.toInt(), end.toInt())
            if (content.replace(Regex("\\s+"), "").length >= minContentChars) {
                out += Window(textId, index, start, end)
                index++
            }
            start = end
        }
        out
    }

    private val cache = mutableMapOf<String, List<Window>>()
    private val conceptPrefix = "c"

    private fun conceptId(textId: String, index: Int) = "$textId#$conceptPrefix$index"

    data class Window(val textId: String, val index: Int, val startChar: Long, val endChar: Long)
}

class EntitlementRepository(private val dao: EntitlementDao) : EntitlementStore {
    override suspend fun entitlement(): Entitlement =
        dao.get()?.toEntitlement() ?: Entitlement()

    override suspend fun save(entitlement: Entitlement) =
        dao.upsert(EntitlementEntity(owned = entitlement.owned, freeSessionsUsed = entitlement.freeSessionsUsed))

    /** Reactive entitlement for the UI; purchase completion flips the gate live. */
    fun observe(): Flow<Entitlement> = dao.observe().map { it?.toEntitlement() ?: Entitlement() }
}

class ReadingPositions(private val dao: ReadingPositionDao) {
    suspend fun restore(textId: String): ReadingPositionEntity? = dao.get(textId)
    suspend fun lastReadTextId(): String? = dao.latest()?.textId
    suspend fun save(textId: String, charOffset: Long, paragraphIndex: Int) =
        dao.upsert(ReadingPositionEntity(textId, charOffset, paragraphIndex, System.currentTimeMillis()))
}

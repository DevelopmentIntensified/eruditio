package app.eruditio

import app.eruditio.core.Clock
import app.eruditio.core.Entitlement
import app.eruditio.core.EntitlementStore
import app.eruditio.core.InterceptionLog
import app.eruditio.core.InterceptionResult
import app.eruditio.core.LedgerEntry
import app.eruditio.core.LedgerStore
import app.eruditio.core.ConceptSource
import app.eruditio.core.PositionStore
import app.eruditio.core.SessionLog
import app.eruditio.core.SessionRecord
import app.eruditio.core.TrailingWeek
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneId

/* ---- fakes for the seam stores (no Android) ---- */

class TestClock(private var day: Long) : Clock {
    override val epochDay: Long get() = day
    override val epochMillis: Long get() = day * 86_400_000L
    override fun startOfDayMillis(daysAgo: Int): Long = (day - daysAgo) * 86_400_000L
    fun advance(days: Long) { day += days }
}

/** Fixed-instant clock in a fixed zone: pins local-day bucketing against UTC drift. */
class ZoneClock(var instantMillis: Long, private val zone: ZoneId) : Clock {
    override val epochMillis: Long get() = instantMillis
    override val epochDay: Long
        get() = Instant.ofEpochMilli(instantMillis).atZone(zone).toLocalDate().toEpochDay()
    override fun startOfDayMillis(daysAgo: Int): Long =
        Instant.ofEpochMilli(instantMillis).atZone(zone).toLocalDate()
            .minusDays(daysAgo.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()
}

class FakeLedgerStore : LedgerStore {
    private val mutex = Mutex()
    private val entries = mutableMapOf<String, LedgerEntry>()

    override suspend fun all(): List<LedgerEntry> = mutex.withLock { entries.values.toList() }
    override suspend fun get(conceptId: String): LedgerEntry? = mutex.withLock { entries[conceptId] }
    override suspend fun upsert(entry: LedgerEntry) = mutex.withLock { entries[entry.conceptId] = entry }
    override suspend fun conceptIds(): Set<String> = mutex.withLock { entries.keys.toSet() }
}

class FakeInterceptionLog : InterceptionLog {
    data class Record(val packageId: String, val result: InterceptionResult, val epochDay: Long)

    private val records = mutableListOf<Record>()

    override suspend fun record(packageId: String, result: InterceptionResult, epochDay: Long) {
        records += Record(packageId, result, epochDay)
    }

    override suspend fun weeklyCount(week: TrailingWeek): Int =
        records.count { it.epochDay in week.dayRange }

    override suspend fun weeklyReplaced(week: TrailingWeek): Int =
        records.count {
            it.epochDay in week.dayRange && it.result == InterceptionResult.REPLACED
        }

    val all: List<Record> get() = records.toList()
}

class FakeSessionLog : SessionLog {
    private val sessions = mutableMapOf<Long, SessionRecord>()
    private val readTimes = mutableMapOf<Long, Long>()

    override suspend fun start(textId: String, startChar: Long, epochDay: Long, atMillis: Long): Long {
        val id = sessions.size.toLong() + 1
        sessions[id] = SessionRecord(id, textId, startChar, null, atMillis, null, null)
        return id
    }

    override suspend fun get(sessionId: Long): SessionRecord? = sessions[sessionId]

    override suspend fun complete(sessionId: Long, endChar: Long, atMillis: Long, epochDay: Long) {
        sessions[sessionId]?.let {
            sessions[sessionId] = it.copy(endChar = endChar, completedAtMillis = atMillis, completedEpochDay = epochDay)
        }
    }

    override suspend fun recent(limit: Int): List<SessionRecord> =
        sessions.values.filter { it.endChar != null }.sortedByDescending { it.startedAtMillis }.take(limit)

    override suspend fun completedEpochDays(): Set<Long> =
        sessions.values.mapNotNull { it.completedEpochDay }.toSet()

    override suspend fun countCompletedSince(fromEpochDay: Long): Int =
        sessions.values.count { (it.completedEpochDay ?: Long.MIN_VALUE) >= fromEpochDay }

    override suspend fun addReadingTime(sessionId: Long, deltaMillis: Long) {
        if (deltaMillis > 0) readTimes.merge(sessionId, deltaMillis, Long::plus)
    }

    override suspend fun totalReadMillis(): Long = readTimes.values.sum()

    override suspend fun readMillisSince(sinceMillis: Long): Long =
        sessions.values.filter { it.startedAtMillis >= sinceMillis }.sumOf { readTimes[it.sessionId] ?: 0L }
}

class FakeConceptSource : ConceptSource {
    override suspend fun conceptsFor(textId: String, startChar: Long, endChar: Long): List<String> =
        if (endChar > startChar) listOf("$textId#c0") else emptyList()

    override suspend fun cueFor(conceptId: String): String? = "cue: $conceptId"
}

class FakeEntitlementStore : EntitlementStore {
    var value = Entitlement()
    override suspend fun entitlement(): Entitlement = value
    override suspend fun save(entitlement: Entitlement) { value = entitlement }
}

class FakePositionStore(
    private val offsets: Map<String, Long> = emptyMap(),
    private val lastTextId: String? = null,
) : PositionStore {
    override suspend fun restoreOffset(textId: String): Long? = offsets[textId]
    override suspend fun lastReadTextId(): String? = lastTextId
}

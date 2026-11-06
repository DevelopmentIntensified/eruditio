package app.eruditio

import app.eruditio.core.Clock
import app.eruditio.core.Annotation
import app.eruditio.core.AnnotationKind
import app.eruditio.core.AnnotationStore
import app.eruditio.core.AnnotationDraft
import app.eruditio.core.Entitlement
import app.eruditio.core.EntitlementStore
import app.eruditio.core.InterceptionLog
import app.eruditio.core.InterceptionResult
import app.eruditio.core.LedgerEntry
import app.eruditio.core.LedgerStore
import app.eruditio.core.ConceptSource
import app.eruditio.core.Gleaning
import app.eruditio.core.GleaningDraft
import app.eruditio.core.GleaningStore
import app.eruditio.core.PendingGleaning
import app.eruditio.core.PendingGleaningDraft
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

    override suspend fun complete(sessionId: Long, endChar: Long, atMillis: Long, epochDay: Long): Boolean {
        val session = sessions[sessionId] ?: return false
        if (session.isCompleted) return false
        sessions[sessionId] = session.copy(
            endChar = endChar,
            completedAtMillis = atMillis,
            completedEpochDay = epochDay,
        )
        return true
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

/**
 * Mirrors the Room adapter's keep/rewrite/forText/drop contract for reader
 * marks (variant A) so the store interface can be tested without Android.
 */
class FakeAnnotationStore : AnnotationStore {
    private val rows = LinkedHashMap<Long, Annotation>()
    private var nextId = 1L

    override suspend fun keep(draft: AnnotationDraft): Annotation {
        val stored = Annotation(
            id = nextId++,
            textId = draft.textId,
            kind = draft.kind,
            startChar = draft.startChar,
            endChar = draft.endChar,
            noteText = draft.noteText,
        )
        rows[stored.id] = stored
        return stored
    }

    override suspend fun rewrite(id: Long, noteText: String): Annotation? {
        if (noteText.isBlank()) {
            throw IllegalArgumentException("A note needs the reader's own words")
        }
        val row = rows[id] ?: return null
        if (row.kind != AnnotationKind.NOTE) return null
        val updated = row.copy(noteText = noteText)
        rows[id] = updated
        return updated
    }

    override suspend fun forText(textId: String): List<Annotation> =
        rows.values.filter { it.textId == textId }.sortedBy { it.startChar }

    override suspend fun drop(id: Long) {
        rows.remove(id)
    }
}

/**
 * Mirrors the Room adapter's explicit pending/saved/discarded contract so the
 * store interface and the recovery flow can be tested without Android.
 */
class FakeGleaningStore : GleaningStore {
    private enum class Status { PENDING, SAVED, DISCARDED }

    private data class Row(val saved: Gleaning?, val pending: PendingGleaning?, val status: Status)

    private val rows = mutableMapOf<Long, Row>()
    private val completed = mutableListOf<Long>()
    private var clock = 1_000L

    fun advanceClock(by: Long) { clock += by }

    fun complete(sessionId: Long) { completed.removeAll { it == sessionId }; completed.add(sessionId) }

    fun isCompleted(sessionId: Long): Boolean = sessionId in completed

    override suspend fun prepare(draft: PendingGleaningDraft): PendingGleaning {
        val pending = PendingGleaning(
            sessionId = draft.sessionId,
            textId = draft.textId,
            startChar = draft.startChar,
            endChar = draft.endChar,
            sourceContext = draft.sourceContext,
            curatedContext = draft.curatedContext,
            createdAtMillis = clock++,
        )
        rows[draft.sessionId] = Row(null, pending, Status.PENDING)
        return pending
    }

    override suspend fun pending(sessionId: Long): PendingGleaning? =
        rows[sessionId]?.takeIf { it.status == Status.PENDING }?.pending

    override suspend fun latestPending(): PendingGleaning? =
        rows.values.filter { it.status == Status.PENDING }.maxByOrNull { it.pending!!.createdAtMillis }?.pending

    override suspend fun discard(sessionId: Long) {
        val row = rows[sessionId] ?: return
        if (row.status == Status.PENDING) rows[sessionId] = row.copy(status = Status.DISCARDED)
    }

    override suspend fun isHandled(sessionId: Long): Boolean = rows.containsKey(sessionId)

    override suspend fun save(draft: GleaningDraft): Gleaning {
        val saved = Gleaning(
            sessionId = draft.sessionId,
            textId = draft.textId,
            startChar = draft.startChar,
            endChar = draft.endChar,
            takeaway = draft.takeaway,
            sourceContext = draft.sourceContext,
            curatedContext = draft.curatedContext,
            createdAtMillis = clock++,
        )
        rows[draft.sessionId] = Row(saved, null, Status.SAVED)
        return saved
    }

    override suspend fun all(): List<Gleaning> =
        rows.values.filter { it.status == Status.SAVED }.map { it.saved!! }

    override suspend fun count(): Int = rows.values.count { it.status == Status.SAVED }
}

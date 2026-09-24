package app.eruditio.core

/**
 * The buy-once gate (billing ticket): a fresh install can evaluate the app
 * before purchase; after [FREE_SESSION_LIMIT] completed sessions the core loop
 * is gated until the one-time purchase. Debug builds bypass the gate entirely.
 * Trial state is part of entitlement data, never of mastery.
 */
interface EntitlementStore {
    suspend fun entitlement(): Entitlement
    suspend fun save(entitlement: Entitlement)
}

data class Entitlement(
    val owned: Boolean = false,
    val freeSessionsUsed: Int = 0,
)

class TrialGate(private val store: EntitlementStore, private val isDebug: Boolean) {
    suspend fun unlocked(): Boolean = isDebug || isUnlocked(store.entitlement())

    suspend fun markSessionCompleted() {
        if (isDebug) return
        val e = store.entitlement()
        if (!e.owned && e.freeSessionsUsed < FREE_SESSION_LIMIT) {
            store.save(e.copy(freeSessionsUsed = e.freeSessionsUsed + 1))
        }
    }

    suspend fun markOwned() {
        if (isDebug) return
        store.save(store.entitlement().copy(owned = true))
    }

    companion object {
        const val FREE_SESSION_LIMIT = 3

        /** The one unlock rule; TrialGate and the UI flow both read this. */
        fun isUnlocked(e: Entitlement) = e.owned || e.freeSessionsUsed < FREE_SESSION_LIMIT
    }
}

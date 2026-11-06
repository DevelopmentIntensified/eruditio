package app.eruditio.data

import android.app.Activity
import android.content.Context
import app.eruditio.core.TrialGate

/**
 * F-Droid build: the core loop is unlocked, so billing is a no-op.
 *
 * F-Droid's Inclusion Policy forbids the proprietary Play Billing client,
 * which lives in the `play` flavor only (src/play/.../PlayBillingGate.kt).
 * Same class name and API as the Play version so AppContainer and
 * EruditioViewModel compile for both flavors unchanged. TrialGate treats
 * `foss` like debug (unlocked), so [launchPurchase] is unreachable in
 * practice — it only reports that purchases are unavailable here.
 */
class PlayBillingGate(
    context: Context,
    private val trial: TrialGate,
) {
    fun connect() {
        // Nothing to connect: F-Droid build needs no store connection.
    }

    /** No-op: the F-Droid build has no store to query. */
    fun restore() {
        // The core loop is already unlocked in this build.
    }

    /** Unreachable when the gate is unlocked; reports instead of charging. */
    suspend fun launchPurchase(activity: Activity, onError: (String) -> Unit): Boolean {
        onError("Purchases are unavailable in the F-Droid build — the core loop is unlocked")
        return false
    }
}

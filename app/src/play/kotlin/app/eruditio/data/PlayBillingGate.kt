package app.eruditio.data

import android.app.Activity
import android.content.Context
import app.eruditio.core.TrialGate
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/** The one OK check; every BillingResult branch reads this. */
private fun BillingResult.isOk(): Boolean =
    responseCode == BillingClient.BillingResponseCode.OK

/**
 * Play Billing, one-time product (billing ticket): a fresh install evaluates
 * the app under the trial gate; the one-time purchase unlocks the core loop
 * and survives reinstalls via Play's own licensing. No subscriptions, no ads,
 * no third-party billing SDKs. Debug builds bypass the gate at the seam
 * (see TrialGate), so development stays friction-free.
 */
class PlayBillingGate(
    context: Context,
    private val trial: TrialGate,
) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(Dispatchers.IO)

    // Billing 8.x requires explicitly supporting pending purchases for one-time
    // products; plain build() throws IllegalArgumentException (verified on device).
    // Lazy so debug builds — which bypass the gate — never initialize billing.
    private val client: BillingClient by lazy {
        BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build(),
            )
            .build()
    }

    /** Product id for the one-time core unlock (finalized at publish per spec). */
    private val coreProductId = "eruditio_core_unlock"

    fun connect() {
        if (client.isReady) return
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.isOk()) {
                    scope.launch { restore() }
                }
            }

            override fun onBillingServiceDisconnected() { /* reconnect on next launch */ }
        })
    }

    /** Purchase flow for the one-time unlock; result lands in [onPurchasesUpdated]. */
    suspend fun launchPurchase(activity: Activity, onError: (String) -> Unit): Boolean =
        withContext(Dispatchers.IO) {
            if (!client.isReady) {
                onError("Billing not ready")
                return@withContext false
            }
            val details = suspendCancellableCoroutine { continuation ->
                fetchProductDetails { continuation.resume(it) }
            }
            if (details == null) {
                onError("Product not available")
                return@withContext false
            }
            val flow = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(details)
                            .build(),
                    ),
                )
                .build()
            client.launchBillingFlow(activity, flow).isOk()
        }

    /** Play licensing check: owned state survives reinstalls via past purchases. */
    suspend fun restore() {
        if (!client.isReady) return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.isOk()) {
                scope.launch { handle(purchases) }
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (!result.isOk()) return
        scope.launch { handle(purchases.orEmpty()) }
    }

    private suspend fun handle(purchases: List<Purchase>) {
        for (purchase in purchases) {
            if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) continue
            if (coreProductId !in purchase.products) continue
            if (!purchase.isAcknowledged) {
                client.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build(),
                ) { /* acknowledged on Play's side */ }
            }
            trial.markOwned()
        }
    }

    private fun fetchProductDetails(onLoaded: (ProductDetails?) -> Unit) {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(coreProductId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                ),
            )
            .build()
        client.queryProductDetailsAsync(params) { result, queryResult ->
            if (!result.isOk()) {
                onLoaded(null)
            } else {
                onLoaded(queryResult.productDetailsList.firstOrNull { it.productId == coreProductId })
            }
        }
    }
}

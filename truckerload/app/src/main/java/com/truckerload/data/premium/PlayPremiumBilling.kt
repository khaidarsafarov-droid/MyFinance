package com.truckerload.data.premium

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.truckerload.domain.premium.PremiumPolicy
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Google Play subscription. The free month is an offer on [PremiumPolicy.PLAY_PRODUCT_ID]
 * in Play Console. This client only launches that offer and reads the resulting purchase.
 */
class PlayPremiumBilling(
    context: Context,
    private val store: PremiumStore,
    private val onPurchasesChanged: () -> Unit,
) {
    private val app = context.applicationContext
    private val connectLock = Mutex()
    private val client: BillingClient = BillingClient.newBuilder(app)
        .setListener { result, purchases ->
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    applyPurchases(purchases.orEmpty())
                    onPurchasesChanged()
                }
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> onPurchasesChanged()
            }
        }
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
        )
        .build()

    suspend fun hasActiveSubscription(): Boolean {
        if (!connect()) return store.isSubscribed()
        val purchases = querySubs() ?: return store.isSubscribed()
        return applyPurchases(purchases)
    }

    suspend fun formattedPrice(): String? {
        if (!connect()) return null
        val details = queryProduct() ?: return null
        val phases = playOffer(details)?.pricingPhases?.pricingPhaseList.orEmpty()
        return phases.lastOrNull { it.priceAmountMicros > 0L }?.formattedPrice
            ?: phases.lastOrNull()?.formattedPrice
    }

    suspend fun launchPurchase(activity: Activity): String? {
        if (!connect()) return "billing_unavailable"
        val details = queryProduct() ?: return "product_missing"
        val offer = playOffer(details)?.offerToken ?: return "offer_missing"
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offer)
                        .build(),
                ),
            )
            .build()
        val result = withContext(Dispatchers.Main) {
            client.launchBillingFlow(activity, params)
        }
        return when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> null
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> "already_owned"
            else -> result.debugMessage.ifBlank { "billing_${result.responseCode}" }
        }
    }

    private fun applyPurchases(purchases: List<Purchase>): Boolean {
        val active = purchases.any { purchase ->
            purchase.products.contains(PremiumPolicy.PLAY_PRODUCT_ID) &&
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        purchases.filter {
            it.products.contains(PremiumPolicy.PLAY_PRODUCT_ID) &&
                it.purchaseState == Purchase.PurchaseState.PURCHASED &&
                !it.isAcknowledged
        }.forEach { purchase ->
            client.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build(),
            ) { }
        }
        store.setSubscribed(active)
        return active
    }

    private suspend fun querySubs(): List<Purchase>? = suspendCancellableCoroutine { cont ->
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                cont.resume(purchases)
            } else {
                cont.resume(null)
            }
        }
    }

    /** Prefer the Play Console offer that includes a free month, then any other offer. */
    private fun playOffer(details: ProductDetails) =
        details.subscriptionOfferDetails.orEmpty().let { offers ->
            offers.firstOrNull { offer ->
                offer.pricingPhases.pricingPhaseList.any { phase ->
                    phase.priceAmountMicros == 0L && phase.billingCycleCount >= 1
                }
            } ?: offers.firstOrNull()
        }

    private suspend fun queryProduct(): ProductDetails? = suspendCancellableCoroutine { cont ->
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PremiumPolicy.PLAY_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        client.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build(),
        ) { result, details ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                cont.resume(details.firstOrNull())
            } else {
                cont.resume(null)
            }
        }
    }

    private suspend fun connect(): Boolean {
        if (client.isReady) return true
        return connectLock.withLock {
            if (client.isReady) return true
            suspendCancellableCoroutine { cont ->
                client.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(result: com.android.billingclient.api.BillingResult) {
                        if (cont.isActive) {
                            cont.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                        }
                    }

                    override fun onBillingServiceDisconnected() = Unit
                })
            }
        }
    }
}

package com.truckerload.data.premium

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.truckerload.data.preferences.SecurePreferences
import com.truckerload.domain.premium.PremiumPolicy
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Единственная точка работы с Google Play Billing.
 *
 * Бесплатный месяц задаётся оффером подписки [PremiumPolicy.PLAY_PRODUCT_ID]
 * в Play Console. Пока триал или оплаченный период активны, Play отдаёт покупку
 * в состоянии PURCHASED — отдельный таймер в приложении не нужен.
 * Бэкенда нет: покупку подтверждаем на устройстве и кэшируем статус локально.
 */
class BillingManager(context: Context) {

    private val app = context.applicationContext
    private val cache = SubscriptionCache(app)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connectLock = Mutex()

    private val _isSubscribed = MutableStateFlow(cache.subscribed)
    /** true — активная подписка или бесплатный месяц Play; false — нет или истекла. */
    val isSubscribed: StateFlow<Boolean> = _isSubscribed.asStateFlow()

    private var reconnectAttempt = 0

    private val client: BillingClient = BillingClient.newBuilder(app)
        .setListener { result, purchases -> onPurchasesUpdated(result, purchases) }
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
        )
        .enableAutoServiceReconnection()
        .build()

    /** Подключиться к Play и сразу перечитать подписки. */
    fun start() {
        scope.launch { refresh() }
    }

    /**
     * Онлайн-проверка. Если Play недоступен (нет сети, сервисов), остаётся кэш
     * последней успешной проверки.
     */
    suspend fun refresh() {
        if (!connect()) {
            _isSubscribed.value = cache.subscribed
            return
        }
        val purchases = queryPurchases()
        if (purchases == null) {
            _isSubscribed.value = cache.subscribed
            return
        }
        val active = applyPurchases(purchases)
        cache.save(active)
        _isSubscribed.value = active
        reconnectAttempt = 0
    }

    /** Цена платного периода из оффера Play, чтобы кнопка не врала сумму. */
    suspend fun formattedPrice(): String? {
        if (!connect()) return null
        val phases = playOffer(queryProduct() ?: return null)
            ?.pricingPhases
            ?.pricingPhaseList
            .orEmpty()
        return phases.lastOrNull { it.priceAmountMicros > 0L }?.formattedPrice
    }

    /** Открывает платёжный лист Google Play. Вызывать с Activity. */
    suspend fun launchBillingFlow(activity: Activity): String? {
        if (!connect()) return "billing_unavailable"
        val details = queryProduct() ?: return "product_missing"
        val offerToken = playOffer(details)?.offerToken ?: return "offer_missing"
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offerToken)
                        .build(),
                ),
            )
            .build()
        val result = withContext(Dispatchers.Main) {
            client.launchBillingFlow(activity, params)
        }
        return when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> null
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                refresh()
                "already_owned"
            }
            else -> result.debugMessage.ifBlank { "billing_${result.responseCode}" }
        }
    }

    private fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val active = applyPurchases(purchases.orEmpty())
                cache.save(active)
                _isSubscribed.value = active
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> start()
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
        }
    }

    /** PURCHASED по нашему product id. Триал Play тоже приходит как PURCHASED. */
    private fun applyPurchases(purchases: List<Purchase>): Boolean {
        val ours = purchases.filter { it.products.contains(PremiumPolicy.PLAY_PRODUCT_ID) }
        ours.filter { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged
        }.forEach { purchase ->
            client.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build(),
            ) { }
        }
        return ours.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
    }

    private suspend fun queryPurchases(): List<Purchase>? = suspendCancellableCoroutine { cont ->
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
        ) { result, purchases ->
            if (!cont.isActive) return@queryPurchasesAsync
            cont.resume(
                if (result.responseCode == BillingClient.BillingResponseCode.OK) purchases else null,
            )
        }
    }

    private suspend fun queryProduct(): ProductDetails? = suspendCancellableCoroutine { cont ->
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PremiumPolicy.PLAY_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        client.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build(),
        ) { result, details ->
            if (!cont.isActive) return@queryProductDetailsAsync
            cont.resume(
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    details.productDetailsList.firstOrNull()
                } else {
                    null
                },
            )
        }
    }

    /** Сначала оффер с бесплатной фазой (триал из Play Console), иначе базовый. */
    private fun playOffer(details: ProductDetails) =
        details.subscriptionOfferDetails.orEmpty().let { offers ->
            offers.firstOrNull { offer ->
                offer.pricingPhases.pricingPhaseList.any { phase ->
                    phase.priceAmountMicros == 0L && phase.billingCycleCount >= 1
                }
            } ?: offers.firstOrNull()
        }

    private suspend fun connect(): Boolean {
        if (client.isReady) return true
        return connectLock.withLock {
            if (client.isReady) return true
            suspendCancellableCoroutine { cont ->
                client.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(result: BillingResult) {
                        if (cont.isActive) {
                            cont.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                        }
                    }

                    override fun onBillingServiceDisconnected() {
                        // Play оборвал сервис. Не крутим бесконечно: несколько попыток и кэш.
                        if (reconnectAttempt >= MAX_RECONNECT) return
                        reconnectAttempt += 1
                        scope.launch {
                            delay(RECONNECT_DELAY_MS * reconnectAttempt)
                            if (connect()) refresh()
                        }
                    }
                })
            }
        }
    }

    private class SubscriptionCache(context: Context) {
        private val prefs = SecurePreferences.open(context, PREFS)

        val subscribed: Boolean
            get() = prefs.getBoolean(KEY_SUBSCRIBED, false)

        /** Пишем только после успешного ответа Play, вместе с временем проверки. */
        fun save(subscribed: Boolean) {
            prefs.edit()
                .putBoolean(KEY_SUBSCRIBED, subscribed)
                .putLong(KEY_CHECKED_AT, System.currentTimeMillis())
                .apply()
        }

        companion object {
            private const val PREFS = "truckorig_billing"
            private const val KEY_SUBSCRIBED = "is_subscribed"
            private const val KEY_CHECKED_AT = "last_online_check_at"
        }
    }

    companion object {
        private const val MAX_RECONNECT = 3
        private const val RECONNECT_DELAY_MS = 1_500L
    }
}

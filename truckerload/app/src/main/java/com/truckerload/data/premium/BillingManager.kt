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
import java.util.concurrent.atomic.AtomicBoolean

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

    private val _isSubscribed = MutableStateFlow(cache.readFresh())
    /** true — активная подписка или бесплатный месяц Play; false — нет или истекла. */
    val isSubscribed: StateFlow<Boolean> = _isSubscribed.asStateFlow()

    private var reconnectAttempt = 0
    private var connecting: kotlinx.coroutines.CompletableDeferred<Boolean>? = null
    private val reconnectScheduled = AtomicBoolean(false)

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
            _isSubscribed.value = cache.readFresh()
            return
        }
        retryPendingAcks()
        val purchases = queryPurchases()
        if (purchases == null) {
            _isSubscribed.value = cache.readFresh()
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
                scope.launch {
                    val active = applyPurchases(purchases.orEmpty())
                    cache.save(active)
                    _isSubscribed.value = active
                }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> start()
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
        }
    }

    /**
     * PURCHASED по нашему product id. Триал Play тоже приходит как PURCHASED.
     * Неподтверждённая покупка не открывает Premium: Google отзывает её через 3 дня.
     */
    private suspend fun applyPurchases(purchases: List<Purchase>): Boolean {
        val ours = purchases.filter { it.products.contains(PremiumPolicy.PLAY_PRODUCT_ID) }
        val purchased = ours.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        var ackFailed = false
        for (purchase in purchased.filter { !it.isAcknowledged }) {
            if (acknowledge(purchase.purchaseToken)) {
                cache.removePendingAck(purchase.purchaseToken)
            } else {
                cache.addPendingAck(purchase.purchaseToken)
                ackFailed = true
            }
        }
        val alreadyAcked = purchased.any { it.isAcknowledged }
        return alreadyAcked || (purchased.isNotEmpty() && !ackFailed)
    }

    private suspend fun retryPendingAcks() {
        for (token in cache.pendingAcks()) {
            if (acknowledge(token)) cache.removePendingAck(token)
        }
    }

    private suspend fun acknowledge(token: String): Boolean = suspendCancellableCoroutine { cont ->
        client.acknowledgePurchase(
            AcknowledgePurchaseParams.newBuilder().setPurchaseToken(token).build(),
        ) { result ->
            if (cont.isActive) {
                cont.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
            }
        }
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

    /** Оффер с тегом [PremiumPolicy.FREE_MONTH_OFFER_TAG], иначе базовый. Нулевая цена сама по себе не выбирается. */
    private fun playOffer(details: ProductDetails) =
        details.subscriptionOfferDetails.orEmpty().let { offers ->
            offers.firstOrNull { PremiumPolicy.FREE_MONTH_OFFER_TAG in it.offerTags }
                ?: offers.firstOrNull()
        }

    private suspend fun connect(): Boolean {
        if (client.isReady) return true
        val pending = connectLock.withLock {
            if (client.isReady) return true
            connecting?.let { return@withLock it }
            val created = kotlinx.coroutines.CompletableDeferred<Boolean>()
            connecting = created
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    connecting = null
                    created.complete(result.responseCode == BillingClient.BillingResponseCode.OK)
                }

                override fun onBillingServiceDisconnected() {
                    // Не вызывать connect() отсюда: этот callback может прийти, пока connect() ещё ждёт.
                    scheduleReconnect()
                }
            })
            created
        }
        return pending.await()
    }

    private fun scheduleReconnect() {
        if (reconnectAttempt >= MAX_RECONNECT) return
        if (!reconnectScheduled.compareAndSet(false, true)) return
        reconnectAttempt += 1
        scope.launch {
            delay(RECONNECT_DELAY_MS * reconnectAttempt)
            reconnectScheduled.set(false)
            if (connect()) refresh()
        }
    }

    private class SubscriptionCache(context: Context) {
        private val prefs = SecurePreferences.open(context, PREFS)
        private val encrypted = !SecurePreferences.plaintextFallbackUsed

        /** Просроченный или незашифрованный кэш не открывает Premium. */
        fun readFresh(now: Long = System.currentTimeMillis()): Boolean {
            if (!encrypted) return false
            val checkedAt = prefs.getLong(KEY_CHECKED_AT, 0L)
            if (checkedAt <= 0L || now - checkedAt > PremiumPolicy.SUBSCRIPTION_CACHE_TTL_MS) return false
            return prefs.getBoolean(KEY_SUBSCRIBED, false)
        }

        fun save(subscribed: Boolean) {
            if (!encrypted) return
            prefs.edit()
                .putBoolean(KEY_SUBSCRIBED, subscribed)
                .putLong(KEY_CHECKED_AT, System.currentTimeMillis())
                .apply()
        }

        fun pendingAcks(): Set<String> =
            if (!encrypted) emptySet() else prefs.getStringSet(KEY_PENDING_ACKS, emptySet()).orEmpty()

        fun addPendingAck(token: String) {
            if (!encrypted || token.isBlank()) return
            prefs.edit().putStringSet(KEY_PENDING_ACKS, pendingAcks() + token).apply()
        }

        fun removePendingAck(token: String) {
            if (!encrypted) return
            prefs.edit().putStringSet(KEY_PENDING_ACKS, pendingAcks() - token).apply()
        }

        companion object {
            private const val PREFS = "truckorig_billing"
            private const val KEY_SUBSCRIBED = "is_subscribed"
            private const val KEY_CHECKED_AT = "last_online_check_at"
            private const val KEY_PENDING_ACKS = "pending_ack_tokens"
        }
    }

    companion object {
        private const val MAX_RECONNECT = 3
        private const val RECONNECT_DELAY_MS = 1_500L
    }
}

package com.truckerload.data.premium

import android.app.Activity
import android.content.Context
import com.truckerload.domain.premium.PremiumPolicy
import com.truckerload.domain.premium.PremiumStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Фасад для экранов. Статус подписки — это [BillingManager.isSubscribed]:
 * true, если Google Play видит активную подписку или её бесплатный месяц.
 */
class PremiumAccess private constructor(context: Context) {
    private val billing = BillingManager(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val isSubscribed: StateFlow<Boolean> = billing.isSubscribed

    val status: StateFlow<PremiumStatus> = billing.isSubscribed
        .map { PremiumPolicy.status(it) }
        .stateIn(scope, SharingStarted.Eagerly, PremiumPolicy.status(billing.isSubscribed.value))

    init {
        billing.start()
    }

    fun refresh() {
        scope.launch(Dispatchers.IO) { billing.refresh() }
    }

    fun isUnlocked(): Boolean = isSubscribed.value

    suspend fun formattedPrice(): String? = billing.formattedPrice()

    fun purchase(activity: Activity, onResult: (String?) -> Unit) {
        scope.launch(Dispatchers.IO) {
            val error = runCatching { billing.launchBillingFlow(activity) }.getOrElse { it.message }
            withContext(Dispatchers.Main) {
                onResult(error?.takeUnless { it == "already_owned" })
            }
        }
    }

    companion object {
        @Volatile
        private var instance: PremiumAccess? = null

        fun get(context: Context): PremiumAccess {
            instance?.let { return it }
            return synchronized(this) {
                instance ?: PremiumAccess(context.applicationContext).also { instance = it }
            }
        }
    }
}

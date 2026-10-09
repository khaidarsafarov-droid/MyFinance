package com.truckerload.data.premium

import android.app.Activity
import android.content.Context
import com.truckerload.domain.premium.PremiumPolicy
import com.truckerload.domain.premium.PremiumStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Trial plus Google Play subscription. The journal does not go through this gate. */
class PremiumAccess private constructor(context: Context) {
    private val app = context.applicationContext
    private val store = PremiumStore(app)
    private val billing = PlayPremiumBilling(app, store) { refresh() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _status = MutableStateFlow(initialStatus())
    val status: StateFlow<PremiumStatus> = _status.asStateFlow()

    init {
        refresh()
    }

    private fun initialStatus(): PremiumStatus =
        PremiumPolicy.status(store.isSubscribed())

    fun refresh() {
        scope.launch {
            val subscribed = runCatching { billing.hasActiveSubscription() }.getOrDefault(store.isSubscribed())
            if (subscribed != store.isSubscribed()) store.setSubscribed(subscribed)
            _status.value = PremiumPolicy.status(subscribed)
        }
    }

    fun isUnlocked(): Boolean = _status.value.unlocked

    suspend fun formattedPrice(): String? = billing.formattedPrice()

    fun purchase(activity: Activity, onResult: (String?) -> Unit) {
        scope.launch {
            val error = runCatching { billing.launchPurchase(activity) }.getOrElse { it.message }
            if (error == "already_owned") refresh()
            withContext(Dispatchers.Main) { onResult(error?.takeUnless { it == "already_owned" }) }
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

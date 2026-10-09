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

/** Trial plus Google Play subscription. The journal does not go through this gate. */
class PremiumAccess private constructor(context: Context) {
    private val app = context.applicationContext
    private val store = PremiumStore(app)
    private val billing = PlayPremiumBilling(app, store)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _status = MutableStateFlow(initialStatus())
    val status: StateFlow<PremiumStatus> = _status.asStateFlow()

    init {
        refresh()
    }

    private fun initialStatus(): PremiumStatus {
        val now = System.currentTimeMillis()
        val started = store.ensureTrialStarted(now)
        return PremiumPolicy.status(started, store.isSubscribed(), now)
    }

    fun refresh() {
        scope.launch {
            val started = store.ensureTrialStarted(System.currentTimeMillis())
            val subscribed = runCatching { billing.hasActiveSubscription() }.getOrDefault(store.isSubscribed())
            if (subscribed != store.isSubscribed()) store.setSubscribed(subscribed)
            _status.value = PremiumPolicy.status(started, subscribed, System.currentTimeMillis())
        }
    }

    fun isUnlocked(): Boolean = _status.value.unlocked

    fun purchase(activity: Activity, onResult: (String?) -> Unit) {
        scope.launch {
            val error = billing.launchPurchase(activity)
            refresh()
            onResult(error)
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

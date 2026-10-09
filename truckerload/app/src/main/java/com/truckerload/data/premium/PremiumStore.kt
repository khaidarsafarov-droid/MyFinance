package com.truckerload.data.premium

import android.content.Context

/** Last known Google Play subscription. The free month is a Play offer, not stored here. */
class PremiumStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isSubscribed(): Boolean = prefs.getBoolean(KEY_SUBSCRIBED, false)

    fun setSubscribed(subscribed: Boolean) {
        prefs.edit().putBoolean(KEY_SUBSCRIBED, subscribed).apply()
    }

    companion object {
        private const val PREFS = "truckorig_premium"
        private const val KEY_SUBSCRIBED = "play_subscribed"
    }
}

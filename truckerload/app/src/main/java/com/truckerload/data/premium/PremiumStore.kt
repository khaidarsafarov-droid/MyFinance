package com.truckerload.data.premium

import android.content.Context

/** Device-level trial clock and the last known Play subscription. One trial per install. */
class PremiumStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun trialStartedAt(): Long = prefs.getLong(KEY_TRIAL_START, 0L)

    fun ensureTrialStarted(now: Long): Long {
        val existing = trialStartedAt()
        if (existing > 0L) return existing
        prefs.edit().putLong(KEY_TRIAL_START, now).apply()
        return now
    }

    fun isSubscribed(): Boolean = prefs.getBoolean(KEY_SUBSCRIBED, false)

    fun setSubscribed(subscribed: Boolean) {
        prefs.edit().putBoolean(KEY_SUBSCRIBED, subscribed).apply()
    }

    fun shouldShowTrialWelcome(): Boolean = !prefs.getBoolean(KEY_WELCOME_SHOWN, false)

    fun markTrialWelcomeShown() {
        prefs.edit().putBoolean(KEY_WELCOME_SHOWN, true).apply()
    }

    companion object {
        private const val PREFS = "truckorig_premium"
        private const val KEY_TRIAL_START = "trial_started_at"
        private const val KEY_SUBSCRIBED = "play_subscribed"
        private const val KEY_WELCOME_SHOWN = "trial_welcome_shown"
    }
}

package com.truckerload.data.premium

import android.content.Context

/** Remembers that the first-run Play offer was skipped or completed. Not a trial timer. */
class PremiumOfferStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun wasHandled(): Boolean = prefs.getBoolean(KEY_HANDLED, false)

    fun markHandled() {
        prefs.edit().putBoolean(KEY_HANDLED, true).apply()
    }

    private companion object {
        const val PREFS = "truckorig_premium_offer"
        const val KEY_HANDLED = "offer_handled"
    }
}

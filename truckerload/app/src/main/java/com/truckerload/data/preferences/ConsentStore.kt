package com.truckerload.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.truckerload.domain.account.AccountConsents

/** Per-account ToS (required) vs analytics (optional, never pre-checked). */
class ConsentStore(context: Context) {
    private val appContext = context.applicationContext

    fun load(userId: String): AccountConsents {
        val prefs = prefs(userId)
        val tosAt = prefs.getLong(KEY_TOS_AT, 0L).takeIf { it > 0L }
        val analyticsAt = prefs.getLong(KEY_ANALYTICS_AT, 0L).takeIf { it > 0L }
        return AccountConsents(
            tosAccepted = tosAt != null,
            analyticsAccepted = analyticsAt != null,
            ageConfirmed = prefs.getBoolean(KEY_AGE, false),
            acceptedTosAt = tosAt,
            analyticsConsentAt = analyticsAt,
        )
    }

    fun save(
        userId: String,
        tosAccepted: Boolean,
        analyticsAccepted: Boolean,
        ageConfirmed: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        prefs(userId).edit {
            if (tosAccepted) putLong(KEY_TOS_AT, nowMillis) else remove(KEY_TOS_AT)
            if (analyticsAccepted) putLong(KEY_ANALYTICS_AT, nowMillis) else remove(KEY_ANALYTICS_AT)
            putBoolean(KEY_AGE, ageConfirmed)
        }
    }

    fun clear(userId: String) {
        prefs(userId).edit { clear() }
    }

    private fun prefs(userId: String): SharedPreferences =
        appContext.getSharedPreferences(
            "truckerload_consent_${AccountIds.sanitizeFilePart(userId)}",
            Context.MODE_PRIVATE,
        )

    companion object {
        private const val KEY_TOS_AT = "accepted_tos_at"
        private const val KEY_ANALYTICS_AT = "analytics_consent_at"
        private const val KEY_AGE = "age_confirmed"
    }
}

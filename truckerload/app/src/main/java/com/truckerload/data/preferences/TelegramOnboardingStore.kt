package com.truckerload.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Per-account flag: the first-login Telegram token wizard was finished or skipped.
 */
class TelegramOnboardingStore(
    context: Context,
    userId: String? = AuthStore(context).currentUserIdOrNull(),
) {
    private val resolvedUserId = userId?.trim()?.takeIf { it.isNotBlank() }
    private val prefs: SharedPreferences = SecurePreferences.open(
        context.applicationContext,
        prefsName(resolvedUserId),
    )

    fun isCompleted(): Boolean = prefs.getBoolean(KEY_COMPLETED, false)

    fun markCompleted() {
        prefs.edit { putBoolean(KEY_COMPLETED, true) }
    }

    /**
     * Show the wizard once after the first account open, unless a token is already
     * stored or the app uses server-side Telegram sync.
     */
    fun shouldPrompt(context: Context): Boolean {
        if (isCompleted()) return false
        // FIX: check the same account and only a token the user actually saved
        if (TelegramTokenStore(context, resolvedUserId).hasPersistedToken()) {
            markCompleted()
            return false
        }
        return true
    }

    companion object {
        private const val KEY_COMPLETED = "telegram_onboarding_completed"

        private fun prefsName(userId: String?): String {
            val part = AccountIds.sanitizeFilePart(userId?.trim().orEmpty())
            return if (part.isBlank()) "telegram_onboarding_enc"
            else "telegram_onboarding_enc_$part"
        }
    }
}

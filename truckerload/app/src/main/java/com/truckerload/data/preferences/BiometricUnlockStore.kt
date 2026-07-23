package com.truckerload.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Opt-in fingerprint / face unlock after a successful email+password login.
 * The biometric prompt only unlocks an already-persisted session — it never
 * replaces password verification for the first sign-in.
 */
class BiometricUnlockStore(context: Context) {
    private val prefs: SharedPreferences =
        SecurePreferences.open(context.applicationContext, PREFS_NAME)

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun setEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_ENABLED, enabled) }
    }

    fun clear() {
        prefs.edit { clear() }
    }

    companion object {
        private const val PREFS_NAME = "truckerload_biometric_unlock_enc"
        private const val KEY_ENABLED = "enabled"
    }
}

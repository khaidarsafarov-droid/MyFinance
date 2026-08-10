package com.truckerload.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.truckerload.BuildConfig
import com.truckerload.utils.CrashReporting

/**
 * Encrypted SharedPreferences with one-time migration from legacy plain-text stores.
 *
 * When Android Keystore / EncryptedSharedPreferences is unavailable (common on some OEM
 * tablets), falls back to a durable plaintext file (`{name}_fallback`) and sets
 * [plaintextFallbackUsed]. High-value secrets (bot tokens, API keys) still refuse disk
 * writes via [requireEncryptedForSecretWrite]. Session identity and PBKDF2 password
 * verifiers are allowed on the degraded store so users are not forced to re-register.
 *
 * Important: do not delete the fallback file on every [open] — Keystore can stay broken
 * across process restarts; wiping would erase registration/login every cold start.
 */
object SecurePreferences {

    fun open(context: Context, name: String): SharedPreferences {
        val appContext = context.applicationContext
        return try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                name,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: Exception) {
            Log.e(TAG, "EncryptedSharedPreferences unavailable for $name", e)
            plaintextFallbackUsed = true
            CrashReporting.setCustomKey("secure_prefs_fallback", true)
            CrashReporting.setCustomKey("secure_prefs_name", name)
            CrashReporting.setCustomKey("secure_prefs_release", !BuildConfig.DEBUG)
            CrashReporting.recordException(e)

            val fallbackName = fallbackName(name)
            val fallback = appContext.getSharedPreferences(fallbackName, Context.MODE_PRIVATE)
            // One-time rescue from a same-named plaintext file (older debug fallback).
            if (fallback.all.isEmpty()) {
                val legacySameName = appContext.getSharedPreferences(name, Context.MODE_PRIVATE)
                if (legacySameName.all.isNotEmpty()) {
                    fallback.edit {
                        legacySameName.all.forEach { (key, value) ->
                            when (value) {
                                is String -> putString(key, value)
                                is Boolean -> putBoolean(key, value)
                                is Int -> putInt(key, value)
                                is Long -> putLong(key, value)
                                is Float -> putFloat(key, value)
                            }
                        }
                    }
                    legacySameName.edit { clear() }
                }
            }
            Log.w(TAG, "Durable plaintext fallback for $name → $fallbackName")
            return fallback
        }
    }

    fun fallbackName(name: String): String = "${name}_fallback"

    /**
     * True after any [open] call fell back away from EncryptedSharedPreferences.
     * Callers storing bot tokens / passwords should surface a user warning when set.
     */
    @Volatile
    var plaintextFallbackUsed: Boolean = false
        private set

    /** Test-only reset. */
    internal fun resetFallbackForTests() {
        plaintextFallbackUsed = false
    }

    /** Test-only: simulate Keystore / encrypted-prefs failure. */
    internal fun markFallbackForTests() {
        plaintextFallbackUsed = true
    }

    fun requireEncryptedForSecretWrite(secretName: String) {
        check(!plaintextFallbackUsed) {
            "Secure storage unavailable; refusing to persist $secretName"
        }
    }

    /**
     * Copies all entries from a legacy plain prefs file into [securePrefs], then clears the legacy file.
     */
    fun migratePlainToSecure(
        context: Context,
        legacyName: String,
        securePrefs: SharedPreferences,
        migrationFlagKey: String,
    ) {
        if (securePrefs.getBoolean(migrationFlagKey, false)) return
        val legacy = context.applicationContext.getSharedPreferences(legacyName, Context.MODE_PRIVATE)
        if (legacy.all.isEmpty()) {
            securePrefs.edit { putBoolean(migrationFlagKey, true) }
            return
        }
        securePrefs.edit {
            legacy.all.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Boolean -> putBoolean(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Float -> putFloat(key, value)
                }
            }
            putBoolean(migrationFlagKey, true)
        }
        legacy.edit { clear() }
    }

    private const val TAG = "SecurePreferences"
}

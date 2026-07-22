package com.truckerload.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted SharedPreferences with one-time migration from legacy plain-text stores.
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
            Log.e(TAG, "EncryptedSharedPreferences unavailable for $name — using plaintext fallback", e)
            plaintextFallbackUsed = true
            appContext.getSharedPreferences(name, Context.MODE_PRIVATE)
        }
    }

    /**
     * True after any [open] call fell back to unencrypted MODE_PRIVATE prefs.
     * Callers storing bot tokens / passwords should surface a user warning when set.
     */
    @Volatile
    var plaintextFallbackUsed: Boolean = false
        private set

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

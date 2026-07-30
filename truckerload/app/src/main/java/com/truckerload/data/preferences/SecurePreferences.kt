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
 * Debug: may fall back to plaintext (banner via [plaintextFallbackUsed]).
 * Release: fail closed — wipe any same-named plaintext file and keep secrets off disk
 * via [InMemorySharedPreferences]; secret writes still refused by [requireEncryptedForSecretWrite].
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

            // Never leave secrets on a plaintext disk file.
            runCatching {
                appContext.getSharedPreferences(name, Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .commit()
            }

            if (BuildConfig.DEBUG) {
                Log.w(TAG, "DEBUG fallback to plaintext SharedPreferences for $name")
                return appContext.getSharedPreferences(name, Context.MODE_PRIVATE)
            }

            Log.e(TAG, "RELEASE fail-closed: in-memory prefs only for $name (re-login required)")
            return InMemorySharedPreferences()
        }
    }

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
        // Never copy secrets into a degraded store.
        if (plaintextFallbackUsed && !BuildConfig.DEBUG) {
            legacy.edit { clear() }
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

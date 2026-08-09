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
 * When the Android Keystore / EncryptedSharedPreferences stack fails (common on some
 * tablets and OEM builds), fall back to durable plaintext prefs so login/registration
 * survive process death. Callers that store raw secrets (bot tokens, JWTs) must still
 * gate writes with [requireEncryptedForSecretWrite] / skip persistence when
 * [plaintextFallbackUsed] is true. PBKDF2 password verifiers are safe to persist.
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

            // Durable plaintext fallback — identity must survive restarts so users are
            // not forced to register again. Raw JWTs/bot tokens stay out of this store
            // via caller-side gates when [plaintextFallbackUsed] is set.
            Log.w(
                TAG,
                "Falling back to plaintext SharedPreferences for $name " +
                    "(debug=${BuildConfig.DEBUG}); session identity will still persist",
            )
            return appContext.getSharedPreferences(name, Context.MODE_PRIVATE)
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
        // Prefer migrating identity into the (possibly plaintext) durable store over wiping it.
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

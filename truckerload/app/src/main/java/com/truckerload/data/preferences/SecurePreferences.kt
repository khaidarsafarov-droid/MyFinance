package com.truckerload.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.truckerload.BuildConfig
import com.truckerload.data.local.DatabasePassphraseStore
import com.truckerload.utils.CrashReporting
import com.truckerload.utils.logFailure
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.ProviderException

/**
 * Encrypted SharedPreferences.
 *
 * Secret stores ([open] with `secrets = true`) never fall back to a plaintext
 * file. If the Android Keystore is unavailable, they throw
 * [SecureStorageUnavailableException] and the caller must not write the secret.
 *
 * Non-secret metadata (session flags, onboarding) may still use a plaintext
 * file so the app can open. Password verifiers, CDL keys, and tokens must not
 * use that path.
 *
 * Robolectric has no Android Keystore. Unit tests get a process-local
 * preferences file. That branch does not run on a device.
 */
object SecurePreferences {

    fun open(context: Context, name: String, secrets: Boolean = false): SharedPreferences {
        val appContext = context.applicationContext
        return try {
            createEncrypted(appContext, name)
        } catch (e: GeneralSecurityException) {
            onEncryptedFailure(appContext, name, secrets, e)
        } catch (e: IOException) {
            onEncryptedFailure(appContext, name, secrets, e)
        } catch (e: ProviderException) {
            onEncryptedFailure(appContext, name, secrets, e)
        }
    }

    private fun createEncrypted(appContext: Context, name: String): SharedPreferences {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            appContext,
            name,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private fun onEncryptedFailure(
        appContext: Context,
        name: String,
        secrets: Boolean,
        error: Exception,
    ): SharedPreferences {
        if (DatabasePassphraseStore.isRobolectric()) {
            return appContext.getSharedPreferences(name, Context.MODE_PRIVATE)
        }
        if (secrets) {
            throw SecureStorageUnavailableException(name, error)
        }
        return plaintextFallback(appContext, name, error)
    }

    private fun plaintextFallback(appContext: Context, name: String, e: Exception): SharedPreferences {
        logFailure(TAG, "EncryptedSharedPreferences unavailable for $name", e)
        plaintextFallbackUsed = true
        CrashReporting.setCustomKey("secure_prefs_fallback", true)
        CrashReporting.setCustomKey("secure_prefs_name", name)
        CrashReporting.setCustomKey("secure_prefs_release", !BuildConfig.DEBUG)
        CrashReporting.recordException(e)

        val fallbackName = fallbackName(name)
        val fallback = appContext.getSharedPreferences(fallbackName, Context.MODE_PRIVATE)
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
        Log.w(TAG, "Plaintext fallback for non-secret prefs $name → $fallbackName")
        return fallback
    }

    fun fallbackName(name: String): String = "${name}_fallback"

    /**
     * True after a non-secret [open] fell back to plaintext.
     * Secret stores do not set this; they throw instead.
     */
    @Volatile
    var plaintextFallbackUsed: Boolean = false
        private set

    internal fun resetFallbackForTests() {
        plaintextFallbackUsed = false
    }

    internal fun markFallbackForTests() {
        plaintextFallbackUsed = true
    }

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

class SecureStorageUnavailableException(
    name: String,
    cause: Throwable,
) : IllegalStateException("Secure storage unavailable for $name", cause)

package com.truckerload.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Local email/password store for offline login and Supabase outage / rate-limit fallback.
 * Credentials are keyed by normalized email so multiple users on one device stay isolated.
 */
class AuthCredentialsStore(context: Context) {
    private val prefs: SharedPreferences = openPrefs(context)

    fun saveCredentials(email: String, password: String) {
        val key = normalizeEmail(email)
        require(key.isNotBlank()) { "email required" }
        require(password.isNotBlank()) { "password required" }
        prefs.edit {
            putString(pwdKey(key), password)
            putString(KEY_LAST_EMAIL, key)
            // Drop legacy single-slot keys after migrating into the map.
            remove(KEY_EMAIL)
            remove(KEY_PASSWORD)
        }
    }

    fun getEmail(): String = prefs.getString(KEY_LAST_EMAIL, null)
        ?: prefs.getString(KEY_EMAIL, "")
        ?: ""

    fun getPassword(): String {
        val email = getEmail()
        if (email.isBlank()) return prefs.getString(KEY_PASSWORD, "") ?: ""
        return passwordFor(email).orEmpty()
    }

    fun passwordFor(email: String): String? {
        val key = normalizeEmail(email)
        if (key.isBlank()) return null
        prefs.getString(pwdKey(key), null)?.let { return it }
        // Legacy single-account prefs.
        val legacyEmail = prefs.getString(KEY_EMAIL, null)?.let(::normalizeEmail)
        if (legacyEmail != null && legacyEmail == key) {
            return prefs.getString(KEY_PASSWORD, null)
        }
        return null
    }

    fun validateCredentials(email: String, password: String): Boolean {
        if (email.isBlank() || password.isBlank()) return false
        val saved = passwordFor(email) ?: return false
        return saved == password
    }

    fun hasCredentials(): Boolean =
        prefs.all.keys.any { it.startsWith(KEY_PWD_PREFIX) } ||
            prefs.getString(KEY_EMAIL, null).orEmpty().isNotBlank()

    fun hasCredentialsFor(email: String): Boolean = !passwordFor(email).isNullOrBlank()

    companion object {
        private const val PREFS_NAME = "truckerload_auth_credentials_enc"
        private const val LEGACY_PREFS_NAME = "truckerload_auth_credentials"
        private const val MIGRATION_FLAG = "migrated_from_plain"
        private const val KEY_EMAIL = "email"
        private const val KEY_PASSWORD = "password"
        private const val KEY_LAST_EMAIL = "last_email"
        private const val KEY_PWD_PREFIX = "pwd:"

        fun normalizeEmail(email: String): String = email.trim().lowercase()

        private fun pwdKey(normalizedEmail: String): String = KEY_PWD_PREFIX + normalizedEmail

        private fun openPrefs(context: Context): SharedPreferences {
            val secure = SecurePreferences.open(context, PREFS_NAME)
            SecurePreferences.migratePlainToSecure(
                context = context,
                legacyName = LEGACY_PREFS_NAME,
                securePrefs = secure,
                migrationFlagKey = MIGRATION_FLAG,
            )
            // One-shot: lift legacy single email/password into the multi-user map.
            val legacyEmail = secure.getString(KEY_EMAIL, null)?.let(::normalizeEmail)
            val legacyPassword = secure.getString(KEY_PASSWORD, null)
            if (!legacyEmail.isNullOrBlank() && !legacyPassword.isNullOrBlank() &&
                secure.getString(pwdKey(legacyEmail), null).isNullOrBlank()
            ) {
                secure.edit {
                    putString(pwdKey(legacyEmail), legacyPassword)
                    putString(KEY_LAST_EMAIL, legacyEmail)
                }
            }
            return secure
        }
    }
}

package com.truckerload.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.truckerload.data.privacy.BackupCrypto

/**
 * Local email/password store for offline login and Supabase outage / rate-limit fallback.
 * Passwords are stored as salted SHA-256 hashes (never recoverable plaintext).
 * Credentials are keyed by normalized email so multiple users on one device stay isolated.
 */
class AuthCredentialsStore(context: Context) {
    private val prefs: SharedPreferences = openPrefs(context)

    fun saveCredentials(email: String, password: String) {
        val key = normalizeEmail(email)
        require(key.isNotBlank()) { "email required" }
        require(password.isNotBlank()) { "password required" }
        val (hash, salt) = BackupCrypto.hashPassword(password)
        prefs.edit {
            putString(pwdKey(key), hash)
            putString(saltKey(key), salt)
            putString(KEY_LAST_EMAIL, key)
            // Drop legacy single-slot keys after migrating into the map.
            remove(KEY_EMAIL)
            remove(KEY_PASSWORD)
        }
    }

    fun getEmail(): String = prefs.getString(KEY_LAST_EMAIL, null)
        ?: prefs.getString(KEY_EMAIL, "")
        ?: ""

    /** @deprecated Passwords are hashed; always empty. Use [validateCredentials]. */
    fun getPassword(): String = ""

    fun passwordFor(email: String): String? {
        val key = normalizeEmail(email)
        if (key.isBlank()) return null
        prefs.getString(pwdKey(key), null)?.let { return it }
        val legacyEmail = prefs.getString(KEY_EMAIL, null)?.let(::normalizeEmail)
        if (legacyEmail != null && legacyEmail == key) {
            return prefs.getString(KEY_PASSWORD, null)
        }
        return null
    }

    fun validateCredentials(email: String, password: String): Boolean {
        if (email.isBlank() || password.isBlank()) return false
        val key = normalizeEmail(email)
        val stored = prefs.getString(pwdKey(key), null)
        val salt = prefs.getString(saltKey(key), null)
        if (stored != null && salt != null) {
            return BackupCrypto.verifyPassword(password, stored, salt)
        }
        // Legacy plaintext (pre-hash) — verify then upgrade in place.
        val legacy = passwordFor(email) ?: return false
        if (legacy == password) {
            saveCredentials(email, password)
            return true
        }
        return false
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
        private const val KEY_SALT_PREFIX = "salt:"

        fun normalizeEmail(email: String): String = email.trim().lowercase()

        private fun pwdKey(normalizedEmail: String): String = KEY_PWD_PREFIX + normalizedEmail
        private fun saltKey(normalizedEmail: String): String = KEY_SALT_PREFIX + normalizedEmail

        private fun openPrefs(context: Context): SharedPreferences {
            val secure = SecurePreferences.open(context, PREFS_NAME)
            SecurePreferences.migratePlainToSecure(
                context = context,
                legacyName = LEGACY_PREFS_NAME,
                securePrefs = secure,
                migrationFlagKey = MIGRATION_FLAG,
            )
            val legacyEmail = secure.getString(KEY_EMAIL, null)?.let(::normalizeEmail)
            val legacyPassword = secure.getString(KEY_PASSWORD, null)
            if (!legacyEmail.isNullOrBlank() && !legacyPassword.isNullOrBlank() &&
                secure.getString(pwdKey(legacyEmail), null).isNullOrBlank()
            ) {
                val (hash, salt) = BackupCrypto.hashPassword(legacyPassword)
                secure.edit {
                    putString(pwdKey(legacyEmail), hash)
                    putString(saltKey(legacyEmail), salt)
                    putString(KEY_LAST_EMAIL, legacyEmail)
                    remove(KEY_EMAIL)
                    remove(KEY_PASSWORD)
                }
            }
            return secure
        }
    }
}

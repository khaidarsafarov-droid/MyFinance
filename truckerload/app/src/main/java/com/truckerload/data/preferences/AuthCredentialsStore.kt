package com.truckerload.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.truckerload.data.auth.PasswordPolicy
import com.truckerload.utils.normalizeKey

/**
 * Local email/password store for offline login.
 * Credentials are keyed by normalized email so multiple users on one device stay isolated.
 *
 * Passwords are stored as PBKDF2 verifiers (see [PasswordPolicy]). Legacy plaintext
 * entries are hashed once on the next successful check, and only into encrypted prefs.
 * If encrypted storage is unavailable, this store does not write the verifier.
 */
class AuthCredentialsStore(context: Context) {
    private val prefs: SharedPreferences? = openPrefs(context)

    fun saveCredentials(email: String, password: String, enforcePolicy: Boolean = true) {
        val key = normalizeEmail(email)
        require(key.isNotBlank()) { "email required" }
        require(password.isNotBlank()) { "password required" }
        val target = prefs ?: error("Secure storage unavailable; refusing to persist password verifier")
        if (enforcePolicy && !PasswordPolicy.isHashed(password)) {
            val validation = PasswordPolicy.validate(password)
            require(validation.ok) { "password policy rejected" }
        }
        val toStore = if (PasswordPolicy.isHashed(password)) password else PasswordPolicy.hash(password)
        target.edit {
            putString(pwdKey(key), toStore)
            putString(KEY_LAST_EMAIL, key)
            remove(KEY_EMAIL)
            remove(KEY_PASSWORD)
        }
    }

    fun getEmail(): String {
        val stored = prefs ?: return ""
        return stored.getString(KEY_LAST_EMAIL, null)
            ?: stored.getString(KEY_EMAIL, "")
            ?: ""
    }

    /** Never returns a recoverable password — hashes are not reversible. */
    fun getPassword(): String = ""

    fun passwordFor(email: String): String? {
        val stored = prefs ?: return null
        val key = normalizeEmail(email)
        if (key.isBlank()) return null
        stored.getString(pwdKey(key), null)?.let { return it }
        val legacyEmail = stored.getString(KEY_EMAIL, null)?.let(::normalizeEmail)
        if (legacyEmail != null && legacyEmail == key) {
            return stored.getString(KEY_PASSWORD, null)
        }
        return null
    }

    fun validateCredentials(email: String, password: String): Boolean {
        if (email.isBlank() || password.isBlank()) return false
        val saved = passwordFor(email) ?: return false
        val ok = PasswordPolicy.matches(password, saved)
        if (ok && !PasswordPolicy.isHashed(saved)) {
            // Upgrade legacy plaintext at rest (skip policy — password already authenticated).
            saveCredentials(email, password, enforcePolicy = false)
        }
        return ok
    }

    fun hasCredentials(): Boolean {
        val stored = prefs ?: return false
        return stored.all.keys.any { it.startsWith(KEY_PWD_PREFIX) } ||
            stored.getString(KEY_EMAIL, null).orEmpty().isNotBlank()
    }

    fun hasCredentialsFor(email: String): Boolean = !passwordFor(email).isNullOrBlank()

    fun saveBoundUserId(email: String, userId: String) {
        val stored = prefs ?: return
        val key = normalizeEmail(email)
        val id = userId.trim()
        if (key.isBlank() || id.isBlank()) return
        stored.edit { putString(uidKey(key), id) }
    }

    fun boundUserIdFor(email: String): String? {
        val stored = prefs ?: return null
        val key = normalizeEmail(email)
        if (key.isBlank()) return null
        return stored.getString(uidKey(key), null)?.trim()?.takeIf { it.isNotBlank() }
    }

    fun clearCredentials(email: String) {
        val stored = prefs ?: return
        val key = normalizeEmail(email)
        if (key.isBlank()) return
        stored.edit {
            remove(pwdKey(key))
            remove(uidKey(key))
            if (getEmail() == key) {
                remove(KEY_LAST_EMAIL)
                remove(KEY_EMAIL)
                remove(KEY_PASSWORD)
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "truckerload_auth_credentials_enc"
        private const val LEGACY_PREFS_NAME = "truckerload_auth_credentials"
        private const val MIGRATION_FLAG = "migrated_from_plain"
        private const val KEY_EMAIL = "email"
        private const val KEY_PASSWORD = "password"
        private const val KEY_LAST_EMAIL = "last_email"
        private const val KEY_PWD_PREFIX = "pwd:"
        private const val KEY_UID_PREFIX = "uid:"

        fun normalizeEmail(email: String): String = email.normalizeKey()

        private fun pwdKey(normalizedEmail: String): String = KEY_PWD_PREFIX + normalizedEmail

        private fun uidKey(normalizedEmail: String): String = KEY_UID_PREFIX + normalizedEmail

        private fun openPrefs(context: Context): SharedPreferences? {
            val secure = try {
                SecurePreferences.open(context, PREFS_NAME, secrets = true)
            } catch (_: SecureStorageUnavailableException) {
                return null
            }
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
                val hashed = if (PasswordPolicy.isHashed(legacyPassword)) {
                    legacyPassword
                } else {
                    PasswordPolicy.hash(legacyPassword)
                }
                secure.edit {
                    putString(pwdKey(legacyEmail), hashed)
                    putString(KEY_LAST_EMAIL, legacyEmail)
                    remove(KEY_PASSWORD)
                }
            }
            return secure
        }
    }
}

package com.truckerload.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Локальное хранилище email/пароля для входа по почте (offline / LOCAL_ONLY_MODE).
 * Значения хранятся в EncryptedSharedPreferences.
 */
class AuthCredentialsStore(context: Context) {
    private val prefs: SharedPreferences = openPrefs(context)

    fun saveCredentials(email: String, password: String) {
        prefs.edit {
            putString(KEY_EMAIL, email)
            putString(KEY_PASSWORD, password)
        }
    }

    fun getEmail(): String = prefs.getString(KEY_EMAIL, "") ?: ""
    fun getPassword(): String = prefs.getString(KEY_PASSWORD, "") ?: ""

    fun validateCredentials(email: String, password: String): Boolean {
        if (email.isBlank() || password.isBlank()) return false
        val savedEmail = getEmail()
        val savedPassword = getPassword()
        return savedEmail.equals(email, ignoreCase = true) && savedPassword == password
    }

    fun hasCredentials(): Boolean = getEmail().isNotBlank()

    companion object {
        private const val PREFS_NAME = "truckerload_auth_credentials_enc"
        private const val LEGACY_PREFS_NAME = "truckerload_auth_credentials"
        private const val MIGRATION_FLAG = "migrated_from_plain"
        private const val KEY_EMAIL = "email"
        private const val KEY_PASSWORD = "password"

        private fun openPrefs(context: Context): SharedPreferences {
            val secure = SecurePreferences.open(context, PREFS_NAME)
            SecurePreferences.migratePlainToSecure(
                context = context,
                legacyName = LEGACY_PREFS_NAME,
                securePrefs = secure,
                migrationFlagKey = MIGRATION_FLAG,
            )
            return secure
        }
    }
}

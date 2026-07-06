package com.truckerload.data.preferences

import androidx.core.content.edit
import android.content.Context
import android.content.SharedPreferences

/**
 * Локальное хранилище email/пароля для входа по почте.
 * Используется вместе с SignUpScreen и LoginEmailScreen.
 */
class AuthCredentialsStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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
        private const val PREFS_NAME = "truckerload_auth_credentials"
        private const val KEY_EMAIL = "email"
        private const val KEY_PASSWORD = "password"
    }
}

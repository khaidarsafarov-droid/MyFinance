package com.truckerload.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PREFS_NAME = "truckerload_auth"
private const val KEY_LOGGED_IN = "is_logged_in"
private const val DEFAULT_LOGGED_IN = false

class AuthStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean(KEY_LOGGED_IN, DEFAULT_LOGGED_IN))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun setLoggedIn(value: Boolean) {
        prefs.edit().putBoolean(KEY_LOGGED_IN, value).apply()
        _isLoggedIn.value = value
    }

    fun logout() {
        prefs.edit().remove(KEY_LOGGED_IN).apply()
        _isLoggedIn.value = false
    }

    /** @param rememberMe при true сессия сохраняется при закрытии приложения */
    fun login(rememberMe: Boolean = true) {
        _isLoggedIn.value = true
        if (rememberMe) {
            prefs.edit().putBoolean(KEY_LOGGED_IN, true).apply()
        }
    }
}

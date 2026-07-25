package com.truckerload.data.preferences

import android.content.Context
import androidx.core.content.edit

class PushTokenStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun get(): String? = prefs.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() }

    fun set(token: String) {
        if (token.isBlank()) return
        prefs.edit { putString(KEY_TOKEN, token) }
    }

    fun clear() {
        prefs.edit { remove(KEY_TOKEN) }
    }

    companion object {
        private const val PREFS_NAME = "truckerload_push"
        private const val KEY_TOKEN = "fcm_token"
    }
}

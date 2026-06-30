package com.truckerload.data.preferences

import android.content.Context
import com.truckerload.BuildConfig

class TelegramTokenStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getToken(): String {
        val saved = prefs.getString(KEY_TOKEN, null)?.trim().orEmpty()
        if (saved.isNotBlank()) return saved
        return BuildConfig.TELEGRAM_BOT_TOKEN.trim()
    }

    fun setToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token.trim()).apply()
    }

    fun hasToken(): Boolean = getToken().isNotBlank()

    /** После пересборки local.properties всегда попадает в BuildConfig — синхронизируем в prefs. */
    fun syncFromBuildConfig() {
        val fromBuild = BuildConfig.TELEGRAM_BOT_TOKEN.trim()
        if (fromBuild.isBlank()) return
        val saved = prefs.getString(KEY_TOKEN, null)?.trim().orEmpty()
        if (saved != fromBuild) {
            prefs.edit().putString(KEY_TOKEN, fromBuild).apply()
        }
    }

    companion object {
        private const val PREFS = "telegram_token"
        private const val KEY_TOKEN = "bot_token"
    }
}

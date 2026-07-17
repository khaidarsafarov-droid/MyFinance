package com.truckerload.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.truckerload.BuildConfig

class TelegramTokenStore(context: Context) {
    private val prefs: SharedPreferences = openPrefs(context)

    fun getToken(): String {
        val saved = prefs.getString(KEY_TOKEN, null)?.trim().orEmpty()
        if (saved.isNotBlank()) return saved
        return BuildConfig.TELEGRAM_BOT_TOKEN.trim()
    }

    fun setToken(token: String) {
        prefs.edit { putString(KEY_TOKEN, token.trim()) }
    }

    fun hasToken(): Boolean = getToken().isNotBlank()

    /**
     * One-time bootstrap from local.properties → encrypted prefs (dev builds only).
     * Does not overwrite a token the user already saved.
     */
    fun bootstrapFromBuildConfigIfEmpty() {
        val saved = prefs.getString(KEY_TOKEN, null)?.trim().orEmpty()
        if (saved.isNotBlank()) return
        val fromBuild = BuildConfig.TELEGRAM_BOT_TOKEN.trim()
        if (fromBuild.isNotBlank()) {
            prefs.edit { putString(KEY_TOKEN, fromBuild) }
        }
    }

    /** @deprecated Use [bootstrapFromBuildConfigIfEmpty]; kept for call-site compatibility. */
    fun syncFromBuildConfig() = bootstrapFromBuildConfigIfEmpty()

    companion object {
        private const val PREFS_NAME = "telegram_token_enc"
        private const val LEGACY_PREFS_NAME = "telegram_token"
        private const val MIGRATION_FLAG = "migrated_from_plain"
        private const val KEY_TOKEN = "bot_token"

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

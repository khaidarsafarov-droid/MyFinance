package com.truckerload.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.truckerload.BuildConfig

/**
 * Bot token storage scoped per account. Each user configures (or inherits bootstrap for)
 * their own Telegram bot so loads land in their own Room DB.
 */
class TelegramTokenStore(
    context: Context,
    userId: String? = AuthStore(context).currentUserIdOrNull(),
) {
    private val appContext = context.applicationContext
    private val resolvedUserId = userId?.trim()?.takeIf { it.isNotBlank() }
    private val prefs: SharedPreferences = openPrefs(appContext, resolvedUserId)

    fun getToken(): String {
        val saved = prefs.getString(KEY_TOKEN, null)?.trim().orEmpty()
        if (saved.isNotBlank()) return saved
        // Device bootstrap token only when an account is active (avoid writing into empty logout state).
        if (resolvedUserId != null) return BuildConfig.TELEGRAM_BOT_TOKEN.trim()
        return ""
    }

    fun setToken(token: String) {
        val trimmed = token.trim()
        if (trimmed.isNotBlank()) {
            SecurePreferences.requireEncryptedForSecretWrite("Telegram bot token")
        }
        prefs.edit { putString(KEY_TOKEN, trimmed) }
    }

    fun hasToken(): Boolean = getToken().isNotBlank()

    /**
     * True only when this account saved a token in encrypted prefs.
     * Unlike [hasToken], ignores the BuildConfig bootstrap fallback.
     */
    // FIX: onboarding must not treat a BuildConfig bootstrap token as "user configured a bot"
    fun hasPersistedToken(): Boolean = prefs.getString(KEY_TOKEN, null)?.trim().orEmpty().isNotBlank()

    fun clearToken() {
        prefs.edit { remove(KEY_TOKEN) }
    }

    /**
     * One-time bootstrap from local.properties → encrypted prefs (dev builds only).
     * Does not overwrite a token the user already saved.
     */
    fun bootstrapFromBuildConfigIfEmpty() {
        val saved = prefs.getString(KEY_TOKEN, null)?.trim().orEmpty()
        if (saved.isNotBlank()) return
        val fromBuild = BuildConfig.TELEGRAM_BOT_TOKEN.trim()
        if (fromBuild.isNotBlank()) {
            SecurePreferences.requireEncryptedForSecretWrite("Telegram bot token")
            prefs.edit { putString(KEY_TOKEN, fromBuild) }
        }
    }

    /** @deprecated Use [bootstrapFromBuildConfigIfEmpty]; kept for call-site compatibility. */
    fun syncFromBuildConfig() = bootstrapFromBuildConfigIfEmpty()

    companion object {
        private const val LEGACY_SECURE = "telegram_token_enc"
        private const val LEGACY_PLAIN = "telegram_token"
        private const val MIGRATION_FLAG = "migrated_from_plain"
        private const val KEY_TOKEN = "bot_token"
        private const val META_PREFS = "truckerload_account_meta"
        private const val KEY_LEGACY_TOKEN_MIGRATED = "legacy_telegram_token_migrated"

        private fun prefsName(userId: String?): String =
            if (userId.isNullOrBlank()) LEGACY_SECURE
            else "telegram_token_enc_${AccountIds.sanitizeFilePart(userId)}"

        private fun openPrefs(context: Context, userId: String?): SharedPreferences {
            val name = prefsName(userId)
            val secure = SecurePreferences.open(context, name)
            if (SecurePreferences.plaintextFallbackUsed) return secure
            if (userId.isNullOrBlank()) {
                SecurePreferences.migratePlainToSecure(
                    context = context,
                    legacyName = LEGACY_PLAIN,
                    securePrefs = secure,
                    migrationFlagKey = MIGRATION_FLAG,
                )
            } else {
                migrateLegacyTokenIfNeeded(context, userId, secure)
            }
            return secure
        }

        private fun migrateLegacyTokenIfNeeded(
            context: Context,
            userId: String,
            target: SharedPreferences,
        ) {
            val meta = context.getSharedPreferences(META_PREFS, Context.MODE_PRIVATE)
            if (meta.getBoolean(KEY_LEGACY_TOKEN_MIGRATED, false)) return
            if (!target.getString(KEY_TOKEN, null).isNullOrBlank()) {
                meta.edit { putBoolean(KEY_LEGACY_TOKEN_MIGRATED, true) }
                return
            }
            val legacy = SecurePreferences.open(context, LEGACY_SECURE)
            SecurePreferences.migratePlainToSecure(
                context = context,
                legacyName = LEGACY_PLAIN,
                securePrefs = legacy,
                migrationFlagKey = MIGRATION_FLAG,
            )
            val token = legacy.getString(KEY_TOKEN, null)?.trim().orEmpty()
            if (token.isNotBlank()) {
                if (SecurePreferences.plaintextFallbackUsed) return
                SecurePreferences.requireEncryptedForSecretWrite("Telegram bot token")
                target.edit { putString(KEY_TOKEN, token) }
            }
            meta.edit { putBoolean(KEY_LEGACY_TOKEN_MIGRATED, true) }
        }

        fun forActiveUser(context: Context): TelegramTokenStore? {
            val userId = AuthStore(context).currentUserIdOrNull() ?: return null
            return TelegramTokenStore(context, userId)
        }
    }
}

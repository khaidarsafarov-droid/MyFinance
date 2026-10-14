package com.truckerload.data.preferences

import android.content.Context
import androidx.core.content.edit
import com.truckerload.BuildConfig
import com.truckerload.data.local.AndroidKeystorePassphraseCipher
import com.truckerload.data.local.DatabasePassphraseStore
import com.truckerload.data.local.PassphraseCipher
import com.truckerload.data.local.SoftwarePassphraseCipher
import java.io.File

/**
 * Bot token storage scoped per account. The token is AES-GCM wrapped with a key
 * that stays in the Android Keystore. A one-time read of the old
 * EncryptedSharedPreferences (and the older plaintext prefs) copies an existing
 * token so an update does not disconnect the bot.
 */
class TelegramTokenStore(
    context: Context,
    userId: String? = AuthStore(context).currentUserIdOrNull(),
) {
    private val appContext = context.applicationContext
    private val resolvedUserId = userId?.trim()?.takeIf { it.isNotBlank() }
    private val storeName = prefsName(resolvedUserId)
    private val tokenFile = File(appContext.filesDir, "telegram-tokens/$storeName.bin")
    private val cipher: PassphraseCipher = cipherFor(appContext)

    fun getToken(): String {
        ensureMigrated()
        return readWrapped().trim()
    }

    fun setToken(token: String) {
        ensureMigrated()
        val trimmed = token.trim()
        if (trimmed.isBlank()) {
            tokenFile.delete()
            return
        }
        writeWrapped(trimmed)
    }

    fun hasToken(): Boolean = getToken().isNotBlank()

    /**
     * True only when this account saved a token.
     * Unlike [hasToken], ignores the BuildConfig bootstrap fallback.
     */
    fun hasPersistedToken(): Boolean = getToken().isNotBlank()

    fun clearToken() {
        ensureMigrated()
        tokenFile.delete()
    }

    /**
     * One-time bootstrap from local.properties → Keystore-wrapped file (dev builds only).
     * Does not overwrite a token the user already saved.
     */
    fun bootstrapFromBuildConfigIfEmpty() {
        if (getToken().isNotBlank()) return
        val fromBuild = BuildConfig.TELEGRAM_BOT_TOKEN.trim()
        if (fromBuild.isBlank()) return
        setToken(fromBuild)
    }

    /** @deprecated Use [bootstrapFromBuildConfigIfEmpty]; kept for call-site compatibility. */
    fun syncFromBuildConfig() = bootstrapFromBuildConfigIfEmpty()

    private fun ensureMigrated() {
        val meta = appContext.getSharedPreferences(META_PREFS, Context.MODE_PRIVATE)
        val flag = migratedFlag(storeName)
        if (meta.getBoolean(flag, false)) return
        val legacy = readLegacyToken()
        if (legacy.isNotBlank()) {
            writeWrapped(legacy)
            clearLegacyToken()
        }
        meta.edit { putBoolean(flag, true) }
    }

    private fun readLegacyToken(): String {
        val names = buildList {
            add(storeName)
            if (storeName != LEGACY_SECURE) add(LEGACY_SECURE)
        }
        for (name in names) {
            if (!legacyStoreExists(appContext, name)) continue
            val token = SecurePreferences.open(appContext, name)
                .getString(KEY_TOKEN, null)
                ?.trim()
                .orEmpty()
            if (token.isNotBlank()) return token
        }
        return appContext.getSharedPreferences(LEGACY_PLAIN, Context.MODE_PRIVATE)
            .getString(KEY_TOKEN, null)
            ?.trim()
            .orEmpty()
    }

    private fun clearLegacyToken() {
        val names = buildList {
            add(storeName)
            if (storeName != LEGACY_SECURE) add(LEGACY_SECURE)
        }
        for (name in names) {
            if (!legacyStoreExists(appContext, name)) continue
            SecurePreferences.open(appContext, name).edit { remove(KEY_TOKEN) }
        }
        appContext.getSharedPreferences(LEGACY_PLAIN, Context.MODE_PRIVATE).edit {
            remove(KEY_TOKEN)
        }
    }

    private fun readWrapped(): String {
        if (!tokenFile.exists() || tokenFile.length() == 0L) return ""
        return cipher.unwrap(tokenFile.readBytes()).toString(Charsets.UTF_8)
    }

    private fun writeWrapped(token: String) {
        tokenFile.parentFile?.mkdirs()
        val wrapped = cipher.wrap(token.toByteArray(Charsets.UTF_8))
        val tmp = File(tokenFile.path + ".tmp")
        tmp.writeBytes(wrapped)
        if (!tmp.renameTo(tokenFile)) {
            tokenFile.writeBytes(tmp.readBytes())
            tmp.delete()
        }
    }

    companion object {
        private const val LEGACY_SECURE = "telegram_token_enc"
        private const val LEGACY_PLAIN = "telegram_token"
        private const val KEY_TOKEN = "bot_token"
        private const val META_PREFS = "truckerload_account_meta"
        private const val KEYSTORE_ALIAS = "truckerload_telegram_token"

        private fun prefsName(userId: String?): String =
            if (userId.isNullOrBlank()) LEGACY_SECURE
            else "telegram_token_enc_${AccountIds.sanitizeFilePart(userId)}"

        private fun migratedFlag(storeName: String): String = "keystore_token_migrated_$storeName"

        private fun cipherFor(context: Context): PassphraseCipher =
            if (DatabasePassphraseStore.isRobolectric()) {
                SoftwarePassphraseCipher.forFile(
                    File(context.filesDir, "telegram-tokens/robolectric-wrap.key"),
                )
            } else {
                AndroidKeystorePassphraseCipher(KEYSTORE_ALIAS)
            }

        private fun legacyStoreExists(context: Context, name: String): Boolean {
            val dir = File(context.applicationInfo.dataDir, "shared_prefs")
            return File(dir, "$name.xml").exists() ||
                File(dir, "${SecurePreferences.fallbackName(name)}.xml").exists()
        }

        fun forActiveUser(context: Context): TelegramTokenStore? {
            val userId = AuthStore(context).currentUserIdOrNull() ?: return null
            return TelegramTokenStore(context, userId)
        }
    }
}

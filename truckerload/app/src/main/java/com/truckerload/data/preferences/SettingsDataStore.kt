package com.truckerload.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.truckerload.sync.TelegramPairingCodes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "truckerload_settings"
)

private val KEY_FIRST_RUN = booleanPreferencesKey("is_first_run")
private val KEY_TELEGRAM_CHAT_ID = longPreferencesKey("telegram_chat_id")
private val KEY_TELEGRAM_UPDATE_OFFSET = longPreferencesKey("telegram_last_update_offset")
private val KEY_THEME_MODE = intPreferencesKey("app_theme_mode")
private val KEY_LANGUAGE = intPreferencesKey("app_language")
private val KEY_PARSER_AUTO_UPDATE = booleanPreferencesKey("parser_auto_update")
private val KEY_PARSER_PRICE_THRESHOLD = floatPreferencesKey("parser_price_threshold_percent")
private val KEY_SHARE_PATH_WITH_FRIENDS = booleanPreferencesKey("share_path_with_friends")
private val KEY_TELEGRAM_PAIR_CODE = stringPreferencesKey("telegram_pair_code")
private val KEY_TELEGRAM_PAIR_EXPIRES = longPreferencesKey("telegram_pair_expires_at")

class SettingsDataStore(context: Context) {

    private val appContext = context.applicationContext

    companion object {
        private const val SYNC_PREFS_NAME = "truckerload_settings_sync"
        private const val KEY_LANGUAGE_TAG = "app_language_tag"

        fun mirrorLanguageTag(context: Context, tag: String) {
            context.applicationContext
                .getSharedPreferences(SYNC_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LANGUAGE_TAG, tag)
                .apply()
        }

        fun readStoredLanguage(context: Context): AppLanguage {
            val tag = context.applicationContext
                .getSharedPreferences(SYNC_PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LANGUAGE_TAG, null)
            return AppLanguage.entries.firstOrNull { it.tag == tag } ?: AppLanguage.RU
        }
    }

    val isFirstRun: Flow<Boolean> = appContext.settingsDataStore.data.map { prefs ->
        prefs[KEY_FIRST_RUN] ?: true
    }

    val telegramChatId: Flow<Long?> = appContext.settingsDataStore.data.map { prefs ->
        prefs[KEY_TELEGRAM_CHAT_ID]
    }

    val themeMode: Flow<AppThemeMode> = appContext.settingsDataStore.data.map { prefs ->
        AppThemeMode.fromOrdinal(prefs[KEY_THEME_MODE] ?: AppThemeMode.SYSTEM.ordinal)
    }

    val language: Flow<AppLanguage> = appContext.settingsDataStore.data.map { prefs ->
        AppLanguage.fromOrdinal(prefs[KEY_LANGUAGE] ?: AppLanguage.RU.ordinal)
    }

    val parserAutoUpdate: Flow<Boolean> = appContext.settingsDataStore.data.map { prefs ->
        prefs[KEY_PARSER_AUTO_UPDATE] ?: true
    }

    val parserPriceThreshold: Flow<Double> = appContext.settingsDataStore.data.map { prefs ->
        prefs[KEY_PARSER_PRICE_THRESHOLD]?.toDouble() ?: 1.0
    }

    val sharePathWithFriends: Flow<Boolean> = appContext.settingsDataStore.data.map { prefs ->
        prefs[KEY_SHARE_PATH_WITH_FRIENDS] ?: false
    }

    suspend fun isFirstRunOnce(): Boolean = isFirstRun.first()

    suspend fun getTelegramChatIdOnce(): Long? {
        val userId = AuthStore(appContext).currentUserIdOrNull()
        if (userId != null) {
            val key = longPreferencesKey("telegram_chat_id_${AccountIds.sanitizeFilePart(userId)}")
            val prefs = appContext.settingsDataStore.data.first()
            prefs[key]?.let { return it }
            // One-shot migrate global chat id into this account.
            val legacy = prefs[KEY_TELEGRAM_CHAT_ID] ?: return null
            saveTelegramChatId(legacy)
            return legacy
        }
        return telegramChatId.first()
    }

    suspend fun markFirstRunComplete() {
        appContext.settingsDataStore.edit { prefs ->
            prefs[KEY_FIRST_RUN] = false
        }
    }

    suspend fun saveTelegramChatId(chatId: Long) {
        val userId = AuthStore(appContext).currentUserIdOrNull()
        appContext.settingsDataStore.edit { prefs ->
            if (userId != null) {
                prefs[longPreferencesKey("telegram_chat_id_${AccountIds.sanitizeFilePart(userId)}")] = chatId
            } else {
                prefs[KEY_TELEGRAM_CHAT_ID] = chatId
            }
        }
    }

    suspend fun clearTelegramChatId() {
        val userId = AuthStore(appContext).currentUserIdOrNull()
        appContext.settingsDataStore.edit { prefs ->
            if (userId != null) {
                prefs.remove(longPreferencesKey("telegram_chat_id_${AccountIds.sanitizeFilePart(userId)}"))
            }
            prefs.remove(KEY_TELEGRAM_CHAT_ID)
        }
    }

    private fun pairCodeKey(userId: String?): Preferences.Key<String> =
        if (userId != null) {
            stringPreferencesKey("telegram_pair_code_${AccountIds.sanitizeFilePart(userId)}")
        } else {
            KEY_TELEGRAM_PAIR_CODE
        }

    private fun pairExpiresKey(userId: String?): Preferences.Key<Long> =
        if (userId != null) {
            longPreferencesKey("telegram_pair_expires_${AccountIds.sanitizeFilePart(userId)}")
        } else {
            KEY_TELEGRAM_PAIR_EXPIRES
        }

    /**
     * Issues a fresh 6-digit OTP for Telegram chat pairing (TTL [TelegramPairingCodes.TTL_MS]).
     */
    suspend fun issueTelegramPairingCode(): Pair<String, Long> {
        val userId = AuthStore(appContext).currentUserIdOrNull()
        val code = TelegramPairingCodes.generate()
        val expiresAt = System.currentTimeMillis() + TelegramPairingCodes.TTL_MS
        appContext.settingsDataStore.edit { prefs ->
            prefs[pairCodeKey(userId)] = code
            prefs[pairExpiresKey(userId)] = expiresAt
        }
        return code to expiresAt
    }

    suspend fun getTelegramPairingCodeOnce(): Pair<String, Long>? {
        val userId = AuthStore(appContext).currentUserIdOrNull()
        val prefs = appContext.settingsDataStore.data.first()
        val code = prefs[pairCodeKey(userId)] ?: return null
        val expires = prefs[pairExpiresKey(userId)] ?: return null
        return code to expires
    }

    suspend fun clearTelegramPairingCode() {
        val userId = AuthStore(appContext).currentUserIdOrNull()
        appContext.settingsDataStore.edit { prefs ->
            prefs.remove(pairCodeKey(userId))
            prefs.remove(pairExpiresKey(userId))
        }
    }

    /** Next offset for getUpdates (last processed update_id + 1). Scoped per active account. */
    suspend fun getLastUpdateOffset(): Long {
        val userId = AuthStore(appContext).currentUserIdOrNull()
        if (userId != null) {
            val key = longPreferencesKey("telegram_last_update_offset_${AccountIds.sanitizeFilePart(userId)}")
            val scoped = appContext.settingsDataStore.data.first()[key]
            if (scoped != null) return scoped
        }
        return appContext.settingsDataStore.data.first()[KEY_TELEGRAM_UPDATE_OFFSET] ?: 0L
    }

    suspend fun saveLastUpdateOffset(offset: Long) {
        val safe = offset.coerceAtLeast(0L)
        val userId = AuthStore(appContext).currentUserIdOrNull()
        appContext.settingsDataStore.edit { prefs ->
            if (userId != null) {
                prefs[longPreferencesKey("telegram_last_update_offset_${AccountIds.sanitizeFilePart(userId)}")] = safe
            } else {
                prefs[KEY_TELEGRAM_UPDATE_OFFSET] = safe
            }
        }
    }

    suspend fun getThemeModeOnce(): AppThemeMode = themeMode.first()

    suspend fun getLanguageOnce(): AppLanguage = language.first()

    suspend fun saveLanguage(language: AppLanguage) {
        appContext.settingsDataStore.edit { prefs ->
            prefs[KEY_LANGUAGE] = language.ordinal
        }
        mirrorLanguageTag(appContext, language.tag)
    }

    suspend fun saveThemeMode(mode: AppThemeMode) {
        appContext.settingsDataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.ordinal
        }
    }

    suspend fun getParserAutoUpdateOnce(): Boolean = parserAutoUpdate.first()

    suspend fun getParserPriceThresholdOnce(): Double = parserPriceThreshold.first()

    suspend fun saveParserAutoUpdate(enabled: Boolean) {
        appContext.settingsDataStore.edit { prefs ->
            prefs[KEY_PARSER_AUTO_UPDATE] = enabled
        }
    }

    suspend fun saveParserPriceThreshold(percent: Double) {
        appContext.settingsDataStore.edit { prefs ->
            prefs[KEY_PARSER_PRICE_THRESHOLD] = percent.toFloat()
        }
    }

    suspend fun getSharePathWithFriendsOnce(): Boolean = sharePathWithFriends.first()

    suspend fun saveSharePathWithFriends(enabled: Boolean) {
        appContext.settingsDataStore.edit { prefs ->
            prefs[KEY_SHARE_PATH_WITH_FRIENDS] = enabled
        }
    }
}

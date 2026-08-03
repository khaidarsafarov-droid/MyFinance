package com.truckerload.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "truckerload_settings"
)

private val KEY_FIRST_RUN = booleanPreferencesKey("is_first_run")
private val KEY_TELEGRAM_CHAT_ID = longPreferencesKey("telegram_chat_id")
private val KEY_TELEGRAM_CHAT_ID_LEGACY_CLAIMED = booleanPreferencesKey("telegram_chat_id_legacy_claimed")
private val KEY_TELEGRAM_UPDATE_OFFSET = longPreferencesKey("telegram_last_update_offset")
private val KEY_THEME_MODE = intPreferencesKey("app_theme_mode")
private val KEY_LANGUAGE = intPreferencesKey("app_language")
private val KEY_PARSER_AUTO_UPDATE = booleanPreferencesKey("parser_auto_update")
private val KEY_PARSER_PRICE_THRESHOLD = floatPreferencesKey("parser_price_threshold_percent")
private val KEY_SHARE_PATH_WITH_FRIENDS = booleanPreferencesKey("share_path_with_friends")
private val KEY_REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
private val KEY_OLED_DARK = booleanPreferencesKey("oled_dark")
private val KEY_QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
private val KEY_QUIET_HOURS_START = intPreferencesKey("quiet_hours_start")
private val KEY_QUIET_HOURS_END = intPreferencesKey("quiet_hours_end")
private val KEY_NOTIFY_MISSING_WEEK = booleanPreferencesKey("notify_missing_week")
private val KEY_NOTIFY_MAINTENANCE = booleanPreferencesKey("notify_maintenance")

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

    private fun accountPart(): String? =
        AuthStore(appContext).currentUserIdOrNull()?.let(AccountIds::sanitizeFilePart)

    private fun boolKey(base: String, account: String?) =
        booleanPreferencesKey(if (account != null) "${base}_$account" else base)

    private fun intKey(base: String, account: String?) =
        intPreferencesKey(if (account != null) "${base}_$account" else base)

    private fun floatKey(base: String, account: String?) =
        floatPreferencesKey(if (account != null) "${base}_$account" else base)

    val isFirstRun: Flow<Boolean> = appContext.settingsDataStore.data.map { prefs ->
        prefs[KEY_FIRST_RUN] ?: true
    }

    val telegramChatId: Flow<Long?> = appContext.settingsDataStore.data.map { prefs ->
        prefs[KEY_TELEGRAM_CHAT_ID]
    }

    // Device-global UX
    val themeMode: Flow<AppThemeMode> = appContext.settingsDataStore.data.map { prefs ->
        AppThemeMode.fromOrdinal(prefs[KEY_THEME_MODE] ?: AppThemeMode.SYSTEM.ordinal)
    }

    val language: Flow<AppLanguage> = appContext.settingsDataStore.data.map { prefs ->
        AppLanguage.fromOrdinal(prefs[KEY_LANGUAGE] ?: AppLanguage.RU.ordinal)
    }

    val reduceMotion: Flow<Boolean> = appContext.settingsDataStore.data.map { prefs ->
        prefs[KEY_REDUCE_MOTION] ?: false
    }

    val oledDark: Flow<Boolean> = appContext.settingsDataStore.data.map { prefs ->
        prefs[KEY_OLED_DARK] ?: false
    }

    // FIX Stage3: account-scoped parser / quiet hours / notify / share-path
    val parserAutoUpdate: Flow<Boolean> = appContext.settingsDataStore.data.map { prefs ->
        accountScopedBool(prefs, "parser_auto_update", KEY_PARSER_AUTO_UPDATE, default = true)
    }

    val parserPriceThreshold: Flow<Double> = appContext.settingsDataStore.data.map { prefs ->
        val account = accountPart()
        val scoped = prefs[floatKey("parser_price_threshold_percent", account)]?.toDouble()
        scoped ?: prefs[KEY_PARSER_PRICE_THRESHOLD]?.toDouble() ?: 1.0
    }

    val sharePathWithFriends: Flow<Boolean> = appContext.settingsDataStore.data.map { prefs ->
        val account = accountPart()
        if (account != null) {
            prefs[boolKey("share_path_with_friends", account)] ?: false
        } else {
            prefs[KEY_SHARE_PATH_WITH_FRIENDS] ?: false
        }
    }

    val quietHoursEnabled: Flow<Boolean> = appContext.settingsDataStore.data.map { prefs ->
        accountScopedBool(prefs, "quiet_hours_enabled", KEY_QUIET_HOURS_ENABLED, default = false)
    }

    val quietHoursStart: Flow<Int> = appContext.settingsDataStore.data.map { prefs ->
        accountScopedInt(prefs, "quiet_hours_start", KEY_QUIET_HOURS_START, default = 22)
    }

    val quietHoursEnd: Flow<Int> = appContext.settingsDataStore.data.map { prefs ->
        accountScopedInt(prefs, "quiet_hours_end", KEY_QUIET_HOURS_END, default = 7)
    }

    val notifyMissingWeek: Flow<Boolean> = appContext.settingsDataStore.data.map { prefs ->
        accountScopedBool(prefs, "notify_missing_week", KEY_NOTIFY_MISSING_WEEK, default = true)
    }

    val notifyMaintenance: Flow<Boolean> = appContext.settingsDataStore.data.map { prefs ->
        accountScopedBool(prefs, "notify_maintenance", KEY_NOTIFY_MAINTENANCE, default = true)
    }

    private fun accountScopedBool(
        prefs: Preferences,
        base: String,
        legacy: Preferences.Key<Boolean>,
        default: Boolean,
    ): Boolean {
        val account = accountPart()
        if (account != null) {
            prefs[boolKey(base, account)]?.let { return it }
            // One-shot read of legacy global value (not for share_path privacy).
            return prefs[legacy] ?: default
        }
        return prefs[legacy] ?: default
    }

    private fun accountScopedInt(
        prefs: Preferences,
        base: String,
        legacy: Preferences.Key<Int>,
        default: Int,
    ): Int {
        val account = accountPart()
        if (account != null) {
            prefs[intKey(base, account)]?.let { return it }
            return prefs[legacy] ?: default
        }
        return prefs[legacy] ?: default
    }

    suspend fun isFirstRunOnce(): Boolean = isFirstRun.first()

    suspend fun getTelegramChatIdOnce(): Long? {
        val userId = AuthStore(appContext).currentUserIdOrNull()
        if (userId != null) {
            val key = longPreferencesKey("telegram_chat_id_${AccountIds.sanitizeFilePart(userId)}")
            val prefs = appContext.settingsDataStore.data.first()
            prefs[key]?.let { return it }
            // FIX: one-shot migrate — clear global so later accounts do not inherit the same chat
            if (prefs[KEY_TELEGRAM_CHAT_ID_LEGACY_CLAIMED] == true) return null
            val legacy = prefs[KEY_TELEGRAM_CHAT_ID] ?: return null
            appContext.settingsDataStore.edit { editPrefs ->
                editPrefs[key] = legacy
                editPrefs.remove(KEY_TELEGRAM_CHAT_ID)
                editPrefs[KEY_TELEGRAM_CHAT_ID_LEGACY_CLAIMED] = true
            }
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

    /** Next offset for getUpdates (last processed update_id + 1). Scoped per active account. */
    suspend fun getLastUpdateOffset(): Long {
        val userId = AuthStore(appContext).currentUserIdOrNull()
        if (userId != null) {
            val key = longPreferencesKey("telegram_last_update_offset_${AccountIds.sanitizeFilePart(userId)}")
            val scoped = appContext.settingsDataStore.data.first()[key]
            // FIX: never fall back to global offset — that skipped updates for new accounts
            return scoped ?: 0L
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
        val account = accountPart()
        appContext.settingsDataStore.edit { prefs ->
            prefs[boolKey("parser_auto_update", account)] = enabled
        }
    }

    suspend fun saveParserPriceThreshold(percent: Double) {
        val account = accountPart()
        appContext.settingsDataStore.edit { prefs ->
            prefs[floatKey("parser_price_threshold_percent", account)] = percent.toFloat()
        }
    }

    suspend fun getSharePathWithFriendsOnce(): Boolean = sharePathWithFriends.first()

    suspend fun saveSharePathWithFriends(enabled: Boolean) {
        val account = accountPart()
        appContext.settingsDataStore.edit { prefs ->
            // Never migrate global true into a new account — privacy default is off.
            prefs[boolKey("share_path_with_friends", account)] = enabled
        }
    }

    suspend fun getReduceMotionOnce(): Boolean = reduceMotion.first()

    suspend fun saveReduceMotion(enabled: Boolean) {
        appContext.settingsDataStore.edit { prefs ->
            prefs[KEY_REDUCE_MOTION] = enabled
        }
    }

    suspend fun getOledDarkOnce(): Boolean = oledDark.first()

    suspend fun saveOledDark(enabled: Boolean) {
        appContext.settingsDataStore.edit { prefs ->
            prefs[KEY_OLED_DARK] = enabled
        }
    }

    suspend fun getQuietHoursEnabledOnce(): Boolean = quietHoursEnabled.first()

    suspend fun saveQuietHoursEnabled(enabled: Boolean) {
        val account = accountPart()
        appContext.settingsDataStore.edit { prefs ->
            prefs[boolKey("quiet_hours_enabled", account)] = enabled
        }
    }

    suspend fun getQuietHoursStartOnce(): Int = quietHoursStart.first()

    suspend fun saveQuietHoursStart(hour: Int) {
        val account = accountPart()
        appContext.settingsDataStore.edit { prefs ->
            prefs[intKey("quiet_hours_start", account)] = hour.coerceIn(0, 23)
        }
    }

    suspend fun getQuietHoursEndOnce(): Int = quietHoursEnd.first()

    suspend fun saveQuietHoursEnd(hour: Int) {
        val account = accountPart()
        appContext.settingsDataStore.edit { prefs ->
            prefs[intKey("quiet_hours_end", account)] = hour.coerceIn(0, 23)
        }
    }

    suspend fun getNotifyMissingWeekOnce(): Boolean = notifyMissingWeek.first()

    suspend fun saveNotifyMissingWeek(enabled: Boolean) {
        val account = accountPart()
        appContext.settingsDataStore.edit { prefs ->
            prefs[boolKey("notify_missing_week", account)] = enabled
        }
    }

    suspend fun getNotifyMaintenanceOnce(): Boolean = notifyMaintenance.first()

    suspend fun saveNotifyMaintenance(enabled: Boolean) {
        val account = accountPart()
        appContext.settingsDataStore.edit { prefs ->
            prefs[boolKey("notify_maintenance", account)] = enabled
        }
    }
}

package com.truckerload.sync.telegram

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.truckerload.data.preferences.AccountIds
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.sync.TelegramSyncWorker

/**
 * Persists Telegram getUpdates offset (per-user prefs + SettingsDataStore).
 * WorkManager scheduling lives in [com.truckerload.sync.TelegramSyncWorker].
 */
class TelegramSyncScheduler(
    private val context: Context,
) {
    fun prefsForUser(userId: String): SharedPreferences = telegramSyncPrefs(context, userId)

    suspend fun loadNextRequestOffset(
        prefs: SharedPreferences,
        settingsDataStore: SettingsDataStore,
    ): Long {
        val fromDataStore = settingsDataStore.getLastUpdateOffset()
        val fromPrefs = prefs.getLong(TelegramSyncWorker.KEY_LAST_OFFSET, 0L)
        return maxOf(fromDataStore, fromPrefs)
    }

    suspend fun persistNextRequestOffset(
        prefs: SharedPreferences,
        settingsDataStore: SettingsDataStore,
        offset: Long,
    ) {
        prefs.edit(commit = true) { putLong(TelegramSyncWorker.KEY_LAST_OFFSET, offset) }
        settingsDataStore.saveLastUpdateOffset(offset)
    }

    fun nextDelaySeconds(processed: Int, updatesNonEmpty: Boolean): Long =
        delayAfterPoll(processed, updatesNonEmpty)

    companion object {
        /** After a saved load, poll again immediately so the next Telegram message is not queued. */
        fun delayAfterPoll(processed: Int, updatesNonEmpty: Boolean): Long = when {
            processed > 0 -> 0L
            updatesNonEmpty -> 1L
            else -> 2L
        }

        fun telegramSyncPrefs(context: Context, userId: String): SharedPreferences {
            val name = "telegram_sync_${AccountIds.sanitizeFilePart(userId)}"
            val scoped = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            val meta = context.getSharedPreferences("truckerload_account_meta", Context.MODE_PRIVATE)
            // FIX: per-user migrate flag — device-wide flag made only the first account inherit legacy offset
            val migrateKey = "legacy_telegram_offset_migrated_${AccountIds.sanitizeFilePart(userId)}"
            val migrated = meta.getBoolean(migrateKey, false)
            if (!migrated && !scoped.contains(TelegramSyncWorker.KEY_LAST_OFFSET)) {
                val legacy = context.getSharedPreferences(TelegramSyncWorker.PREFS_NAME, Context.MODE_PRIVATE)
                val offset = legacy.getLong(TelegramSyncWorker.KEY_LAST_OFFSET, 0L)
                if (offset > 0L && !meta.getBoolean("legacy_telegram_offset_claimed", false)) {
                    scoped.edit(commit = true) {
                        putLong(TelegramSyncWorker.KEY_LAST_OFFSET, offset)
                    }
                    meta.edit().putBoolean("legacy_telegram_offset_claimed", true).apply()
                }
                meta.edit().putBoolean(migrateKey, true).apply()
            }
            return scoped
        }
    }
}

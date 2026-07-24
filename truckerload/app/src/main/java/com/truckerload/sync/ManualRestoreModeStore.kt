package com.truckerload.sync

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit

/**
 * Persists per-chat manual restore session state for Telegram restore mode.
 */
class ManualRestoreModeStore(
    private val prefs: SharedPreferences,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {

    fun start(chatId: String) {
        val now = nowMillis()
        prefs.edit {
            putBoolean(modeKey(chatId), true)
            putInt(countKey(chatId), 0)
            putLong(lastActivityKey(chatId), now)
        }
        Log.d(TAG, "Manual restore mode ON for chat $chatId")
    }

    fun touch(chatId: String) {
        prefs.edit { putLong(lastActivityKey(chatId), nowMillis()) }
    }

    fun isActive(chatId: String): Boolean {
        if (!prefs.getBoolean(modeKey(chatId), false)) return false
        val lastActivity = prefs.getLong(lastActivityKey(chatId), 0L)
        if (lastActivity == 0L) return true
        val expired = nowMillis() - lastActivity > TelegramSyncWorker.MANUAL_RESTORE_TIMEOUT_MS
        if (expired) {
            clear(chatId)
            return false
        }
        return true
    }

    fun clear(chatId: String) {
        prefs.edit {
            remove(modeKey(chatId))
            remove(countKey(chatId))
            remove(lastActivityKey(chatId))
        }
        Log.d(TAG, "Manual restore mode OFF for chat $chatId")
    }

    fun incrementCount(chatId: String): Int {
        val next = prefs.getInt(countKey(chatId), 0) + 1
        prefs.edit { putInt(countKey(chatId), next) }
        return next
    }

    private fun modeKey(chatId: String) = "${TelegramSyncWorker.KEY_MANUAL_RESTORE_PREFIX}$chatId"

    private fun countKey(chatId: String) = "${TelegramSyncWorker.KEY_MANUAL_RESTORE_COUNT_PREFIX}$chatId"

    private fun lastActivityKey(chatId: String) =
        "${TelegramSyncWorker.KEY_MANUAL_RESTORE_LAST_ACTIVITY_PREFIX}$chatId"

    private companion object {
        private const val TAG = "BackupRestore"
    }
}

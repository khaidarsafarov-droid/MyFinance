package com.truckerload.sync.import

import androidx.core.content.edit
import android.content.SharedPreferences
import com.truckerload.sync.TelegramSyncWorker

data class ImportSession(
    val chatId: String,
    val startedAt: Long,
)

class ImportSessionManager(private val prefs: SharedPreferences) {

    fun startSession(chatId: String) {
        val now = System.currentTimeMillis()
        prefs.edit {
            putBoolean(importModeKey(chatId), true)
            putLong(importLastActivityKey(chatId), now)
            putInt(importFilesKey(chatId), 0)
        }
    }

    fun isActive(chatId: String): Boolean {
        if (!prefs.getBoolean(importModeKey(chatId), false)) return false
        val lastActivity = prefs.getLong(importLastActivityKey(chatId), 0L)
        if (lastActivity == 0L) return true
        val expired = System.currentTimeMillis() - lastActivity > SESSION_TIMEOUT_MS
        if (expired) {
            endSession(chatId)
            return false
        }
        return true
    }

    fun touchActivity(chatId: String) {
        prefs.edit {putLong(importLastActivityKey(chatId), System.currentTimeMillis())}
    }

    fun endSession(chatId: String) {
        prefs.edit {
            remove(importModeKey(chatId))
            remove(importLastActivityKey(chatId))
            remove(importFilesKey(chatId))
        }
    }

    fun incrementFilesProcessed(chatId: String): Int {
        val next = getFilesProcessed(chatId) + 1
        prefs.edit {putInt(importFilesKey(chatId), next)}
        return next
    }

    fun getFilesProcessed(chatId: String): Int =
        prefs.getInt(importFilesKey(chatId), 0)

    fun cancelSession(chatId: String): Boolean {
        val had = prefs.getBoolean(importModeKey(chatId), false)
        endSession(chatId)
        return had
    }

    private fun importModeKey(chatId: String) =
        "${TelegramSyncWorker.KEY_IMPORT_MODE_PREFIX}$chatId"

    private fun importLastActivityKey(chatId: String) =
        "${TelegramSyncWorker.KEY_IMPORT_LAST_ACTIVITY_PREFIX}$chatId"

    private fun importFilesKey(chatId: String) =
        "${TelegramSyncWorker.KEY_IMPORT_FILES_PREFIX}$chatId"

    companion object {
        const val SESSION_TIMEOUT_MS = 300_000L
    }
}

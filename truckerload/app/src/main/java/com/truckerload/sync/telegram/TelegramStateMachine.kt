package com.truckerload.sync.telegram

import android.content.SharedPreferences
import com.truckerload.sync.ManualRestoreModeStore

/**
 * Per-chat sync session modes (manual restore). Import sessions stay in [ImportSessionManager].
 */
class TelegramStateMachine(
    prefs: SharedPreferences,
) {
    private val restoreStore = ManualRestoreModeStore(prefs)

    fun startManualRestore(chatId: String) = restoreStore.start(chatId)

    fun touchManualRestore(chatId: String) = restoreStore.touch(chatId)

    fun isManualRestoreActive(chatId: String): Boolean = restoreStore.isActive(chatId)

    fun clearManualRestore(chatId: String) = restoreStore.clear(chatId)

    fun incrementManualRestoreCount(chatId: String): Int = restoreStore.incrementCount(chatId)
}

data class TelegramSyncRunResult(
    val skipped: Boolean,
    val processedUpdates: Int,
    val nextDelaySeconds: Long,
    val error: String? = null,
)

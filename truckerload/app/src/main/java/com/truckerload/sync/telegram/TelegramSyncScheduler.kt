package com.truckerload.sync.telegram

import android.content.SharedPreferences
import androidx.core.content.edit
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.sync.TelegramSyncPolicy
import com.truckerload.sync.TelegramSyncWorker

/**
 * Offset persistence, poll spacing, and failure backoff for Telegram long-poll.
 */
class TelegramSyncScheduler {
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

    fun nextDelayAfterPoll(processedUpdates: Int, receivedUpdates: Int): Long = when {
        processedUpdates > 0 -> 1L
        receivedUpdates > 0 -> 1L
        else -> 2L
    }

    /** Delay after getUpdates failure (conflict / network). */
    fun delayAfterGetUpdatesFailure(errorMessage: String?): Long {
        val conflict = errorMessage?.contains("409") == true
        return when {
            conflict -> 45L
            TelegramSyncPolicy.isRetryable(errorMessage) -> 30L
            else -> 30L
        }
    }

    fun backoffMs(attemptIndex: Int): Long =
        TelegramSyncPolicy.backoffDelayMs(attemptIndex)
}

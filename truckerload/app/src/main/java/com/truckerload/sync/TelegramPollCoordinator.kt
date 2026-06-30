package com.truckerload.sync

import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Гарантирует один активный long-poll к Telegram (иначе API 409 Conflict).
 */
object TelegramPollCoordinator {
    private const val TAG = "TelegramPollCoord"
    private val mutex = Mutex()
    @Volatile
    private var foregroundPolling = false

    fun markForegroundPolling(active: Boolean) {
        foregroundPolling = active
    }

    fun isForegroundPolling(): Boolean = foregroundPolling

    suspend fun <T> withPollLock(block: suspend () -> T): T? {
        if (!mutex.tryLock()) {
            Log.d(TAG, "skip: another poll in progress")
            return null
        }
        return try {
            block()
        } finally {
            mutex.unlock()
        }
    }
}

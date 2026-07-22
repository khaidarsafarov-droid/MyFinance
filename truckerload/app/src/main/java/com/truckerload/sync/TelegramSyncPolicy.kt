package com.truckerload.sync

/**
 * Pure reconnect / backoff policy for Telegram long-poll.
 */
object TelegramSyncPolicy {
    const val DEFAULT_MAX_ATTEMPTS = 5
    const val DEFAULT_BASE_DELAY_MS = 1_000L
    const val DEFAULT_MAX_DELAY_MS = 30_000L

    fun backoffDelayMs(attemptIndex: Int, baseDelayMs: Long = DEFAULT_BASE_DELAY_MS): Long {
        val exp = baseDelayMs * (1L shl attemptIndex.coerceIn(0, 8))
        return exp.coerceAtMost(DEFAULT_MAX_DELAY_MS)
    }

    fun isRetryable(errorMessage: String?): Boolean {
        val m = errorMessage.orEmpty().lowercase()
        if (m.contains("401") || m.contains("unauthorized") || m.contains("token")) return false
        return m.contains("timeout") ||
            m.contains("connection") ||
            m.contains("unable to resolve") ||
            m.contains("503") ||
            m.contains("502") ||
            m.contains("429") ||
            m.contains("failed") ||
            m.contains("reset")
    }

    fun shouldResetOffset(conflictDetected: Boolean, unauthorized: Boolean): Boolean =
        conflictDetected && !unauthorized
}

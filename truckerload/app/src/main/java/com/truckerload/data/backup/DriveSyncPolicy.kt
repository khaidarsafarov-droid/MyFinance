package com.truckerload.data.backup

/**
 * Pure sync policy helpers (retry/conflict) — no Android/Drive SDK.
 */
object DriveSyncPolicy {
    const val DEFAULT_MAX_ATTEMPTS = 3
    const val DEFAULT_BASE_DELAY_MS = 400L
    /** Ignore tiny clock skew when comparing remote modifiedTime vs last sync. */
    const val DEFAULT_SKEW_MS = 2_000L

    fun backoffDelayMs(attemptIndex: Int, baseDelayMs: Long = DEFAULT_BASE_DELAY_MS): Long =
        baseDelayMs * (attemptIndex + 1).coerceAtLeast(1)

    fun isRetryableFailure(message: String?): Boolean {
        val m = message.orEmpty().lowercase()
        if (m.contains("not signed") || m.contains("consent") || m.contains("401")) return false
        return m.contains("timeout") ||
            m.contains("503") ||
            m.contains("429") ||
            m.contains("500") ||
            m.contains("502") ||
            m.contains("connection") ||
            m.contains("unable to resolve") ||
            m.contains("failed")
    }

    /**
     * Warn before restore when the device may have newer unsynced edits AND Drive
     * has a newer file than our last successful push.
     */
    fun shouldWarnBeforeRestore(
        localChangedAfterLastSync: Boolean,
        remoteModifiedAt: Long,
        lastSyncAt: Long,
        skewMs: Long = DEFAULT_SKEW_MS,
    ): Boolean {
        if (!localChangedAfterLastSync) return false
        if (lastSyncAt <= 0L || remoteModifiedAt <= 0L) return localChangedAfterLastSync
        return remoteModifiedAt > lastSyncAt + skewMs
    }

    fun remoteIsNewer(remoteModifiedAt: Long, lastSyncAt: Long, skewMs: Long = DEFAULT_SKEW_MS): Boolean =
        remoteModifiedAt > 0L && lastSyncAt > 0L && remoteModifiedAt > lastSyncAt + skewMs
}

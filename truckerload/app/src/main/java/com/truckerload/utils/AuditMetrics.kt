package com.truckerload.utils

import android.util.Log

/**
 * Lightweight structured audit counters for production monitoring (logcat / Crashlytics keys).
 */
object AuditMetrics {
    private const val TAG = "TlAudit"

    fun telegramUnauthorized() = emit("telegram_unauthorized")
    fun telegramPairNeedCode() = emit("telegram_pair_need_code")
    fun telegramPairBadCode() = emit("telegram_pair_bad_code")
    fun telegramPaired() = emit("telegram_paired")
    fun cdcDuplicateBatch() = emit("cdc_duplicate_batch")
    fun cdcStopsSkippedForIgnoredLoad() = emit("cdc_stops_skipped_ignored_load")
    fun friendsShareCleared() = emit("friends_share_cleared")

    private fun emit(event: String) {
        Log.i(TAG, "event=$event")
    }
}

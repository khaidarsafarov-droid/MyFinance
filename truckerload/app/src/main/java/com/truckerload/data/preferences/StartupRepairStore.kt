package com.truckerload.data.preferences

import android.content.Context
import androidx.core.content.edit

/**
 * Per-account flags for one-shot Room data repairs / startup backfill.
 * Success is recorded only after every step completes without error.
 */
class StartupRepairStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(META_PREFS, Context.MODE_PRIVATE)

    fun isBackfillDone(userId: String): Boolean {
        val id = userId.trim()
        if (id.isBlank()) return false
        return prefs.getBoolean(backfillKey(id), false)
    }

    fun markBackfillDone(userId: String) {
        val id = userId.trim()
        if (id.isBlank()) return
        prefs.edit {
            putBoolean(backfillKey(id), true)
            remove(needsRetryKey(id))
        }
    }

    fun markBackfillNeedsRetry(userId: String) {
        val id = userId.trim()
        if (id.isBlank()) return
        prefs.edit { putBoolean(needsRetryKey(id), true) }
    }

    fun clearBackfillNeedsRetry(userId: String) {
        val id = userId.trim()
        if (id.isBlank()) return
        prefs.edit { remove(needsRetryKey(id)) }
    }

    fun needsBackfillRetry(userId: String?): Boolean {
        val id = userId?.trim().orEmpty()
        if (id.isBlank()) return false
        return prefs.getBoolean(needsRetryKey(id), false)
    }

    companion object {
        private const val META_PREFS = "truckerload_app_meta"

        fun backfillKey(userId: String): String =
            "startup_backfill_v2_${AccountIds.sanitizeFilePart(userId)}"

        fun needsRetryKey(userId: String): String =
            "startup_backfill_needs_retry_v2_${AccountIds.sanitizeFilePart(userId)}"
    }
}

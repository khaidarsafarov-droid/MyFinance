package com.truckerload.data.sync

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.truckerload.data.preferences.AccountIds

/**
 * Per-account sync cursor: last successful push/pull timestamp.
 * Used for «what changed since I left?» pull sync (Stage 2).
 */
class CloudSyncCursorStore(context: Context) {
    private val appContext = context.applicationContext

    fun lastSyncedAt(userId: String): Long =
        prefs(userId).getLong(KEY_LAST_SYNCED_AT, 0L)

    fun markSynced(userId: String, at: Long = System.currentTimeMillis()) {
        prefs(userId).edit {
            putLong(KEY_LAST_SYNCED_AT, at)
            putLong(KEY_LAST_SYNC_ATTEMPT_AT, at)
        }
    }

    fun markAttempt(userId: String, at: Long = System.currentTimeMillis()) {
        prefs(userId).edit { putLong(KEY_LAST_SYNC_ATTEMPT_AT, at) }
    }

    fun lastFullHydrationAt(userId: String): Long =
        prefs(userId).getLong(KEY_LAST_HYDRATION_AT, 0L)

    fun markFullHydration(
        userId: String,
        at: Long = System.currentTimeMillis(),
        markSynced: Boolean = true,
    ) {
        prefs(userId).edit {
            putLong(KEY_LAST_HYDRATION_AT, at)
            if (markSynced) putLong(KEY_LAST_SYNCED_AT, at)
        }
    }

    fun clear(userId: String) {
        prefs(userId).edit { clear() }
    }

    private fun prefs(userId: String): SharedPreferences {
        val safe = AccountIds.sanitizeFilePart(userId.trim())
        return appContext.getSharedPreferences(PREFS_PREFIX + safe, Context.MODE_PRIVATE)
    }

    companion object {
        private const val PREFS_PREFIX = "truckerload_cloud_sync_"
        private const val KEY_LAST_SYNCED_AT = "last_synced_at"
        private const val KEY_LAST_SYNC_ATTEMPT_AT = "last_sync_attempt_at"
        private const val KEY_LAST_HYDRATION_AT = "last_full_hydration_at"
    }
}

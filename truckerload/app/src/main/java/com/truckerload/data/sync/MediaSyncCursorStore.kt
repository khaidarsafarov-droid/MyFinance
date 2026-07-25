package com.truckerload.data.sync

import android.content.Context
import com.truckerload.data.preferences.AccountIds

class MediaSyncCursorStore(context: Context) {
    private val appContext = context.applicationContext

    fun get(accountId: String): Long =
        prefs(accountId).getLong(KEY_REVISION, 0L).coerceAtLeast(0)

    fun set(accountId: String, revision: Long) {
        prefs(accountId).edit().putLong(KEY_REVISION, revision.coerceAtLeast(0)).apply()
    }

    private fun prefs(accountId: String) = appContext.getSharedPreferences(
        PREFIX + AccountIds.sanitizeFilePart(accountId),
        Context.MODE_PRIVATE,
    )

    companion object {
        private const val PREFIX = "truckerload_media_sync_"
        private const val KEY_REVISION = "revision"
    }
}

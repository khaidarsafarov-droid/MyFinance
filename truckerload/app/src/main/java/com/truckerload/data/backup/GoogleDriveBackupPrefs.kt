package com.truckerload.data.backup

import android.content.Context
import androidx.core.content.edit

/** Локальные настройки синхронизации с Google Drive (appDataFolder). */
class GoogleDriveBackupPrefs(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var accountEmail: String?
        get() = prefs.getString(KEY_EMAIL, null)
        set(value) = prefs.edit { putString(KEY_EMAIL, value) }

    var driveFileId: String?
        get() = prefs.getString(KEY_FILE_ID, null)
        set(value) = prefs.edit { putString(KEY_FILE_ID, value) }

    var autoSyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SYNC, true)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_SYNC, value) }

    var lastSyncAt: Long
        get() = prefs.getLong(KEY_LAST_SYNC, 0L)
        set(value) = prefs.edit { putLong(KEY_LAST_SYNC, value) }

    val isLinked: Boolean
        get() = !accountEmail.isNullOrBlank()

    fun clear() {
        prefs.edit { clear() }
    }

    companion object {
        private const val PREFS = "google_drive_backup"
        private const val KEY_EMAIL = "account_email"
        private const val KEY_FILE_ID = "drive_file_id"
        private const val KEY_AUTO_SYNC = "auto_sync"
        private const val KEY_LAST_SYNC = "last_sync_at"

        const val BACKUP_FILE_NAME = "truckerload_backup.tlb"
        const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    }
}

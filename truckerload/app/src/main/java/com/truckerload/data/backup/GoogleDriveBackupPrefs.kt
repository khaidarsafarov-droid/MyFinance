package com.truckerload.data.backup

import android.content.Context
import androidx.core.content.edit
import com.truckerload.data.preferences.AccountIds
import com.truckerload.data.preferences.AuthStore

/**
 * Per-user Google Drive backup prefs (appDataFolder file id / email / sync times).
 * Scoped by active [AuthStore] account so account B cannot inherit A’s driveFileId.
 */
class GoogleDriveBackupPrefs(
    context: Context,
    userId: String = AuthStore(context).currentUserIdOrNull() ?: AccountIds.LOCAL_DEV,
) {
    /** Active TruckerLoad account this prefs instance is scoped to. */
    val accountId: String = userId

    private val prefs = context.applicationContext
        .getSharedPreferences(prefsName(userId), Context.MODE_PRIVATE)
        .also { scoped -> migrateFromLegacyIfEmpty(context.applicationContext, scoped) }

    /** Drive appDataFolder object name — unique per TruckerLoad account. */
    fun backupFileName(): String =
        "truckerload_backup_${AccountIds.sanitizeFilePart(accountId)}.tlb"

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

    /** Drive file modifiedTime (epoch ms) from last list/upload/download. */
    var remoteModifiedAt: Long
        get() = prefs.getLong(KEY_REMOTE_MODIFIED, 0L)
        set(value) = prefs.edit { putLong(KEY_REMOTE_MODIFIED, value) }

    val isLinked: Boolean
        get() = !accountEmail.isNullOrBlank()

    fun clear() {
        prefs.edit { clear() }
    }

    companion object {
        private const val LEGACY_PREFS = "google_drive_backup"
        private const val KEY_EMAIL = "account_email"
        private const val KEY_FILE_ID = "drive_file_id"
        private const val KEY_AUTO_SYNC = "auto_sync"
        private const val KEY_LAST_SYNC = "last_sync_at"
        private const val KEY_REMOTE_MODIFIED = "remote_modified_at"

        const val BACKUP_FILE_NAME = "truckerload_backup.tlb"
        const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        private const val META_PREFS = "truckerload_account_meta"
        private const val KEY_LEGACY_MIGRATED = "legacy_drive_backup_migrated"

        fun prefsName(userId: String): String =
            "google_drive_backup_${AccountIds.sanitizeFilePart(userId)}"

        fun backupFileNameFor(userId: String): String =
            "truckerload_backup_${AccountIds.sanitizeFilePart(userId)}.tlb"

        private fun migrateFromLegacyIfEmpty(context: Context, scoped: android.content.SharedPreferences) {
            if (scoped.contains(KEY_EMAIL) || scoped.contains(KEY_FILE_ID)) return
            val meta = context.getSharedPreferences(META_PREFS, Context.MODE_PRIVATE)
            // FIX: one-shot migrate — previously every new account cloned the same driveFileId
            if (meta.getBoolean(KEY_LEGACY_MIGRATED, false)) return
            val legacy = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
            if (legacy.all.isEmpty()) {
                meta.edit { putBoolean(KEY_LEGACY_MIGRATED, true) }
                return
            }
            scoped.edit {
                legacy.getString(KEY_EMAIL, null)?.let { putString(KEY_EMAIL, it) }
                legacy.getString(KEY_FILE_ID, null)?.let { putString(KEY_FILE_ID, it) }
                putBoolean(KEY_AUTO_SYNC, legacy.getBoolean(KEY_AUTO_SYNC, true))
                putLong(KEY_LAST_SYNC, legacy.getLong(KEY_LAST_SYNC, 0L))
                putLong(KEY_REMOTE_MODIFIED, legacy.getLong(KEY_REMOTE_MODIFIED, 0L))
            }
            legacy.edit { clear() }
            meta.edit { putBoolean(KEY_LEGACY_MIGRATED, true) }
        }
    }
}

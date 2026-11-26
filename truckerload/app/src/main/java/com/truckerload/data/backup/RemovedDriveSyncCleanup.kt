package com.truckerload.data.backup

import android.content.Context
import androidx.core.content.edit
import com.truckerload.data.preferences.AccountIds
import com.truckerload.data.preferences.AuthStore
import java.io.File

/**
 * Google Drive sync and Google Sign-In were removed. On the first start after the
 * update this wipes what they left on the device: the linked Google email, the Drive
 * file id and sync times, the Play Services sign-in cache, the old Google ID token,
 * and the unused local email/password verifiers.
 *
 * Only these preference files are touched. The journal database, photos, scans,
 * local backups, and the profile stay as they are.
 */
object RemovedDriveSyncCleanup {
    private const val META_PREFS = "truckerload_account_meta"
    private const val KEY_DONE = "drive_sync_removed_cleanup_v1"

    /** Names without `.xml`. Drive prefs were per account: `google_drive_backup_<id>`. */
    private val EXACT_NAMES = setOf(
        "google_drive_backup",
        "truckerload_auth_credentials",
        "truckerload_auth_credentials_enc",
        "truckerload_auth_credentials_enc_fallback",
        "com.google.android.gms.signin",
    )
    private const val DRIVE_PREFIX = "google_drive_backup_"

    fun isRemovedSyncPrefs(name: String): Boolean = name in EXACT_NAMES || name.startsWith(DRIVE_PREFIX)

    /** Safe to call on every start; does the work once. */
    fun runOnce(context: Context) {
        val app = context.applicationContext
        val meta = app.getSharedPreferences(META_PREFS, Context.MODE_PRIVATE)
        if (meta.getBoolean(KEY_DONE, false)) return
        val prefsDir = File(app.applicationInfo.dataDir, "shared_prefs")
        val onDisk = prefsDir.listFiles()
            ?.map { it.name.removeSuffix(".xml") }
            ?.filter(::isRemovedSyncPrefs)
            .orEmpty()
        val known = EXACT_NAMES + listOfNotNull(
            AccountIds.LOCAL_DEV,
            runCatching { AuthStore(app).currentUserIdOrNull() }.getOrNull(),
        ).map { DRIVE_PREFIX + AccountIds.sanitizeFilePart(it) }
        (onDisk + known).toSet()
            .forEach { name ->
                runCatching {
                    app.getSharedPreferences(name, Context.MODE_PRIVATE).edit(commit = true) { clear() }
                    app.deleteSharedPreferences(name)
                }
            }
        runCatching { AuthStore(app).clearGoogleIdToken() }
        meta.edit {
            putBoolean(KEY_DONE, true)
            remove("legacy_drive_backup_migrated")
        }
    }
}

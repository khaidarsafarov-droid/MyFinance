package com.truckerload.data.backup

import android.content.Context
import android.util.Log
import com.truckerload.R
import com.truckerload.utils.BackupService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Google Drive backup / restore / auto-push, with the remote store injected
 * so unit tests can run the same flow against an in-memory fake.
 */
internal class DriveBackupFlow(
    private val isApiKeyConfigured: () -> Boolean,
    private val isLinkedOrGranted: (Context) -> Boolean,
    private val syncLinkedAccount: (Context) -> Unit,
    private val createBackupJson: suspend (Context) -> String?,
    private val restoreBackupJson: suspend (Context, String, Boolean) -> Result<BackupData>,
    private val clientFactory: (Context, GoogleDriveBackupPrefs) -> DriveRemoteStore,
) {
    suspend fun backupNow(context: Context): Result<String> = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        if (!isApiKeyConfigured()) {
            return@withContext Result.failure(
                IllegalStateException(app.getString(R.string.drive_sync_key_missing)),
            )
        }
        val prefs = GoogleDriveBackupService.prefs(app)
        if (!isLinkedOrGranted(app)) {
            return@withContext Result.failure(
                IllegalStateException(app.getString(R.string.drive_sync_not_linked)),
            )
        }
        syncLinkedAccount(app)
        val json = createBackupJson(app)
            ?: return@withContext Result.failure(
                IllegalStateException(app.getString(R.string.drive_sync_nothing_to_backup)),
            )
        val client = clientFactory(app, prefs)
        client.uploadBackupJson(json).fold(
            onSuccess = {
                prefs.lastSyncAt = System.currentTimeMillis()
                prefs.lastSyncError = null
                prefs.lastSyncErrorAt = 0L
                val msg = app.getString(
                    R.string.drive_sync_backup_ok,
                    prefs.accountEmail.orEmpty(),
                )
                Result.success(msg)
            },
            onFailure = {
                val mapped = GoogleDriveBackupService.mapError(app, it)
                prefs.lastSyncError = mapped.message ?: it.message ?: "backup_failed"
                prefs.lastSyncErrorAt = System.currentTimeMillis()
                Result.failure(mapped)
            },
        )
    }

    suspend fun restoreNow(
        context: Context,
        allowUnboundAccount: Boolean = false,
    ): Result<String> = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        if (!isApiKeyConfigured()) {
            return@withContext Result.failure(
                IllegalStateException(app.getString(R.string.drive_sync_key_missing)),
            )
        }
        val prefs = GoogleDriveBackupService.prefs(app)
        if (!isLinkedOrGranted(app)) {
            return@withContext Result.failure(
                IllegalStateException(app.getString(R.string.drive_sync_not_linked)),
            )
        }
        syncLinkedAccount(app)
        val client = clientFactory(app, prefs)
        val jsonResult = client.downloadBackupJson()
        val json = jsonResult.getOrElse { return@withContext Result.failure(GoogleDriveBackupService.mapError(app, it)) }
        restoreBackupJson(app, json, allowUnboundAccount).fold(
            onSuccess = { backup ->
                prefs.lastSyncAt = System.currentTimeMillis()
                prefs.lastSyncError = null
                prefs.lastSyncErrorAt = 0L
                Result.success(
                    app.getString(
                        R.string.backup_restore_success,
                        backup.loads.size,
                        backup.paychecks.size,
                        backup.diesel.size,
                    )
                )
            },
            onFailure = { Result.failure(it) },
        )
    }

    suspend fun pushAutoBackupIfEnabled(context: Context): Boolean {
        if (!isApiKeyConfigured()) return true
        val app = context.applicationContext
        val prefs = GoogleDriveBackupService.prefs(app)
        if (!prefs.autoSyncEnabled) return true
        if (!isLinkedOrGranted(app)) return true
        syncLinkedAccount(app)
        val client = clientFactory(app, prefs)
        val remote = client.refreshRemoteBackup()
        if (remote.isFailure) {
            Log.w(TAG, "auto push skipped — Drive metadata unavailable")
            prefs.lastSyncError = "drive_metadata_unavailable"
            prefs.lastSyncErrorAt = System.currentTimeMillis()
            return false
        }
        val remoteExists = remote.getOrDefault(false)
        if (remoteExists &&
            DriveSyncPolicy.shouldSkipAutoPush(prefs.remoteModifiedAt, prefs.lastSyncAt)
        ) {
            Log.i(TAG, "auto push skipped — remote backup is newer or unsynced on this device")
            return true
        }
        val json = createBackupJson(app) ?: return true
        return client.uploadBackupJson(json)
            .onSuccess {
                prefs.lastSyncAt = System.currentTimeMillis()
                prefs.lastSyncError = null
                prefs.lastSyncErrorAt = 0L
                Log.d(TAG, "auto push ok")
            }
            .onFailure {
                prefs.lastSyncError = it.message ?: "auto_push_failed"
                prefs.lastSyncErrorAt = System.currentTimeMillis()
                Log.w(TAG, "auto push failed: ${it.message}")
            }
            .isSuccess
    }

    companion object {
        private const val TAG = "GoogleDriveBackup"

        fun production(): DriveBackupFlow = DriveBackupFlow(
            isApiKeyConfigured = { GoogleDriveBackupService.isApiKeyConfigured() },
            isLinkedOrGranted = { ctx ->
                GoogleDriveBackupService.prefs(ctx).isLinked ||
                    GoogleDriveBackupService.isDriveScopeGranted(ctx)
            },
            syncLinkedAccount = { GoogleDriveBackupService.syncLinkedAccountFromGoogle(it) },
            createBackupJson = { BackupService.createBackupJson(it) },
            restoreBackupJson = { context, json, allowUnbound ->
                BackupService.restoreBackupJson(context, json, allowUnbound)
            },
            clientFactory = { context, prefs -> GoogleDriveApiClient(context, prefs) },
        )
    }
}

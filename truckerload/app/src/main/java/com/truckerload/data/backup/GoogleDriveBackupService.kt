package com.truckerload.data.backup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.Scope
import com.truckerload.R
import com.truckerload.data.remote.GoogleSignInClients
import com.truckerload.utils.BackupService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Синхронизация локального Room-бэкапа с личным Google Drive (appDataFolder).
 * iCloud на Android недоступен — только Google-аккаунт пользователя.
 */
object GoogleDriveBackupService {
    private const val TAG = "GoogleDriveBackup"

    /** Must be an [Activity] — Play Services will not show the account picker from applicationContext. */
    fun signInIntent(activity: Activity): Intent =
        GoogleSignInClients.driveSignInIntent(activity)

    /** Email only when Drive app-data scope is still granted for the signed-in Google account. */
    fun linkedAccountEmail(context: Context): String? {
        if (!isDriveScopeGranted(context)) return null
        return prefs(context).accountEmail
            ?: GoogleSignIn.getLastSignedInAccount(context)?.email
    }

    fun prefs(context: Context) = GoogleDriveBackupPrefs(context)

    fun isDriveScopeGranted(context: Context): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return false
        return GoogleSignIn.hasPermissions(
            account,
            Scope(GoogleDriveBackupPrefs.DRIVE_APPDATA_SCOPE),
        )
    }

    fun syncLinkedAccountFromGoogle(context: Context) {
        val prefs = prefs(context)
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null && isDriveScopeGranted(context)) {
            prefs.accountEmail = account.email
        }
    }

    suspend fun disconnect(context: Context) = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val prefs = prefs(app)
        prefs.clear()
        runCatching {
            GoogleSignIn.getClient(app, GoogleSignInClients.driveOptions()).signOut().await()
        }.onFailure { Log.w(TAG, "signOut failed", it) }
    }

    suspend fun backupNow(context: Context): Result<String> = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val prefs = prefs(app)
        if (!prefs.isLinked && !isDriveScopeGranted(app)) {
            return@withContext Result.failure(
                IllegalStateException(app.getString(R.string.drive_sync_not_linked)),
            )
        }
        syncLinkedAccountFromGoogle(app)
        val json = BackupService.createBackupJson(app)
            ?: return@withContext Result.failure(
                IllegalStateException(app.getString(R.string.drive_sync_nothing_to_backup)),
            )
        val client = GoogleDriveApiClient(app, prefs)
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
                val mapped = mapError(app, it)
                prefs.lastSyncError = mapped.message ?: it.message ?: "backup_failed"
                prefs.lastSyncErrorAt = System.currentTimeMillis()
                Result.failure(mapped)
            },
        )
    }

    suspend fun restoreNow(context: Context): Result<String> = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val prefs = prefs(app)
        if (!prefs.isLinked && !isDriveScopeGranted(app)) {
            return@withContext Result.failure(
                IllegalStateException(app.getString(R.string.drive_sync_not_linked)),
            )
        }
        syncLinkedAccountFromGoogle(app)
        val client = GoogleDriveApiClient(app, prefs)
        val jsonResult = client.downloadBackupJson()
        val json = jsonResult.getOrElse { return@withContext Result.failure(mapError(app, it)) }
        BackupService.restoreBackupJson(app, json).fold(
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

    /** Фоновый push после локального auto-backup — без UI. Returns false when upload failed. */
    suspend fun pushAutoBackupIfEnabled(context: Context): Boolean {
        val app = context.applicationContext
        val prefs = prefs(app)
        if (!prefs.autoSyncEnabled) return true
        if (!prefs.isLinked && !isDriveScopeGranted(app)) return true
        syncLinkedAccountFromGoogle(app)
        val client = GoogleDriveApiClient(app, prefs)
        runCatching { client.hasRemoteBackup() }
        // FIX: auto-push used to PATCH Drive even when remote was newer (or this device never synced)
        if (DriveSyncPolicy.shouldSkipAutoPush(prefs.remoteModifiedAt, prefs.lastSyncAt)) {
            Log.i(TAG, "auto push skipped — remote backup is newer or unsynced on this device")
            return true
        }
        val json = BackupService.createBackupJson(app) ?: return true
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

    /**
     * True when restore may overwrite newer local edits with a newer Drive file.
     * Call after refreshing remote metadata via [probeRemote].
     */
    fun shouldWarnBeforeRestore(context: Context, localChangedAfterLastSync: Boolean): Boolean {
        val prefs = prefs(context)
        return DriveSyncPolicy.shouldWarnBeforeRestore(
            localChangedAfterLastSync = localChangedAfterLastSync,
            remoteModifiedAt = prefs.remoteModifiedAt,
            lastSyncAt = prefs.lastSyncAt,
        )
    }

    /** Local loads edited after last successful Drive sync. */
    suspend fun hasLocalChangesAfterLastSync(context: Context): Boolean = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val lastSync = prefs(app).lastSyncAt
        val db = com.truckerload.data.local.AppDatabase.getInstanceForActiveUser(app)
            ?: return@withContext lastSync <= 0L
        val loads = com.truckerload.data.repository.LoadRepository(db).getAllLoadsOnce()
        if (lastSync <= 0L) return@withContext loads.isNotEmpty()
        loads.any { it.updatedAt > lastSync || it.parsedAt > lastSync }
    }

    suspend fun probeRemote(context: Context): Boolean = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        if (!prefs(app).isLinked && !isDriveScopeGranted(app)) return@withContext false
        GoogleDriveApiClient(app, prefs(app)).hasRemoteBackup()
    }

    fun mapError(context: Context, error: Throwable): Exception {
        return when (error) {
            is GoogleDriveApiClient.DriveError.NeedsUserConsent -> error
            is GoogleDriveApiClient.DriveError.NotSignedIn ->
                IllegalStateException(context.getString(R.string.drive_sync_not_linked))
            is GoogleDriveApiClient.DriveError.Api ->
                IllegalStateException(
                    context.getString(R.string.drive_sync_api_error, error.message ?: ""),
                )
            else -> IllegalStateException(
                error.message ?: context.getString(R.string.drive_sync_api_error, ""),
            )
        }
    }

    /** После успешного Activity Result от Google Sign-In. */
    fun onSignInResult(context: Context, data: Intent?): Boolean {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        return try {
            val account = task.result
            if (account != null && GoogleSignIn.hasPermissions(
                    account,
                    Scope(GoogleDriveBackupPrefs.DRIVE_APPDATA_SCOPE),
                )
            ) {
                prefs(context).accountEmail = account.email
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "onSignInResult failed", e)
            false
        }
    }

    fun launchConsentIfNeeded(activity: Activity, error: Throwable): Boolean {
        val consent = error as? GoogleDriveApiClient.DriveError.NeedsUserConsent
            ?: (error.cause as? GoogleDriveApiClient.DriveError.NeedsUserConsent)
        if (consent?.intent != null) {
            activity.startActivity(consent.intent)
            return true
        }
        return false
    }
}

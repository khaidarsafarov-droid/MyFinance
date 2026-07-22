package com.truckerload.data.backup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.truckerload.R
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

    fun signInIntent(context: Context): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(GoogleDriveBackupPrefs.DRIVE_APPDATA_SCOPE))
            .build()
        return GoogleSignIn.getClient(context.applicationContext, gso).signInIntent
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
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope(GoogleDriveBackupPrefs.DRIVE_APPDATA_SCOPE))
                .build()
            GoogleSignIn.getClient(app, gso).signOut().await()
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
                val msg = app.getString(
                    R.string.drive_sync_backup_ok,
                    prefs.accountEmail.orEmpty(),
                )
                Result.success(msg)
            },
            onFailure = { Result.failure(mapError(app, it)) },
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

    /** Фоновый push после локального auto-backup — без UI. */
    suspend fun pushAutoBackupIfEnabled(context: Context) {
        val app = context.applicationContext
        val prefs = prefs(app)
        if (!prefs.autoSyncEnabled) return
        if (!prefs.isLinked && !isDriveScopeGranted(app)) return
        syncLinkedAccountFromGoogle(app)
        val json = BackupService.createBackupJson(app) ?: return
        GoogleDriveApiClient(app, prefs).uploadBackupJson(json)
            .onFailure { Log.w(TAG, "auto push failed: ${it.message}") }
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

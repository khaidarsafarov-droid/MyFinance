package com.truckerload.data.backup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.truckerload.BuildConfig
import com.truckerload.R
import com.truckerload.data.remote.GoogleSignInClients
import com.truckerload.utils.logFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Синхронизация локального Room-бэкапа с личным Google Drive (appDataFolder).
 * iCloud на Android недоступен — только Google-аккаунт пользователя.
 */
object GoogleDriveBackupService {
    private const val TAG = "GoogleDriveBackup"

    /** False when the Drive API key was not supplied outside the repo. Backup stays off. */
    fun isApiKeyConfigured(): Boolean = BuildConfig.GOOGLE_DRIVE_API_KEY.isNotBlank()

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
        return hasDriveScope(account)
    }

    fun hasDriveScope(account: GoogleSignInAccount): Boolean =
        GoogleSignIn.hasPermissions(
            account,
            Scope(GoogleDriveBackupPrefs.DRIVE_APPDATA_SCOPE),
        )

    fun connectStartsAtDriveConsent(activity: Activity): Boolean =
        !GoogleSignIn.getLastSignedInAccount(activity)?.email.isNullOrBlank()

    data class ParsedSignIn(
        val email: String?,
        val grantedDriveScope: Boolean,
        val error: Throwable?,
    )

    fun parseSignInIntent(data: Intent?): ParsedSignIn {
        return try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                .getResult(ApiException::class.java)
            ParsedSignIn(
                email = account?.email,
                grantedDriveScope = account != null && hasDriveScope(account),
                error = null,
            )
        } catch (e: ApiException) {
            if (!DriveConnectInterpreter.isUserCancel(e)) {
                logFailure(TAG, "parseSignInIntent failed", e)
            }
            ParsedSignIn(email = null, grantedDriveScope = false, error = e)
        }
    }

    fun linkAccountEmail(context: Context, email: String) {
        prefs(context).accountEmail = email
    }

    fun consentIntent(error: Throwable): Intent? {
        val consent = error as? GoogleDriveApiClient.DriveError.NeedsUserConsent
            ?: (error.cause as? GoogleDriveApiClient.DriveError.NeedsUserConsent)
        return consent?.intent
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

    suspend fun backupNow(context: Context): Result<String> =
        DriveBackupFlow.production().backupNow(context)

    suspend fun restoreNow(
        context: Context,
        allowUnboundAccount: Boolean = false,
    ): Result<String> =
        DriveBackupFlow.production().restoreNow(context, allowUnboundAccount)

    /** Фоновый push после локального auto-backup — без UI. Returns false when upload failed. */
    suspend fun pushAutoBackupIfEnabled(context: Context): Boolean =
        DriveBackupFlow.production().pushAutoBackupIfEnabled(context)

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
        val parsed = parseSignInIntent(data)
        if (parsed.grantedDriveScope && !parsed.email.isNullOrBlank()) {
            linkAccountEmail(context, parsed.email)
            return true
        }
        return false
    }

    fun launchConsentIfNeeded(activity: Activity, error: Throwable): Boolean {
        val intent = consentIntent(error) ?: return false
        activity.startActivity(intent)
        return true
    }
}

package com.truckerload.data.remote

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.truckerload.BuildConfig
import com.truckerload.data.backup.GoogleDriveBackupPrefs
import kotlinx.coroutines.tasks.await

/**
 * Shared Google Sign-In clients. Sign-in intents must be built from an [Activity],
 * never [Context.getApplicationContext] — Play Services otherwise fails silently.
 */
object GoogleSignInClients {

    fun loginOptions(requestIdToken: Boolean): GoogleSignInOptions {
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
        if (requestIdToken && BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()) {
            builder.requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
        }
        return builder.build()
    }

    fun loginIntent(activity: Activity, requestIdToken: Boolean): Intent =
        GoogleSignIn.getClient(activity, loginOptions(requestIdToken)).signInIntent

    fun driveOptions(accountEmail: String? = null): GoogleSignInOptions {
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(GoogleDriveBackupPrefs.DRIVE_APPDATA_SCOPE))
        accountEmail?.trim()?.takeIf { it.isNotBlank() }?.let { builder.setAccountName(it) }
        return builder.build()
    }

    /**
     * Opens Google account picker, or re-consents Drive app-data for the account
     * already signed in on the device (setAccountName). Using the last email is
     * required — a second sign-in without it often returns the old account and
     * skips the Drive permission screen.
     */
    fun driveSignInIntent(activity: Activity): Intent {
        val email = GoogleSignIn.getLastSignedInAccount(activity)?.email
        return GoogleSignIn.getClient(activity, driveOptions(email)).signInIntent
    }

    fun isDeveloperError(error: Throwable): Boolean {
        val api = error as? ApiException ?: error.cause as? ApiException
        return api?.statusCode == CommonStatusCodes.DEVELOPER_ERROR
    }

    /**
     * Status 10 (DEVELOPER_ERROR) with a wrong Web client ID still allows local
     * Google login (email / `sub`) if we retry without [GoogleSignInOptions.Builder.requestIdToken].
     */
    fun shouldRetryWithoutIdToken(error: Throwable, alreadyOmittingIdToken: Boolean): Boolean =
        !alreadyOmittingIdToken &&
            BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank() &&
            isDeveloperError(error)

    /** Clears Play Services + Credential Manager so the next login can pick another account. */
    suspend fun signOutDevice(context: Context) {
        val app = context.applicationContext
        runCatching {
            GoogleSignIn.getClient(app, driveOptions()).signOut().await()
        }
        runCatching {
            GoogleSignIn.getClient(app, loginOptions(requestIdToken = false)).signOut().await()
        }
        runCatching {
            CredentialManager.create(app).clearCredentialState(ClearCredentialStateRequest())
        }
    }
}

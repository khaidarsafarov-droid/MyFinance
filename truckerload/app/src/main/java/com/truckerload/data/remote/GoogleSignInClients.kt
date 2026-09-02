package com.truckerload.data.remote

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.truckerload.data.backup.GoogleDriveBackupPrefs
import kotlinx.coroutines.tasks.await

/**
 * Google Sign-In clients used only for optional Drive app-data backup.
 * App identity is local ([com.truckerload.data.preferences.LocalDeviceOnboarding]).
 *
 * Sign-in intents must be built from an [Activity], never
 * [Context.getApplicationContext] — Play Services otherwise fails silently.
 */
object GoogleSignInClients {

    fun driveOptions(accountEmail: String? = null): GoogleSignInOptions {
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(GoogleDriveBackupPrefs.DRIVE_APPDATA_SCOPE))
        accountEmail?.trim()?.takeIf { it.isNotBlank() }?.let { builder.setAccountName(it) }
        return builder.build()
    }

    /** Account picker only — no Drive scope. Pair with [driveConsentIntent]. */
    fun identityOptions(): GoogleSignInOptions =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

    fun identitySignInIntent(activity: Activity): Intent =
        GoogleSignIn.getClient(activity, identityOptions()).signInIntent

    /** Drive app-data consent for a known account. Skips the multi-account picker. */
    fun driveConsentIntent(activity: Activity, accountEmail: String): Intent =
        GoogleSignIn.getClient(activity, driveOptions(accountEmail)).signInIntent

    /**
     * Opens Google account picker, or re-consents Drive app-data for the account
     * already signed in on the device (setAccountName). Using the last email is
     * required — a second sign-in without it often returns the old account and
     * skips the Drive permission screen.
     *
     * The picker itself must not request `drive.appdata`: Play Services then
     * closes after the account tap and never shows Drive consent.
     */
    fun driveSignInIntent(activity: Activity): Intent {
        val email = GoogleSignIn.getLastSignedInAccount(activity)?.email
        return if (email.isNullOrBlank()) {
            identitySignInIntent(activity)
        } else {
            driveConsentIntent(activity, email)
        }
    }

    fun isDeveloperError(error: Throwable): Boolean {
        val api = error as? ApiException ?: error.cause as? ApiException
        return api?.statusCode == CommonStatusCodes.DEVELOPER_ERROR
    }

    /** Clears Play Services so the next Drive connect can pick another account. */
    suspend fun signOutDevice(context: Context) {
        val app = context.applicationContext
        runCatching {
            GoogleSignIn.getClient(app, driveOptions()).signOut().await()
        }
        runCatching {
            GoogleSignIn.getClient(app, identityOptions()).signOut().await()
        }
    }
}

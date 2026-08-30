package com.truckerload.data.backup

import android.app.Activity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes

/**
 * Settings → Google Drive connect is a two-step Play Services flow.
 *
 * Combining the account picker with the restricted `drive.appdata` scope in one
 * [GoogleSignIn] intent makes the picker close after the account tap and return
 * `RESULT_CANCELED` (or an account without Drive). Identity first, then a
 * `setAccountName` Drive-consent intent, is the working sequence.
 */
sealed class DriveConnectOutcome {
    data object Granted : DriveConnectOutcome()
    data class RequestDriveConsent(val email: String) : DriveConnectOutcome()
    data object RetryBackup : DriveConnectOutcome()
    data object Cancelled : DriveConnectOutcome()
    data class Failed(val error: Throwable?) : DriveConnectOutcome()
}

enum class DriveConnectPending {
    None,
    AccountPicker,
    DriveConsent,
    TokenConsent,
}

object DriveConnectInterpreter {

    fun next(
        resultCode: Int,
        accountEmail: String?,
        grantedDriveScope: Boolean,
        error: Throwable?,
        pending: DriveConnectPending,
    ): DriveConnectOutcome {
        if (pending == DriveConnectPending.TokenConsent) {
            return if (resultCode == Activity.RESULT_OK) {
                DriveConnectOutcome.RetryBackup
            } else {
                DriveConnectOutcome.Cancelled
            }
        }
        if (resultCode != Activity.RESULT_OK) {
            return if (error == null || isUserCancel(error)) {
                DriveConnectOutcome.Cancelled
            } else {
                DriveConnectOutcome.Failed(error)
            }
        }
        val email = accountEmail?.trim().orEmpty()
        if (grantedDriveScope && email.isNotEmpty()) {
            return DriveConnectOutcome.Granted
        }
        if (email.isNotEmpty() && pending != DriveConnectPending.DriveConsent) {
            return DriveConnectOutcome.RequestDriveConsent(email)
        }
        return DriveConnectOutcome.Failed(error)
    }

    /** Play Services cancel / empty-intent codes — not a console misconfig. */
    fun isUserCancel(error: Throwable?): Boolean {
        val api = error as? ApiException ?: error?.cause as? ApiException ?: return false
        return api.statusCode == 12501 ||
            api.statusCode == CommonStatusCodes.CANCELED ||
            api.statusCode == CommonStatusCodes.SIGN_IN_REQUIRED
    }
}

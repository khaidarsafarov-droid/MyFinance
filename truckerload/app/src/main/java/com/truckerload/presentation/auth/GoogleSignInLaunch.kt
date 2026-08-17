package com.truckerload.presentation.auth

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.truckerload.data.remote.GoogleSignInClients
import com.truckerload.utils.findActivity

internal const val GOOGLE_SIGN_IN_NO_ACTIVITY = "no_activity"

internal fun launchLegacyGoogleSignIn(
    context: Context,
    launcher: ActivityResultLauncher<Intent>,
    requestIdToken: Boolean,
): Result<Unit> {
    val activity = context.findActivity()
        ?: return Result.failure(IllegalStateException(GOOGLE_SIGN_IN_NO_ACTIVITY))
    return runCatching {
        launcher.launch(GoogleSignInClients.loginIntent(activity, requestIdToken))
    }
}

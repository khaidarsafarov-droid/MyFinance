package com.truckerload.presentation.screens.login

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.truckerload.BuildConfig
import com.truckerload.R
import com.truckerload.data.remote.GoogleSignInClients
import com.truckerload.data.repository.auth.GoogleAuthCredential
import com.truckerload.presentation.auth.GOOGLE_SIGN_IN_NO_ACTIVITY
import com.truckerload.presentation.auth.GoogleSignInSupport
import com.truckerload.presentation.auth.launchLegacyGoogleSignIn
import com.truckerload.presentation.screens.auth.AuthViewModel

/**
 * Activity-result bridge for legacy Google Sign-In (UI-only).
 * Business completion goes through [AuthViewModel.onLegacyGoogleAccount].
 */
@Composable
fun rememberLegacyGoogleSignInLaunch(viewModel: AuthViewModel): () -> Unit {
    val context = LocalContext.current
    var omitIdToken by remember { mutableStateOf(false) }
    val launcherRef = remember {
        arrayOfNulls<androidx.activity.result.ActivityResultLauncher<android.content.Intent>>(1)
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            viewModel.onLegacyGoogleCancelled()
            return@rememberLauncherForActivityResult
        }
        val data = result.data
        if (data == null) {
            viewModel.onLegacyGoogleError(
                context.getString(
                    R.string.login_google_error,
                    context.getString(R.string.login_google_no_data),
                ),
            )
            return@rememberLauncherForActivityResult
        }
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        task.addOnSuccessListener { account ->
            viewModel.onLegacyGoogleAccount(
                GoogleAuthCredential(
                    idToken = account.idToken,
                    email = account.email.orEmpty(),
                    givenName = account.givenName.orEmpty(),
                    familyName = account.familyName.orEmpty(),
                    photoUrl = account.photoUrl?.toString(),
                    googleId = account.id,
                ),
            )
        }
        task.addOnFailureListener { error ->
            val retryLauncher = launcherRef[0]
            if (retryLauncher != null &&
                GoogleSignInClients.shouldRetryWithoutIdToken(error, alreadyOmittingIdToken = omitIdToken)
            ) {
                omitIdToken = true
                launchLegacyOrReport(context, retryLauncher, requestIdToken = false, viewModel)
                return@addOnFailureListener
            }
            viewModel.onLegacyGoogleError(GoogleSignInSupport.formatError(context, error))
        }
    }
    launcherRef[0] = launcher
    return {
        val requestIdToken = !omitIdToken && BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()
        GoogleSignInSupport.signOutThen(context) {
            launchLegacyOrReport(context, launcher, requestIdToken, viewModel)
        }
    }
}

private fun launchLegacyOrReport(
    context: android.content.Context,
    launcher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>,
    requestIdToken: Boolean,
    viewModel: AuthViewModel,
) {
    launchLegacyGoogleSignIn(context, launcher, requestIdToken).onFailure { err ->
        val detail = if (err.message == GOOGLE_SIGN_IN_NO_ACTIVITY) {
            context.getString(R.string.login_google_need_activity)
        } else {
            GoogleSignInSupport.formatError(context, err)
        }
        viewModel.onLegacyGoogleError(detail)
    }
}

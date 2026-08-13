package com.truckerload.presentation.screens.login

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.truckerload.R
import com.truckerload.data.repository.auth.GoogleAuthCredential
import com.truckerload.presentation.auth.GoogleSignInSupport
import com.truckerload.presentation.screens.auth.AuthViewModel

/**
 * Activity-result bridge for legacy Google Sign-In (UI-only).
 * Business completion goes through [AuthViewModel.onLegacyGoogleAccount].
 */
@Composable
fun rememberLegacyGoogleSignInLaunch(viewModel: AuthViewModel): () -> Unit {
    val context = LocalContext.current
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
        task.addOnFailureListener {
            viewModel.onLegacyGoogleError(GoogleSignInSupport.formatError(context, it))
        }
    }
    return {
        // Clear stale account so the picker always shows (avoids silent no-op failures).
        GoogleSignInSupport.signOutThen(context) {
            launcher.launch(GoogleSignInSupport.client(context).signInIntent)
        }
    }
}

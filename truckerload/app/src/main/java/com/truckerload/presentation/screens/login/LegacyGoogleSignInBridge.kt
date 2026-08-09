package com.truckerload.presentation.screens.login

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.truckerload.BuildConfig
import com.truckerload.R
import com.truckerload.data.repository.auth.GoogleAuthCredential
import com.truckerload.presentation.screens.auth.AuthViewModel
import com.truckerload.utils.findActivity

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
            val msg = (it as? ApiException)?.message ?: it.message ?: it.toString()
            viewModel.onLegacyGoogleError(context.getString(R.string.login_google_error, msg))
        }
    }
    return {
        val activity = context.findActivity() ?: context
        val gso = if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()) {
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail().requestProfile().requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID).build()
        } else {
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail().requestProfile().build()
        }
        val client = GoogleSignIn.getClient(activity, gso)
        // Clear a stuck prior Google session (frequent on tablets) then show the picker.
        client.signOut().addOnCompleteListener {
            launcher.launch(client.signInIntent)
        }
    }
}

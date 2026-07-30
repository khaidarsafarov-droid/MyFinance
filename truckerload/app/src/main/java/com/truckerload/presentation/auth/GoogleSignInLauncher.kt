package com.truckerload.presentation.auth

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.truckerload.BuildConfig
import com.truckerload.R
import com.truckerload.data.remote.CredentialManagerGoogleSignIn
import com.truckerload.data.repository.AuthActionResult
import com.truckerload.data.repository.AuthRepository
import com.truckerload.data.repository.GoogleAccountInfo
import com.truckerload.di.AuthRepositoryEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

data class GoogleAuthCallbacks(
    val onBusy: (Boolean) -> Unit,
    val onSignedIn: () -> Unit = {},
)

class GoogleSignInLauncher(
    private val launchCredentialOrLegacy: () -> Unit,
) {
    fun launch() = launchCredentialOrLegacy()
}

@Composable
fun rememberGoogleSignInLauncher(
    callbacks: GoogleAuthCallbacks,
): GoogleSignInLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val callbacksState = rememberUpdatedState(callbacks)
    val authRepository = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            AuthRepositoryEntryPoint::class.java,
        ).authRepository()
    }

    fun applyGoogleResult(result: AuthActionResult) {
        result.toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
        callbacksState.value.onBusy(false)
        if (result.succeeded) {
            callbacksState.value.onSignedIn()
        } else if (result.fieldError != null && result.toastMessage == null) {
            Toast.makeText(context, result.fieldError, Toast.LENGTH_LONG).show()
        }
    }

    fun launchLegacy(launcher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>) {
        val gso = if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()) {
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .build()
        } else {
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .build()
        }
        launcher.launch(GoogleSignIn.getClient(context, gso).signInIntent)
    }

    val legacyLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            callbacksState.value.onBusy(false)
            return@rememberLauncherForActivityResult
        }
        val data = result.data
        if (data == null) {
            Toast.makeText(
                context,
                context.getString(R.string.login_google_error, context.getString(R.string.login_google_no_data)),
                Toast.LENGTH_SHORT,
            ).show()
            callbacksState.value.onBusy(false)
            return@rememberLauncherForActivityResult
        }
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        task.addOnSuccessListener { account ->
            scope.launch {
                val authResult = authRepository.signInWithGoogleAccount(
                    GoogleAccountInfo(
                        idToken = account.idToken,
                        email = account.email,
                        givenName = account.givenName,
                        familyName = account.familyName,
                        photoUrl = account.photoUrl?.toString(),
                        id = account.id,
                    ),
                )
                applyGoogleResult(authResult)
            }
        }
        task.addOnFailureListener {
            val message = (it as? ApiException)?.message ?: it.message ?: it.toString()
            Toast.makeText(
                context,
                context.getString(R.string.login_google_error, message),
                Toast.LENGTH_LONG,
            ).show()
            callbacksState.value.onBusy(false)
        }
    }

    return GoogleSignInLauncher {
        callbacksState.value.onBusy(true)
        if (!CredentialManagerGoogleSignIn.isAvailable()) {
            launchLegacy(legacyLauncher)
            return@GoogleSignInLauncher
        }
        scope.launch {
            val tokenResult = CredentialManagerGoogleSignIn.getGoogleIdToken(context)
            val idToken = tokenResult.getOrNull()
            if (idToken != null) {
                val authResult = authRepository.signInWithGoogleIdToken(idToken)
                applyGoogleResult(authResult)
            } else {
                when (tokenResult.exceptionOrNull()) {
                    is GetCredentialCancellationException -> {
                        Toast.makeText(
                            context,
                            context.getString(R.string.login_google_cancelled),
                            Toast.LENGTH_SHORT,
                        ).show()
                        callbacksState.value.onBusy(false)
                    }
                    else -> launchLegacy(legacyLauncher)
                }
            }
        }
    }
}

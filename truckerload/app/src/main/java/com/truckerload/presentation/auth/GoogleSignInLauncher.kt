package com.truckerload.presentation.auth

import android.app.Activity
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.truckerload.BuildConfig
import com.truckerload.R
import com.truckerload.data.preferences.AuthLogin
import com.truckerload.data.preferences.UserProfile
import com.truckerload.data.remote.GoogleSignInClients
import com.truckerload.data.remote.SupabaseAuthService
import com.truckerload.data.sync.DeviceSlotLogin
import com.truckerload.presentation.di.LocalAuthStore
import com.truckerload.presentation.di.LocalUserProfileStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

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
    val authStore = LocalAuthStore.current
    val userProfileStore = LocalUserProfileStore.current
    val scope = rememberCoroutineScope()
    val supabaseAuth = remember(context) { SupabaseAuthService(context.applicationContext) }
    val callbacksState = rememberUpdatedState(callbacks)
    var omitIdToken by remember { mutableStateOf(false) }
    val legacyLauncherRef = remember {
        arrayOfNulls<androidx.activity.result.ActivityResultLauncher<android.content.Intent>>(1)
    }

    fun saveAndFinish(
        email: String,
        givenName: String,
        familyName: String,
        photoUrl: String?,
        phoneNumber: String? = null,
        supabaseUserId: String? = null,
        accessToken: String? = null,
        refreshToken: String? = null,
        googleId: String? = null,
    ) {
        scope.launch {
            val persisted = withContext(Dispatchers.IO) {
                val ok = AuthLogin.tryCompleteLogin(
                    authStore = authStore,
                    userProfileStore = userProfileStore,
                    supabaseUserId = supabaseUserId,
                    profile = UserProfile(
                        email = email,
                        givenName = givenName,
                        familyName = familyName,
                        photoUrl = photoUrl,
                        phoneNumber = phoneNumber?.takeIf { it.isNotBlank() },
                        googleId = googleId?.takeIf { it.isNotBlank() },
                    ),
                    rememberMe = true,
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                )
                if (!ok) {
                    Result.failure(IllegalStateException(context.getString(R.string.auth_error_email_required)))
                } else {
                    DeviceSlotLogin.afterSessionPersisted(context, authStore)
                }
            }
            callbacksState.value.onBusy(false)
            persisted.fold(
                onSuccess = { callbacksState.value.onSignedIn() },
                onFailure = { error ->
                    Toast.makeText(
                        context,
                        error.message ?: context.getString(R.string.auth_error_email_required),
                        Toast.LENGTH_LONG,
                    ).show()
                },
            )
        }
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
            try {
                val idToken = account.idToken
                fun finishLocal() {
                    saveAndFinish(
                        account.email.orEmpty(),
                        account.givenName.orEmpty(),
                        account.familyName.orEmpty(),
                        resolveGooglePhotoUrl(null, idToken, account.photoUrl?.toString()),
                        accessToken = idToken,
                        googleId = account.id
                            ?: idToken?.let { decodeGoogleIdToken(it)?.optString("sub") },
                    )
                }
                if (supabaseAuth.isConfigured() && !idToken.isNullOrBlank()) {
                    scope.launch {
                        try {
                            val authResult = supabaseAuth.signInWithIdToken(idToken)
                            withContext(Dispatchers.Main) {
                                val signIn = authResult.getOrNull()
                                val user = signIn?.user
                                if (user != null) {
                                    val parts = (user.fullName ?: account.email?.take(10) ?: "User")
                                        .trim()
                                        .split(" ")
                                    saveAndFinish(
                                        user.email ?: account.email.orEmpty(),
                                        parts.firstOrNull() ?: account.givenName.orEmpty(),
                                        parts.drop(1).joinToString(" ").ifBlank {
                                            account.familyName.orEmpty()
                                        },
                                        resolveGooglePhotoUrl(
                                            user.avatarUrl,
                                            idToken,
                                            account.photoUrl?.toString(),
                                        ),
                                        supabaseUserId = user.id,
                                        accessToken = signIn.accessToken,
                                        refreshToken = signIn.refreshToken,
                                        googleId = account.id
                                            ?: decodeGoogleIdToken(idToken)?.optString("sub"),
                                    )
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(
                                            R.string.login_google_fallback,
                                            authResult.exceptionOrNull()?.message.orEmpty(),
                                        ),
                                        Toast.LENGTH_LONG,
                                    ).show()
                                    finishLocal()
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.login_google_fallback, e.message.orEmpty()),
                                    Toast.LENGTH_LONG,
                                ).show()
                                finishLocal()
                            }
                        }
                    }
                } else {
                    finishLocal()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    context.getString(R.string.login_google_error, e.message ?: e.toString()),
                    Toast.LENGTH_LONG,
                ).show()
                callbacksState.value.onBusy(false)
            }
        }
        task.addOnFailureListener { error ->
            val retryLauncher = legacyLauncherRef[0]
            if (retryLauncher != null &&
                GoogleSignInClients.shouldRetryWithoutIdToken(error, alreadyOmittingIdToken = omitIdToken)
            ) {
                omitIdToken = true
                launchLegacyGoogleSignIn(context, retryLauncher, requestIdToken = false)
                    .onFailure { toastLegacyLaunchFailure(context, it, callbacksState.value.onBusy) }
                return@addOnFailureListener
            }
            Toast.makeText(
                context,
                GoogleSignInSupport.formatError(context, error),
                Toast.LENGTH_LONG,
            ).show()
            callbacksState.value.onBusy(false)
        }
    }
    legacyLauncherRef[0] = legacyLauncher

    fun launchLegacy() {
        val requestIdToken = !omitIdToken && BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()
        GoogleSignInSupport.signOutThen(context) {
            launchLegacyGoogleSignIn(context, legacyLauncher, requestIdToken)
                .onFailure { toastLegacyLaunchFailure(context, it, callbacksState.value.onBusy) }
        }
    }

    return GoogleSignInLauncher {
        callbacksState.value.onBusy(true)
        // Credential Manager fails on many devices with a framework error and a 20s hang.
        launchLegacy()
    }
}

internal fun toastLegacyLaunchFailure(
    context: android.content.Context,
    err: Throwable,
    onBusy: (Boolean) -> Unit,
) {
    val detail = if (err.message == GOOGLE_SIGN_IN_NO_ACTIVITY) {
        context.getString(R.string.login_google_need_activity)
    } else {
        context.getString(R.string.login_google_error, err.message ?: err.toString())
    }
    Toast.makeText(context, detail, Toast.LENGTH_LONG).show()
    onBusy(false)
}

internal fun decodeGoogleIdToken(idToken: String): JSONObject? {
    return try {
        val parts = idToken.split(".")
        if (parts.size != 3) return null
        val payload = String(
            Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP),
        )
        JSONObject(payload)
    } catch (e: Exception) {
        Log.w("TL", "swallowed", e)
        null
    }
}

internal fun resolveGooglePhotoUrl(
    primary: String?,
    idToken: String? = null,
    accountPhotoUrl: String? = null,
): String? {
    primary?.takeIf { it.isNotBlank() }?.let { return it }
    idToken?.let { token ->
        decodeGoogleIdToken(token)?.optString("picture")?.takeIf { it.isNotBlank() }?.let { return it }
    }
    return accountPhotoUrl?.takeIf { it.isNotBlank() }
}

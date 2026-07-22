package com.truckerload.presentation.auth

import android.app.Activity
import android.util.Base64
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
import com.truckerload.data.preferences.AccountIds
import com.truckerload.data.preferences.AuthSession
import com.truckerload.data.preferences.UserProfile
import com.truckerload.data.remote.CredentialManagerGoogleSignIn
import com.truckerload.data.remote.SupabaseAuthService
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

    fun saveAndFinish(
        email: String,
        givenName: String,
        familyName: String,
        photoUrl: String?,
        phoneNumber: String? = null,
        supabaseUserId: String? = null,
        accessToken: String? = null,
        refreshToken: String? = null,
    ) {
        AuthSession.completeLogin(
            authStore = authStore,
            userProfileStore = userProfileStore,
            userId = AccountIds.resolve(supabaseUserId, email),
            profile = UserProfile(
                email = email,
                givenName = givenName,
                familyName = familyName,
                photoUrl = photoUrl,
                phoneNumber = phoneNumber?.takeIf { it.isNotBlank() },
            ),
            rememberMe = true,
            accessToken = accessToken,
            refreshToken = refreshToken,
        )
        callbacksState.value.onBusy(false)
        callbacksState.value.onSignedIn()
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
            legacyLauncher.launch(GoogleSignIn.getClient(context, gso).signInIntent)
            return@GoogleSignInLauncher
        }
        scope.launch {
            val tokenResult = CredentialManagerGoogleSignIn.getGoogleIdToken(context)
            val idToken = tokenResult.getOrNull()
            if (idToken != null) {
                withContext(Dispatchers.Main) {
                    val claims = decodeGoogleIdToken(idToken)
                    if (supabaseAuth.isConfigured()) {
                        try {
                            val authResult = supabaseAuth.signInWithIdToken(idToken)
                            val signIn = authResult.getOrNull()
                            val user = signIn?.user
                            if (user != null) {
                                val parts = (user.fullName ?: user.email?.take(10) ?: "User")
                                    .trim()
                                    .split(" ")
                                saveAndFinish(
                                    user.email ?: claims?.optString("email").orEmpty(),
                                    parts.firstOrNull() ?: claims?.optString("given_name").orEmpty(),
                                    parts.drop(1).joinToString(" ").ifBlank {
                                        claims?.optString("family_name").orEmpty()
                                    },
                                    resolveGooglePhotoUrl(user.avatarUrl, idToken),
                                    supabaseUserId = user.id,
                                    accessToken = signIn.accessToken,
                                    refreshToken = signIn.refreshToken,
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
                                saveAndFinish(
                                    claims?.optString("email").orEmpty(),
                                    claims?.optString("given_name").orEmpty(),
                                    claims?.optString("family_name").orEmpty(),
                                    resolveGooglePhotoUrl(null, idToken),
                                )
                            }
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.login_google_fallback, e.message.orEmpty()),
                                Toast.LENGTH_LONG,
                            ).show()
                            saveAndFinish(
                                claims?.optString("email").orEmpty(),
                                claims?.optString("given_name").orEmpty(),
                                claims?.optString("family_name").orEmpty(),
                                resolveGooglePhotoUrl(null, idToken),
                            )
                        }
                    } else {
                        saveAndFinish(
                            claims?.optString("email").orEmpty(),
                            claims?.optString("given_name").orEmpty(),
                            claims?.optString("family_name").orEmpty(),
                            resolveGooglePhotoUrl(null, idToken),
                        )
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    when (tokenResult.exceptionOrNull()) {
                        is GetCredentialCancellationException -> {
                            Toast.makeText(
                                context,
                                context.getString(R.string.login_google_cancelled),
                                Toast.LENGTH_SHORT,
                            ).show()
                            callbacksState.value.onBusy(false)
                        }
                        else -> {
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
                            legacyLauncher.launch(GoogleSignIn.getClient(context, gso).signInIntent)
                        }
                    }
                }
            }
        }
    }
}

internal fun decodeGoogleIdToken(idToken: String): JSONObject? {
    return try {
        val parts = idToken.split(".")
        if (parts.size != 3) return null
        val payload = String(
            Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP),
        )
        JSONObject(payload)
    } catch (_: Exception) {
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

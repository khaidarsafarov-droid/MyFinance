package com.truckerload.presentation.screens.login

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.truckerload.BuildConfig
import com.truckerload.R
import com.truckerload.data.preferences.AuthLogin
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.UserProfile
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.remote.CredentialManagerGoogleSignIn
import com.truckerload.data.remote.SupabaseAuthService
import com.truckerload.presentation.components.GoogleSignInButton
import com.truckerload.presentation.di.LocalAuthStore
import com.truckerload.presentation.di.LocalUserProfileStore
import com.truckerload.presentation.theme.BentoGlassScreenBackground
import com.truckerload.presentation.theme.LocalTruckColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private fun decodeGoogleIdToken(idToken: String): JSONObject? {
    return try {
        val parts = idToken.split(".")
        if (parts.size != 3) return null
        val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP))
        JSONObject(payload)
    } catch (e: Exception) {
        android.util.Log.w("TL", "swallowed", e)
        null
    }
}

/** Prefer Supabase/metadata avatar, then Google ID-token picture, then legacy account photo. */
private fun resolveGooglePhotoUrl(
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

private fun saveProfileAndLogin(
    email: String,
    givenName: String,
    familyName: String,
    photoUrl: String?,
    context: android.content.Context,
    userProfileStore: UserProfileStore,
    authStore: AuthStore,
    rememberMe: Boolean = true,
    phoneNumber: String? = null,
    supabaseUserId: String? = null,
    accessToken: String? = null,
    refreshToken: String? = null,
    googleId: String? = null,
    onSignedIn: () -> Unit = {},
) {
    val profile = UserProfile(
        email = email,
        givenName = givenName,
        familyName = familyName,
        photoUrl = photoUrl,
        phoneNumber = phoneNumber?.takeIf { it.isNotBlank() },
        googleId = googleId?.takeIf { it.isNotBlank() },
    )
    val finish = {
        val ok = AuthLogin.tryCompleteLogin(
            authStore = authStore,
            userProfileStore = userProfileStore,
            supabaseUserId = supabaseUserId,
            profile = profile,
            rememberMe = rememberMe,
            accessToken = accessToken,
            refreshToken = refreshToken,
        )
        if (!ok) {
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.auth_error_email_required),
                android.widget.Toast.LENGTH_LONG,
            ).show()
        } else {
            onSignedIn()
        }
    }
    val activity = context as? ComponentActivity
    if (activity != null) {
        activity.lifecycleScope.launch {
            delay(400)
            withContext(Dispatchers.Main) { finish() }
        }
    } else {
        Handler(Looper.getMainLooper()).postDelayed({ finish() }, 400)
    }
}

/**
 * Android entry screen: Google Sign-In only.
 * Apple / iCloud Sign in with Apple will be the iOS counterpart later.
 */
@Composable
fun LoginScreen(
    onSignedIn: () -> Unit = {},
) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val authStore = LocalAuthStore.current
    val userProfileStore = LocalUserProfileStore.current
    val scope = rememberCoroutineScope()
    val supabaseAuth = remember(context) { SupabaseAuthService(context.applicationContext) }
    var isLoading by remember { mutableStateOf(false) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            isLoading = false
            return@rememberLauncherForActivityResult
        }
        val data = result.data ?: run {
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.login_google_error, context.getString(R.string.login_google_no_data)),
                android.widget.Toast.LENGTH_SHORT,
            ).show()
            isLoading = false
            return@rememberLauncherForActivityResult
        }
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        task.addOnSuccessListener { account ->
            try {
                val idToken = account.idToken
                fun signInWithGoogleIdentity() {
                    saveProfileAndLogin(
                        account.email ?: "",
                        account.givenName ?: "",
                        account.familyName ?: "",
                        resolveGooglePhotoUrl(null, idToken, account.photoUrl?.toString()),
                        context,
                        userProfileStore,
                        authStore,
                        googleId = account.id ?: decodeGoogleIdToken(idToken.orEmpty())?.optString("sub"),
                        onSignedIn = onSignedIn,
                    )
                    isLoading = false
                }
                if (supabaseAuth.isConfigured() && !idToken.isNullOrBlank()) {
                    scope.launch {
                        try {
                            val authResult = supabaseAuth.signInWithIdToken(idToken)
                            withContext(Dispatchers.Main) {
                                val signInResult = authResult.getOrNull()
                                if (signInResult != null) {
                                    val u = signInResult.user
                                    val parts = (u.fullName ?: "${u.email?.take(10) ?: "User"}").trim().split(" ")
                                    saveProfileAndLogin(
                                        u.email ?: account.email ?: "",
                                        parts.firstOrNull() ?: account.givenName ?: "",
                                        parts.drop(1).joinToString(" ").ifBlank { account.familyName ?: "" },
                                        resolveGooglePhotoUrl(u.avatarUrl, idToken, account.photoUrl?.toString()),
                                        context,
                                        userProfileStore,
                                        authStore,
                                        supabaseUserId = u.id,
                                        accessToken = signInResult.accessToken,
                                        refreshToken = signInResult.refreshToken,
                                        googleId = account.id ?: decodeGoogleIdToken(idToken)?.optString("sub"),
                                        onSignedIn = onSignedIn,
                                    )
                                } else {
                                    android.widget.Toast.makeText(
                                        context,
                                        context.getString(
                                            R.string.login_google_fallback,
                                            authResult.exceptionOrNull()?.message ?: "",
                                        ),
                                        android.widget.Toast.LENGTH_LONG,
                                    ).show()
                                    signInWithGoogleIdentity()
                                }
                                isLoading = false
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                android.widget.Toast.makeText(
                                    context,
                                    context.getString(R.string.login_google_fallback, e.message ?: ""),
                                    android.widget.Toast.LENGTH_LONG,
                                ).show()
                                signInWithGoogleIdentity()
                            }
                        }
                    }
                } else {
                    signInWithGoogleIdentity()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.login_google_error, e.message ?: e.toString()),
                    android.widget.Toast.LENGTH_LONG,
                ).show()
                isLoading = false
            }
        }
        task.addOnFailureListener {
            android.widget.Toast.makeText(
                context,
                context.getString(
                    R.string.login_google_error,
                    (it as? ApiException)?.message ?: it.message ?: it.toString(),
                ),
                android.widget.Toast.LENGTH_LONG,
            ).show()
            isLoading = false
        }
    }

    fun launchLegacyGoogleSignIn() {
        val gso = if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()) {
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail().requestProfile().requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID).build()
        } else {
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail().requestProfile().build()
        }
        googleSignInLauncher.launch(GoogleSignIn.getClient(context, gso).signInIntent)
    }

    fun launchGoogleSignIn() {
        isLoading = true
        if (!CredentialManagerGoogleSignIn.isAvailable()) {
            launchLegacyGoogleSignIn()
            return
        }
        scope.launch {
            val tokenResult = CredentialManagerGoogleSignIn.getGoogleIdToken(context)
            val idToken = tokenResult.getOrNull()
            if (idToken != null) {
                withContext(Dispatchers.Main) {
                    if (supabaseAuth.isConfigured()) {
                        try {
                            val authResult = supabaseAuth.signInWithIdToken(idToken)
                            val signInResult = authResult.getOrNull()
                            if (signInResult != null) {
                                val u = signInResult.user
                                val claims = decodeGoogleIdToken(idToken)
                                val parts = (u.fullName ?: u.email?.take(10) ?: "User").trim().split(" ")
                                saveProfileAndLogin(
                                    u.email ?: claims?.optString("email").orEmpty(),
                                    parts.firstOrNull() ?: claims?.optString("given_name").orEmpty(),
                                    parts.drop(1).joinToString(" ").ifBlank {
                                        claims?.optString("family_name").orEmpty()
                                    },
                                    resolveGooglePhotoUrl(u.avatarUrl, idToken),
                                    context,
                                    userProfileStore,
                                    authStore,
                                    supabaseUserId = u.id,
                                    accessToken = signInResult.accessToken,
                                    refreshToken = signInResult.refreshToken,
                                    googleId = claims?.optString("sub"),
                                    onSignedIn = onSignedIn,
                                )
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    context.getString(
                                        R.string.login_google_fallback,
                                        authResult.exceptionOrNull()?.message ?: "",
                                    ),
                                    android.widget.Toast.LENGTH_LONG,
                                ).show()
                                val c = decodeGoogleIdToken(idToken)
                                saveProfileAndLogin(
                                    c?.optString("email") ?: "",
                                    c?.optString("given_name") ?: "",
                                    c?.optString("family_name") ?: "",
                                    resolveGooglePhotoUrl(null, idToken),
                                    context,
                                    userProfileStore,
                                    authStore,
                                    googleId = c?.optString("sub"),
                                    onSignedIn = onSignedIn,
                                )
                            }
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.login_google_fallback, e.message ?: ""),
                                android.widget.Toast.LENGTH_LONG,
                            ).show()
                            val c = decodeGoogleIdToken(idToken)
                            saveProfileAndLogin(
                                c?.optString("email") ?: "",
                                c?.optString("given_name") ?: "",
                                c?.optString("family_name") ?: "",
                                resolveGooglePhotoUrl(null, idToken),
                                context,
                                userProfileStore,
                                authStore,
                                googleId = c?.optString("sub"),
                                onSignedIn = onSignedIn,
                            )
                        }
                    } else {
                        val c = decodeGoogleIdToken(idToken)
                        saveProfileAndLogin(
                            c?.optString("email") ?: "",
                            c?.optString("given_name") ?: "",
                            c?.optString("family_name") ?: "",
                            resolveGooglePhotoUrl(null, idToken),
                            context,
                            userProfileStore,
                            authStore,
                            googleId = c?.optString("sub"),
                            onSignedIn = onSignedIn,
                        )
                    }
                    isLoading = false
                }
            } else {
                withContext(Dispatchers.Main) {
                    when (tokenResult.exceptionOrNull()) {
                        is GetCredentialCancellationException -> {
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.login_google_cancelled),
                                android.widget.Toast.LENGTH_SHORT,
                            ).show()
                            isLoading = false
                        }
                        else -> launchLegacyGoogleSignIn()
                    }
                }
            }
        }
    }

    BentoGlassScreenBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.login_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = tc.TextPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.login_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = tc.TextSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(32.dp))
                GoogleSignInButton(
                    onClick = { launchGoogleSignIn() },
                    enabled = !isLoading,
                    loading = isLoading,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.login_auth_only_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = tc.AccentPrimary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.login_checking),
                            style = MaterialTheme.typography.bodyMedium,
                            color = tc.TextSecondary,
                        )
                    }
                }
            }
        }
    }
}

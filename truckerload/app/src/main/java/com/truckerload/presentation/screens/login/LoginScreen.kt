package com.truckerload.presentation.screens.login

import android.app.Activity
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.GoogleSignInButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.truckerload.BuildConfig
import com.truckerload.R
import android.util.Base64
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.truckerload.data.preferences.AuthLogin
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.UserProfile
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.remote.CredentialManagerGoogleSignIn
import com.truckerload.data.remote.SupabaseAuthService
import com.truckerload.presentation.di.LocalAuthStore
import com.truckerload.presentation.di.LocalUserProfileStore
import com.truckerload.presentation.theme.AppTextFieldDefaults
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
    } catch (_: Exception) { null }
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
) {
    val profile = UserProfile(
        email = email,
        givenName = givenName,
        familyName = familyName,
        photoUrl = photoUrl,
        phoneNumber = phoneNumber?.takeIf { it.isNotBlank() },
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

@Composable
fun LoginScreen(
    onCreateAccount: () -> Unit
) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val authStore = LocalAuthStore.current
    val userProfileStore = LocalUserProfileStore.current
    val scope = rememberCoroutineScope()
    val supabaseAuth = remember(context) { SupabaseAuthService(context.applicationContext) }
    var isLoading by remember { mutableStateOf(false) }
    var showEmailFields by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var rememberMe by remember { mutableStateOf(true) }
    val emailFocus = remember { FocusRequester() }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) { isLoading = false; return@rememberLauncherForActivityResult }
        val data = result.data ?: run {
            android.widget.Toast.makeText(context, context.getString(R.string.login_google_error, context.getString(R.string.login_google_no_data)), android.widget.Toast.LENGTH_SHORT).show()
            isLoading = false
            return@rememberLauncherForActivityResult
        }
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        task.addOnSuccessListener { account ->
            try {
                val idToken = account.idToken
                fun signInLocally() {
                    saveProfileAndLogin(
                        account.email ?: "",
                        account.givenName ?: "",
                        account.familyName ?: "",
                        resolveGooglePhotoUrl(null, idToken, account.photoUrl?.toString()),
                        context,
                        userProfileStore,
                        authStore,
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
                                    )
                                } else {
                                    android.widget.Toast.makeText(context, context.getString(R.string.login_google_fallback, authResult.exceptionOrNull()?.message ?: ""), android.widget.Toast.LENGTH_LONG).show()
                                    signInLocally()
                                }
                                isLoading = false
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                android.widget.Toast.makeText(context, context.getString(R.string.login_google_fallback, e.message ?: ""), android.widget.Toast.LENGTH_LONG).show()
                                signInLocally()
                            }
                        }
                    }
                } else { signInLocally() }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, context.getString(R.string.login_google_error, e.message ?: e.toString()), android.widget.Toast.LENGTH_LONG).show()
                isLoading = false
            }
        }
        task.addOnFailureListener {
            android.widget.Toast.makeText(context, context.getString(R.string.login_google_error, (it as? ApiException)?.message ?: it.message ?: it.toString()), android.widget.Toast.LENGTH_LONG).show()
            isLoading = false
        }
    }

    fun launchLegacyGoogleSignIn() {
        val gso = if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()) {
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail().requestProfile().requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID).build()
        } else {
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().requestProfile().build()
        }
        googleSignInLauncher.launch(GoogleSignIn.getClient(context, gso).signInIntent)
    }

    fun launchGoogleSignIn() {
        isLoading = true
        if (!CredentialManagerGoogleSignIn.isAvailable()) {
            // No web client ID — still try the legacy Google Sign-In activity.
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
                                    parts.drop(1).joinToString(" ").ifBlank { claims?.optString("family_name").orEmpty() },
                                    resolveGooglePhotoUrl(u.avatarUrl, idToken),
                                    context,
                                    userProfileStore,
                                    authStore,
                                    supabaseUserId = u.id,
                                    accessToken = signInResult.accessToken,
                                    refreshToken = signInResult.refreshToken,
                                )
                            } else {
                                android.widget.Toast.makeText(context, context.getString(R.string.login_google_fallback, authResult.exceptionOrNull()?.message ?: ""), android.widget.Toast.LENGTH_LONG).show()
                                val c = decodeGoogleIdToken(idToken)
                                saveProfileAndLogin(c?.optString("email") ?: "", c?.optString("given_name") ?: "", c?.optString("family_name") ?: "", resolveGooglePhotoUrl(null, idToken), context, userProfileStore, authStore)
                            }
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, context.getString(R.string.login_google_fallback, e.message ?: ""), android.widget.Toast.LENGTH_LONG).show()
                            val c = decodeGoogleIdToken(idToken)
                            saveProfileAndLogin(c?.optString("email") ?: "", c?.optString("given_name") ?: "", c?.optString("family_name") ?: "", resolveGooglePhotoUrl(null, idToken), context, userProfileStore, authStore)
                        }
                    } else {
                        val c = decodeGoogleIdToken(idToken)
                        saveProfileAndLogin(c?.optString("email") ?: "", c?.optString("given_name") ?: "", c?.optString("family_name") ?: "", resolveGooglePhotoUrl(null, idToken), context, userProfileStore, authStore)
                    }
                    isLoading = false
                }
            } else {
                withContext(Dispatchers.Main) {
                    when (tokenResult.exceptionOrNull()) {
                        is GetCredentialCancellationException -> {
                            android.widget.Toast.makeText(context, context.getString(R.string.login_google_cancelled), android.widget.Toast.LENGTH_SHORT).show()
                            isLoading = false
                        }
                        else -> {
                            // Fall back to the classic Google account picker.
                            launchLegacyGoogleSignIn()
                        }
                    }
                }
            }
        }
    }

    fun performEmailLogin() {
        error = null
        val emailTrimmed = email.trim()
        when {
            emailTrimmed.isBlank() -> error = context.getString(R.string.auth_error_email_required)
            password.isBlank() -> error = context.getString(R.string.auth_error_password_required)
            password.length < 6 -> error = context.getString(R.string.auth_error_password_short)
            !supabaseAuth.isConfigured() -> {
                android.widget.Toast.makeText(context, context.getString(R.string.supabase_not_configured_local), android.widget.Toast.LENGTH_LONG).show()
                saveProfileAndLogin(
                    emailTrimmed, "", "", null,
                    context, userProfileStore, authStore, rememberMe,
                )
            }
            else -> {
                isLoading = true
                scope.launch {
                    val result = supabaseAuth.signInWithPassword(emailTrimmed, password)
                    result.fold(
                        onSuccess = { r ->
                            val profileResult = supabaseAuth.getProfile(r.accessToken, r.user.id)
                            withContext(Dispatchers.Main) {
                                isLoading = false
                                profileResult.fold(
                                    onSuccess = { profile ->
                                        val fullName = profile.fullName ?: r.user.fullName
                                        val parts = (fullName ?: "").trim().split(" ", limit = 2)
                                        saveProfileAndLogin(
                                            r.user.email ?: profile.email ?: emailTrimmed,
                                            parts.firstOrNull() ?: "",
                                            parts.getOrNull(1) ?: "",
                                            null,
                                            context,
                                            userProfileStore,
                                            authStore,
                                            rememberMe,
                                            phoneNumber = profile.phoneNumber,
                                            supabaseUserId = r.user.id,
                                            accessToken = r.accessToken,
                                            refreshToken = r.refreshToken,
                                        )
                                    },
                                    onFailure = {
                                        val parts = (r.user.fullName ?: "").trim().split(" ", limit = 2)
                                        saveProfileAndLogin(
                                            r.user.email ?: emailTrimmed,
                                            parts.firstOrNull() ?: "",
                                            parts.getOrNull(1) ?: "",
                                            null,
                                            context,
                                            userProfileStore,
                                            authStore,
                                            rememberMe,
                                            supabaseUserId = r.user.id,
                                            accessToken = r.accessToken,
                                            refreshToken = r.refreshToken,
                                        )
                                    }
                                )
                            }
                        },
                        onFailure = {
                            withContext(Dispatchers.Main) {
                                isLoading = false
                                error = it.message ?: context.getString(R.string.auth_error_login_invalid)
                            }
                        }
                    )
                }
            }
        }
    }

    BentoGlassScreenBackground {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(48.dp))
                Text(text = stringResource(R.string.login_title), style = MaterialTheme.typography.headlineLarge, color = tc.TextPrimary, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = stringResource(R.string.login_subtitle), style = MaterialTheme.typography.bodyLarge, color = tc.TextSecondary, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(32.dp))

                GoogleSignInButton(
                    onClick = { launchGoogleSignIn() },
                    enabled = !isLoading,
                    loading = isLoading && !showEmailFields,
                )

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = tc.TextSecondary.copy(alpha = 0.35f))
                    Text(
                        text = stringResource(R.string.login_or_divider),
                        style = MaterialTheme.typography.bodySmall,
                        color = tc.TextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = tc.TextSecondary.copy(alpha = 0.35f))
                }
                Spacer(modifier = Modifier.height(20.dp))

                if (!showEmailFields) {
                    Button(
                        onClick = { showEmailFields = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = !isLoading,
                    ) { Text(stringResource(R.string.login_with_email)) }
                } else {
                    AnimatedVisibility(visible = showEmailFields, enter = expandVertically(), exit = shrinkVertically()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            LaunchedEffect(Unit) { emailFocus.requestFocus() }
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it; error = null },
                                label = { Text(stringResource(R.string.auth_email_hint)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().focusRequester(emailFocus),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                colors = AppTextFieldDefaults.outlined()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it; error = null },
                                label = { Text(stringResource(R.string.auth_password_hint)) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                colors = AppTextFieldDefaults.outlined()
                            )
                            error?.let { Text(text = it, color = tc.AccentExpense, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)) }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = rememberMe,
                                    onCheckedChange = { rememberMe = it },
                                    colors = CheckboxDefaults.colors(checkedColor = tc.AccentPrimary)
                                )
                                Text(stringResource(R.string.auth_remember_me), style = MaterialTheme.typography.bodyMedium, color = tc.TextSecondary, modifier = Modifier.clickable { rememberMe = !rememberMe })
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { if (!isLoading) performEmailLogin() },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                enabled = !isLoading,
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = tc.Background)
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                Text(if (isLoading) stringResource(R.string.login_checking) else stringResource(R.string.login_button))
                            }
                        }
                    }
                }
            }
            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.login_no_account_prefix))
                    withStyle(SpanStyle(color = tc.AccentPrimary, fontWeight = FontWeight.Medium)) { append(stringResource(R.string.login_create_account)) }
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 32.dp).clickable(enabled = !isLoading) { onCreateAccount() }
            )
        }
        if (isLoading && !showEmailFields) {
            Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = tc.AccentPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.login_checking), style = MaterialTheme.typography.bodyMedium, color = tc.TextSecondary)
                }
            }
        }
    }
    }
}

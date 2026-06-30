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
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
import com.truckerload.data.preferences.UserProfile
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.remote.CredentialManagerGoogleSignIn
import com.truckerload.data.remote.SupabaseAuthService
import com.truckerload.presentation.di.LocalAuthStore
import com.truckerload.presentation.di.LocalUserProfileStore
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

private fun scheduleLoginAfterTransition(context: android.content.Context, authStore: AuthStore, rememberMe: Boolean = true) {
    val activity = context as? ComponentActivity
    if (activity != null) {
        activity.lifecycleScope.launch {
            delay(400)
            withContext(Dispatchers.Main) { authStore.login(rememberMe) }
        }
    } else {
        Handler(Looper.getMainLooper()).postDelayed({ authStore.login(rememberMe) }, 400)
    }
}

private fun saveProfileAndLogin(
    email: String, givenName: String, familyName: String, photoUrl: String?,
    context: android.content.Context, userProfileStore: UserProfileStore, authStore: AuthStore,
    rememberMe: Boolean = true
) {
    userProfileStore.saveProfile(UserProfile(email = email, givenName = givenName, familyName = familyName, photoUrl = photoUrl))
    scheduleLoginAfterTransition(context, authStore, rememberMe)
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
    val supabaseAuth = remember { SupabaseAuthService() }
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
            android.widget.Toast.makeText(context, context.getString(R.string.login_google_error, "Нет данных"), android.widget.Toast.LENGTH_SHORT).show()
            isLoading = false
            return@rememberLauncherForActivityResult
        }
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        task.addOnSuccessListener { account ->
            try {
                val idToken = account.idToken
                fun signInLocally() {
                    saveProfileAndLogin(account.email ?: "", account.givenName ?: "", account.familyName ?: "", account.photoUrl?.toString(), context, userProfileStore, authStore)
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
                                    saveProfileAndLogin(u.email ?: "", parts.firstOrNull() ?: "", parts.drop(1).joinToString(" "), u.avatarUrl, context, userProfileStore, authStore)
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
        if (!CredentialManagerGoogleSignIn.isAvailable()) {
            android.widget.Toast.makeText(context, context.getString(R.string.login_google_error, "GOOGLE_WEB_CLIENT_ID не задан"), android.widget.Toast.LENGTH_LONG).show()
            return
        }
        isLoading = true
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
                                val parts = (u.fullName ?: u.email?.take(10) ?: "User").trim().split(" ")
                                saveProfileAndLogin(u.email ?: "", parts.firstOrNull() ?: "", parts.drop(1).joinToString(" "), u.avatarUrl, context, userProfileStore, authStore)
                            } else {
                                android.widget.Toast.makeText(context, context.getString(R.string.login_google_fallback, authResult.exceptionOrNull()?.message ?: ""), android.widget.Toast.LENGTH_LONG).show()
                                val c = decodeGoogleIdToken(idToken)
                                saveProfileAndLogin(c?.optString("email") ?: "", c?.optString("given_name") ?: "", c?.optString("family_name") ?: "", c?.optString("picture")?.takeIf { it.isNotBlank() }, context, userProfileStore, authStore)
                            }
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, context.getString(R.string.login_google_fallback, e.message ?: ""), android.widget.Toast.LENGTH_LONG).show()
                            val c = decodeGoogleIdToken(idToken)
                            saveProfileAndLogin(c?.optString("email") ?: "", c?.optString("given_name") ?: "", c?.optString("family_name") ?: "", c?.optString("picture")?.takeIf { it.isNotBlank() }, context, userProfileStore, authStore)
                        }
                    } else {
                        val c = decodeGoogleIdToken(idToken)
                        saveProfileAndLogin(c?.optString("email") ?: "", c?.optString("given_name") ?: "", c?.optString("family_name") ?: "", c?.optString("picture")?.takeIf { it.isNotBlank() }, context, userProfileStore, authStore)
                    }
                    isLoading = false
                }
            } else {
                withContext(Dispatchers.Main) {
                    when (tokenResult.exceptionOrNull()) {
                        is GetCredentialCancellationException ->
                            android.widget.Toast.makeText(context, "Credential Manager отменён.", android.widget.Toast.LENGTH_LONG).show()
                        else ->
                            android.widget.Toast.makeText(context, context.getString(R.string.login_google_error, "Unknown"), android.widget.Toast.LENGTH_LONG).show()
                    }
                    launchLegacyGoogleSignIn()
                    isLoading = false
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
                android.widget.Toast.makeText(context, "Supabase не настроен. Вход выполнен локально.", android.widget.Toast.LENGTH_LONG).show()
                userProfileStore.saveProfile(UserProfile(email = emailTrimmed, givenName = "", familyName = "", photoUrl = null))
                scheduleLoginAfterTransition(context, authStore, rememberMe)
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
                                            rememberMe
                                        )
                                    },
                                    onFailure = {
                                        val parts = (r.user.fullName ?: "").trim().split(" ", limit = 2)
                                        saveProfileAndLogin(r.user.email ?: emailTrimmed, parts.firstOrNull() ?: "", parts.getOrNull(1) ?: "", null, context, userProfileStore, authStore, rememberMe)
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

                if (!showEmailFields) {
                    Button(
                        onClick = { showEmailFields = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = tc.AccentPrimary)
                    ) { Text(stringResource(R.string.login_button)) }
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
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = tc.AccentPrimary, unfocusedBorderColor = tc.Divider,
                                    focusedLabelColor = tc.AccentPrimary, unfocusedLabelColor = tc.TextSecondary,
                                    focusedTextColor = tc.TextPrimary, unfocusedTextColor = tc.TextPrimary, cursorColor = tc.AccentPrimary
                                )
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
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = tc.AccentPrimary, unfocusedBorderColor = tc.Divider,
                                    focusedLabelColor = tc.AccentPrimary, unfocusedLabelColor = tc.TextSecondary,
                                    focusedTextColor = tc.TextPrimary, unfocusedTextColor = tc.TextPrimary, cursorColor = tc.AccentPrimary
                                )
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
                                colors = ButtonDefaults.buttonColors(containerColor = tc.AccentPrimary)
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
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { launchGoogleSignIn() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = !isLoading
                ) {
                    Image(painter = painterResource(R.drawable.ic_google), contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.login_with_google))
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

package com.truckerload.presentation.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.data.preferences.AccountIds
import com.truckerload.data.preferences.AuthLogin
import com.truckerload.data.preferences.UserProfile
import com.truckerload.data.remote.SupabaseAuthService
import com.truckerload.domain.geo.CountryCatalog
import com.truckerload.presentation.auth.BiometricOptInDialog
import com.truckerload.presentation.auth.GoogleAuthCallbacks
import com.truckerload.presentation.auth.enableBiometricUnlock
import com.truckerload.presentation.auth.rememberGoogleSignInLauncher
import com.truckerload.presentation.auth.shouldOfferBiometricUnlock
import com.truckerload.presentation.components.GoogleSignInButton
import com.truckerload.presentation.components.PhoneWithCountryField
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.di.LocalAuthCredentialsStore
import com.truckerload.presentation.di.LocalAuthStore
import com.truckerload.presentation.di.LocalUserProfileStore
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.BentoGlassScreenBackground
import com.truckerload.presentation.theme.LocalTruckColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val authStore = LocalAuthStore.current
    val userProfileStore = LocalUserProfileStore.current
    val credentialsStore = LocalAuthCredentialsStore.current
    val supabaseAuth = remember(context) { SupabaseAuthService(context.applicationContext) }
    val scope = rememberCoroutineScope()

    var fullName by remember { mutableStateOf("") }
    var phoneCountry by remember { mutableStateOf(CountryCatalog.default) }
    var nationalNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }

    val googleSignIn = rememberGoogleSignInLauncher(
        GoogleAuthCallbacks(
            onBusy = { isGoogleLoading = it },
            onSignedIn = onSuccess,
        ),
    )

    var showBiometricOffer by remember { mutableStateOf(false) }

    fun completeSignUp() {
        if (shouldOfferBiometricUnlock(context)) {
            showBiometricOffer = true
        } else {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ onSuccess() }, 400)
        }
    }

    fun finishLocalSignUp(
        emailTrimmed: String,
        passwordValue: String,
        nameTrimmed: String,
        phoneFormatted: String,
        toastRes: Int,
    ) {
        // Never abort session creation if credential persist fails — user must stay signed in.
        runCatching { credentialsStore.saveCredentials(emailTrimmed, passwordValue) }
            .onFailure { android.util.Log.w("SignUp", "Failed to persist local credentials", it) }
        AuthLogin.completeLogin(
            authStore = authStore,
            userProfileStore = userProfileStore,
            userId = AccountIds.fromEmail(emailTrimmed),
            profile = UserProfile(
                email = emailTrimmed,
                givenName = nameTrimmed,
                familyName = "",
                photoUrl = null,
                phoneNumber = phoneFormatted,
            ),
        )
        // No outbound email without Supabase — treat on-device accounts as verified.
        com.truckerload.data.preferences.EmailVerificationStore(context)
            .markVerified(emailTrimmed)
        android.widget.Toast.makeText(context, context.getString(toastRes), android.widget.Toast.LENGTH_LONG).show()
        completeSignUp()
    }

    fun finishCloudSignUp(
        emailTrimmed: String,
        passwordValue: String,
        userId: String,
        givenName: String,
        familyName: String,
        phoneFormatted: String,
        accessToken: String,
        refreshToken: String,
    ) {
        runCatching { credentialsStore.saveCredentials(emailTrimmed, passwordValue) }
            .onFailure { android.util.Log.w("SignUp", "Failed to persist credentials after cloud signup", it) }
        AuthLogin.completeLogin(
            authStore = authStore,
            userProfileStore = userProfileStore,
            userId = userId,
            profile = UserProfile(
                email = emailTrimmed,
                givenName = givenName,
                familyName = familyName,
                photoUrl = null,
                phoneNumber = phoneFormatted,
            ),
            accessToken = accessToken,
            refreshToken = refreshToken,
        )
        com.truckerload.data.preferences.EmailVerificationStore(context)
            .beginVerification(emailTrimmed)
        completeSignUp()
    }

    fun performSignUp() {
        error = null
        val nameTrimmed = fullName.trim()
        val emailTrimmed = email.trim()
        val phoneFormatted = CountryCatalog.formatE164(phoneCountry, nationalNumber)
        val phoneDigits = phoneFormatted.filter { it.isDigit() }
        when {
            nameTrimmed.isBlank() -> error = context.getString(R.string.auth_error_name_required)
            phoneDigits.length < 8 -> error = context.getString(R.string.auth_error_phone_required)
            emailTrimmed.isBlank() -> error = context.getString(R.string.auth_error_email_required)
            else -> {
                val policy = com.truckerload.data.auth.PasswordPolicy.validate(password)
                if (!policy.ok) {
                    error = context.getString(policy.errorResId)
                    return
                }
                if (!supabaseAuth.isConfigured()) {
                    finishLocalSignUp(
                        emailTrimmed,
                        password,
                        nameTrimmed,
                        phoneFormatted,
                        R.string.supabase_not_configured_local,
                    )
                    return
                }
                isLoading = true
                scope.launch {
                    val checkResult = supabaseAuth.checkRegistration(emailTrimmed, phoneFormatted)
                    val shouldProceed = checkResult.fold(
                        onSuccess = { (emailTaken, phoneTaken) ->
                            if (emailTaken || phoneTaken) {
                                withContext(Dispatchers.Main) {
                                    isLoading = false
                                    error = context.getString(R.string.auth_error_user_exists)
                                }
                                false
                            } else true
                        },
                        onFailure = { true },
                    )
                    if (!shouldProceed) return@launch
                    val signUpResult = supabaseAuth.signUp(emailTrimmed, password, nameTrimmed, phoneFormatted)
                    withContext(Dispatchers.Main) {
                        signUpResult.fold(
                            onSuccess = { r ->
                                val parts = nameTrimmed.split(" ", limit = 2)
                                if (r.accessToken.isNotBlank()) {
                                    scope.launch {
                                        val upsertResult = supabaseAuth.upsertProfile(
                                            r.accessToken,
                                            r.user.id,
                                            nameTrimmed,
                                            phoneFormatted,
                                            r.user.email ?: emailTrimmed,
                                        )
                                        withContext(Dispatchers.Main) {
                                            isLoading = false
                                            // Profile upsert is best-effort: auth already succeeded.
                                            // Blocking login here left orphan Supabase users who could
                                            // neither finish signup nor sign in locally.
                                            upsertResult.onFailure {
                                                android.util.Log.w("SignUp", "Profile upsert failed", it)
                                                android.widget.Toast.makeText(
                                                    context,
                                                    it.message
                                                        ?: context.getString(R.string.signup_error_profile_save),
                                                    android.widget.Toast.LENGTH_LONG,
                                                ).show()
                                            }
                                            finishCloudSignUp(
                                                emailTrimmed = r.user.email ?: emailTrimmed,
                                                passwordValue = password,
                                                userId = r.user.id,
                                                givenName = parts.firstOrNull() ?: "",
                                                familyName = parts.getOrNull(1) ?: "",
                                                phoneFormatted = phoneFormatted,
                                                accessToken = r.accessToken,
                                                refreshToken = r.refreshToken,
                                            )
                                        }
                                    }
                                } else {
                                    isLoading = false
                                    finishLocalSignUp(
                                        emailTrimmed,
                                        password,
                                        nameTrimmed,
                                        phoneFormatted,
                                        R.string.signup_success_confirm_email,
                                    )
                                }
                            },
                            onFailure = { err ->
                                isLoading = false
                                if (SupabaseAuthService.isEmailSendRateLimited(err)) {
                                    finishLocalSignUp(
                                        emailTrimmed,
                                        password,
                                        nameTrimmed,
                                        phoneFormatted,
                                        R.string.auth_error_email_rate_limit,
                                    )
                                } else {
                                    error = err.message
                                        ?: context.getString(R.string.signup_error_register)
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = tc.Background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.signup_title), color = tc.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = tc.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = tc.Background, titleContentColor = tc.TextPrimary)
            )
        }
    ) { padding ->
        BentoGlassScreenBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.signup_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tc.TextSecondary,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                val tfColors = AppTextFieldDefaults.outlined()
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it; error = null },
                    label = { Text(stringResource(R.string.auth_full_name_hint)) },
                    placeholder = { Text(stringResource(R.string.signup_name_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    colors = tfColors
                )
                Spacer(modifier = Modifier.height(16.dp))
                PhoneWithCountryField(
                    country = phoneCountry,
                    nationalNumber = nationalNumber,
                    onCountryChange = { phoneCountry = it; error = null },
                    onNationalNumberChange = { nationalNumber = it; error = null },
                    label = stringResource(R.string.auth_phone_hint),
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; error = null },
                    label = { Text(stringResource(R.string.auth_email_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = tfColors
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = null },
                    label = { Text(stringResource(R.string.auth_password_hint)) },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = stringResource(
                                    if (passwordVisible) R.string.auth_password_hide_cd
                                    else R.string.auth_password_show_cd,
                                )
                            )
                        }
                    },
                    colors = tfColors
                )
                error?.let { Text(text = it, color = tc.AccentExpense, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)) }
                Spacer(modifier = Modifier.height(24.dp))
                GoogleSignInButton(
                    onClick = { googleSignIn.launch() },
                    enabled = !isLoading && !isGoogleLoading,
                    loading = isGoogleLoading,
                    text = stringResource(R.string.signup_with_google),
                )
                Text(
                    text = stringResource(R.string.login_or_divider),
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 16.dp),
                )
                Button(
                    onClick = { if (!isLoading) performSignUp() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = !isLoading,
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = tc.Background)
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Text(if (isLoading) stringResource(R.string.signup_loading) else stringResource(R.string.signup_button))
                }
            }
        }
        }
    }
    if (showBiometricOffer) {
        BiometricOptInDialog(
            onDismiss = {
                showBiometricOffer = false
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ onSuccess() }, 400)
            },
            onEnabled = {
                enableBiometricUnlock(context)
                showBiometricOffer = false
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ onSuccess() }, 400)
            },
        )
    }
}

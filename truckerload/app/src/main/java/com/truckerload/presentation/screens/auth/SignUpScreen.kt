package com.truckerload.presentation.screens.auth

import com.truckerload.presentation.icons.AppIcons

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
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
import com.truckerload.data.preferences.RegistrationBootstrap
import com.truckerload.data.preferences.UserProfile
import com.truckerload.presentation.auth.BiometricOptInDialog
import com.truckerload.presentation.auth.GoogleAuthCallbacks
import com.truckerload.presentation.auth.enableBiometricUnlock
import com.truckerload.presentation.auth.rememberGoogleSignInLauncher
import com.truckerload.presentation.auth.shouldOfferBiometricUnlock
import com.truckerload.presentation.components.GoogleSignInButton
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.components.verticalContentScroll
import com.truckerload.presentation.di.LocalAuthCredentialsStore
import com.truckerload.presentation.di.LocalAuthStore
import com.truckerload.presentation.di.LocalUserProfileStore
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.BentoGlassScreenBackground
import com.truckerload.presentation.theme.LocalTruckColors

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

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }

    val googleSignIn = rememberGoogleSignInLauncher(
        GoogleAuthCallbacks(
            onBusy = { isGoogleLoading = it },
            onSignedIn = {
                val uid = authStore.currentUserIdOrNull()
                if (uid != null) {
                    RegistrationBootstrap.afterCredentialsCreated(
                        context = context,
                        userId = uid,
                        isVerified = true,
                    )
                }
                onSuccess()
            },
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

    fun nameParts(nameTrimmed: String): Pair<String, String> {
        val parts = nameTrimmed.split(" ", limit = 2)
        return (parts.firstOrNull() ?: "") to (parts.getOrNull(1) ?: "")
    }

    fun finishLocalSignUp(
        emailTrimmed: String,
        passwordValue: String,
        nameTrimmed: String,
        toastRes: Int,
    ) {
        val (given, family) = nameParts(nameTrimmed)
        runCatching { credentialsStore.saveCredentials(emailTrimmed, passwordValue) }
        AuthLogin.completeLogin(
            authStore = authStore,
            userProfileStore = userProfileStore,
            userId = AccountIds.fromEmail(emailTrimmed),
            profile = UserProfile(
                email = emailTrimmed,
                givenName = given,
                familyName = family,
                photoUrl = null,
                phoneNumber = null,
            ),
        )
        RegistrationBootstrap.afterCredentialsCreated(
            context = context,
            userId = AccountIds.fromEmail(emailTrimmed),
            isVerified = true,
        )
        com.truckerload.data.preferences.EmailVerificationStore(context)
            .markVerified(emailTrimmed)
        android.widget.Toast.makeText(context, context.getString(toastRes), android.widget.Toast.LENGTH_LONG).show()
        completeSignUp()
    }

    fun performSignUp() {
        error = null
        val nameTrimmed = fullName.trim()
        val emailTrimmed = email.trim()
        val formError = SignUpFormValidation.errorResId(
            name = nameTrimmed,
            email = emailTrimmed,
            password = password,
            confirmPassword = confirmPassword,
        )
        if (formError != null) {
            error = context.getString(formError)
            return
        }
        finishLocalSignUp(
            emailTrimmed,
            password,
            nameTrimmed,
            R.string.signup_success,
        )
    }

    Scaffold(
        containerColor = tc.Background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.signup_title), color = tc.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = tc.TextPrimary)
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
                    .verticalContentScroll()
                    .padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.signup_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tc.TextSecondary,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                GoogleSignInButton(
                    onClick = { googleSignIn.launch() },
                    enabled = !isLoading && !isGoogleLoading,
                    loading = isGoogleLoading,
                    text = stringResource(R.string.signup_with_google),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.login_google_sync_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary,
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = tc.TextSecondary.copy(alpha = 0.35f),
                    )
                    Text(
                        text = stringResource(R.string.login_or_divider),
                        style = MaterialTheme.typography.bodySmall,
                        color = tc.TextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = tc.TextSecondary.copy(alpha = 0.35f),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.signup_email_alternative),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tc.TextSecondary,
                    modifier = Modifier.padding(bottom = 16.dp),
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
                SignUpPasswordField(
                    value = password,
                    onValueChange = { password = it; error = null },
                    labelRes = R.string.auth_password_hint,
                    visible = passwordVisible,
                    onToggleVisible = { passwordVisible = !passwordVisible },
                    colors = tfColors,
                )
                Spacer(modifier = Modifier.height(16.dp))
                SignUpPasswordField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; error = null },
                    labelRes = R.string.auth_confirm_password_hint,
                    visible = passwordVisible,
                    onToggleVisible = { passwordVisible = !passwordVisible },
                    colors = tfColors,
                )
                error?.let { Text(text = it, color = tc.AccentExpense, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)) }
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(
                    onClick = { if (!isLoading) performSignUp() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = !isLoading && !isGoogleLoading,
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = tc.AccentPrimary)
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

@Composable
private fun SignUpPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    labelRes: Int,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    colors: androidx.compose.material3.TextFieldColors,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = onToggleVisible) {
                Icon(
                    if (visible) AppIcons.VisibilityOff else AppIcons.Visibility,
                    contentDescription = stringResource(
                        if (visible) R.string.auth_password_hide_cd
                        else R.string.auth_password_show_cd,
                    )
                )
            }
        },
        colors = colors,
    )
}

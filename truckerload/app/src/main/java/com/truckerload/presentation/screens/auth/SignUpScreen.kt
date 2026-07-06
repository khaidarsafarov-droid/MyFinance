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
import com.truckerload.presentation.components.TlButton as Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.truckerload.data.preferences.UserProfile
import com.truckerload.data.preferences.UserProfileStore
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
    val supabaseAuth = remember(context) { SupabaseAuthService(context.applicationContext) }
    val scope = rememberCoroutineScope()

    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    fun completeSignUp() {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ onSuccess() }, 400)
    }

    fun performSignUp() {
        error = null
        val nameTrimmed = fullName.trim()
        val emailTrimmed = email.trim()
        val phoneTrimmed = phone.trim().replace(Regex("[^+0-9]"), "")
        val phoneFormatted = if (phoneTrimmed.startsWith("+")) phoneTrimmed else "+1 $phoneTrimmed"
        when {
            nameTrimmed.isBlank() -> error = context.getString(R.string.auth_error_name_required)
            phoneTrimmed.length < 10 -> error = context.getString(R.string.auth_error_phone_required)
            emailTrimmed.isBlank() -> error = context.getString(R.string.auth_error_email_required)
            password.length < 6 -> error = context.getString(R.string.auth_error_password_short)
            !supabaseAuth.isConfigured() -> {
                android.widget.Toast.makeText(context, context.getString(R.string.supabase_not_configured), android.widget.Toast.LENGTH_LONG).show()
                userProfileStore.saveProfile(UserProfile(email = emailTrimmed, givenName = nameTrimmed, familyName = "", photoUrl = null))
                completeSignUp()
            }
            else -> {
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
                        onFailure = {
                            true
                        }
                    )
                    if (!shouldProceed) return@launch
                    val signUpResult = supabaseAuth.signUp(emailTrimmed, password, nameTrimmed, phoneFormatted)
                    withContext(Dispatchers.Main) {
                        signUpResult.fold(
                                                    onSuccess = { r ->
                                                        val parts = nameTrimmed.split(" ", limit = 2)
                                                        if (r.accessToken.isNotBlank()) {
                                                            scope.launch {
                                                                val upsertResult = supabaseAuth.upsertProfile(r.accessToken, r.user.id, nameTrimmed, phoneFormatted, r.user.email ?: emailTrimmed)
                                                                withContext(Dispatchers.Main) {
                                                                    isLoading = false
                                                                    upsertResult.fold(
                                                                        onSuccess = {
                                                                            userProfileStore.saveProfile(UserProfile(email = r.user.email ?: emailTrimmed, givenName = parts.firstOrNull() ?: "", familyName = parts.getOrNull(1) ?: "", photoUrl = null))
                                                                            completeSignUp()
                                                                        },
                                                                        onFailure = { error = it.message ?: context.getString(R.string.signup_error_profile_save) }
                                                                    )
                                                                }
                                                            }
                                                        } else {
                                                            isLoading = false
                                                            android.widget.Toast.makeText(context, context.getString(R.string.signup_success_confirm_email), android.widget.Toast.LENGTH_LONG).show()
                                                        }
                                                    },
                                                    onFailure = {
                                                        isLoading = false
                                                        error = it.message ?: context.getString(R.string.signup_error_register)
                                                    }
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
                    placeholder = { Text("Иван Иванов") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    colors = tfColors
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { s ->
                        val digits = s.filter { it.isDigit() || it == '+' }
                        phone = if (digits.startsWith("+")) {
                            if (digits.length > 1) "+${digits.drop(1).take(11)}" else "+"
                        } else {
                            digits.take(11)
                        }
                        error = null
                    },
                    label = { Text(stringResource(R.string.auth_phone_hint)) },
                    placeholder = { Text("+1 234 567 8900") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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
                                contentDescription = if (passwordVisible) "Скрыть пароль" else "Показать пароль"
                            )
                        }
                    },
                    colors = tfColors
                )
                error?.let { Text(text = it, color = tc.AccentExpense, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)) }
                Spacer(modifier = Modifier.height(32.dp))
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
}

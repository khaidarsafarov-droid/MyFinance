package com.truckerload.presentation.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.truckerload.BuildConfig
import com.truckerload.R
import com.truckerload.data.preferences.EmailVerificationStore
import com.truckerload.data.remote.SupabaseAuthService
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Soft email verification: 6-digit code. Account stays usable while pending
 * ([onSkip]); until verified status remains "awaiting activation".
 *
 * With Supabase configured, the code is sent by Supabase Auth (OTP email).
 * In local-only / debug builds the code is shown on-screen for QA.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailVerificationScreen(
    email: String,
    onVerified: () -> Unit,
    onSkip: () -> Unit,
) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val store = remember { EmailVerificationStore(context.applicationContext) }
    val supabaseAuth = remember(context) { SupabaseAuthService(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var hint by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }

    val supabaseConfigured = remember {
        BuildConfig.SUPABASE_URL.isNotBlank() &&
            BuildConfig.SUPABASE_ANON_KEY.isNotBlank() &&
            !BuildConfig.LOCAL_ONLY_MODE &&
            supabaseAuth.isConfigured()
    }

    fun sendOtp() {
        if (isSending) return
        scope.launch {
            isSending = true
            error = null
            if (supabaseConfigured) {
                val result = withContext(Dispatchers.IO) {
                    supabaseAuth.sendEmailOtp(email)
                }
                result.fold(
                    onSuccess = {
                        hint = context.getString(R.string.email_verify_sent_hint)
                    },
                    onFailure = { err ->
                        error = err.message ?: context.getString(R.string.email_verify_send_failed_generic)
                    },
                )
            } else {
                val generated = store.beginVerification(email)
                hint = context.getString(R.string.email_verify_dev_code, generated)
            }
            isSending = false
        }
    }

    LaunchedEffect(email, supabaseConfigured) {
        if (email.isBlank()) {
            onSkip()
            return@LaunchedEffect
        }
        if (store.isVerified(email)) {
            onVerified()
            return@LaunchedEffect
        }
        if (!store.isPending(email)) {
            store.beginVerification(email)
        }
        if (supabaseConfigured) {
            isSending = true
            error = null
            val result = withContext(Dispatchers.IO) {
                supabaseAuth.sendEmailOtp(email)
            }
            result.fold(
                onSuccess = { hint = context.getString(R.string.email_verify_sent_hint) },
                onFailure = { err ->
                    error = err.message ?: context.getString(R.string.email_verify_send_failed_generic)
                },
            )
            isSending = false
        } else {
            val generated = store.beginVerification(email)
            hint = context.getString(R.string.email_verify_dev_code, generated)
        }
    }

    Scaffold(
        containerColor = BentoGlassTheme.ScreenBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.email_verify_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BentoGlassTheme.ScreenBackground,
                    titleContentColor = tc.TextPrimary,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (supabaseConfigured) {
                    stringResource(R.string.email_verify_subtitle_cloud, email)
                } else {
                    stringResource(R.string.email_verify_subtitle, email)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = tc.TextSecondary,
            )
            hint?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = tc.TextSecondary)
            }
            OutlinedTextField(
                value = code,
                onValueChange = {
                    code = it.filter { ch -> ch.isDigit() }.take(6)
                    error = null
                },
                label = { Text(stringResource(R.string.email_verify_code_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = AppTextFieldDefaults.outlined(),
                enabled = !isVerifying,
            )
            error?.let {
                Text(text = it, color = tc.AccentExpense, style = MaterialTheme.typography.bodySmall)
            }
            if (supabaseConfigured) {
                TextButton(
                    onClick = { sendOtp() },
                    enabled = !isSending && !isVerifying,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (isSending) {
                            stringResource(R.string.email_verify_resending)
                        } else {
                            stringResource(R.string.email_verify_resend)
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    when {
                        code.length != 6 -> error = context.getString(R.string.email_verify_code_invalid)
                        else -> {
                            scope.launch {
                                isVerifying = true
                                error = null
                                val verified = if (supabaseConfigured) {
                                    withContext(Dispatchers.IO) {
                                        supabaseAuth.verifyEmailOtp(email, code)
                                    }.fold(
                                        onSuccess = {
                                            store.markVerified(email)
                                            true
                                        },
                                        onFailure = { err ->
                                            error = err.message
                                                ?: context.getString(R.string.email_verify_code_wrong)
                                            false
                                        },
                                    )
                                } else {
                                    store.verifyCode(email, code)
                                }
                                isVerifying = false
                                if (verified) {
                                    onVerified()
                                } else if (!supabaseConfigured) {
                                    error = context.getString(R.string.email_verify_code_wrong)
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !isVerifying,
            ) {
                Text(
                    if (isVerifying) {
                        stringResource(R.string.email_verify_confirming)
                    } else {
                        stringResource(R.string.email_verify_confirm)
                    },
                )
            }
            TextButton(
                onClick = {
                    store.skipForNow(email)
                    onSkip()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isVerifying,
            ) {
                Text(stringResource(R.string.email_verify_skip))
            }
        }
    }
}

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
import com.truckerload.R
import com.truckerload.data.preferences.EmailVerificationStore
import com.truckerload.data.remote.SupabaseEmailOtp
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
 * When Supabase is configured we request a real OTP email. If delivery fails
 * (or Supabase is off), a local code is shown so the driver is never stuck.
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
    val otpApi = remember { SupabaseEmailOtp() }
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var hint by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }
    var confirming by remember { mutableStateOf(false) }

    fun showLocalFallback(generated: String) {
        hint = context.getString(R.string.email_verify_dev_code, generated)
        status = context.getString(R.string.email_verify_local_fallback)
    }

    suspend fun deliverCode(forceNewLocal: Boolean = false) {
        if (email.isBlank()) return
        sending = true
        error = null
        status = null
        hint = null
        val localCode = if (forceNewLocal || !store.isPending(email)) {
            store.beginVerification(email)
        } else {
            // Keep existing local code; still attempt cloud (re)send.
            store.peekCode(email) ?: store.beginVerification(email)
        }
        if (!otpApi.isConfigured()) {
            showLocalFallback(localCode)
            sending = false
            return
        }
        val sendResult = withContext(Dispatchers.IO) { otpApi.sendVerificationCode(email) }
        if (sendResult.isSuccess) {
            status = context.getString(R.string.email_verify_sent)
            // Keep local code as offline backup but do not advertise it when mail went out.
            hint = null
        } else {
            showLocalFallback(localCode)
        }
        sending = false
    }

    LaunchedEffect(email) {
        if (email.isBlank()) {
            onSkip()
            return@LaunchedEffect
        }
        if (store.isVerified(email)) {
            onVerified()
            return@LaunchedEffect
        }
        deliverCode(forceNewLocal = false)
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
                text = stringResource(R.string.email_verify_subtitle, email),
                style = MaterialTheme.typography.bodyLarge,
                color = tc.TextSecondary,
            )
            status?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = tc.TextSecondary)
            }
            hint?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = tc.TextSecondary)
            }
            OutlinedTextField(
                value = code,
                onValueChange = {
                    code = it.filter { ch -> ch.isDigit() }.take(8)
                    error = null
                },
                label = { Text(stringResource(R.string.email_verify_code_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = AppTextFieldDefaults.outlined(),
            )
            error?.let {
                Text(text = it, color = tc.AccentExpense, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (confirming) return@Button
                    when {
                        code.length < 6 -> {
                            error = context.getString(R.string.email_verify_code_invalid)
                        }
                        else -> {
                            confirming = true
                            scope.launch {
                                var ok = false
                                if (otpApi.isConfigured()) {
                                    val cloud = withContext(Dispatchers.IO) {
                                        otpApi.verifyCode(email, code)
                                    }
                                    if (cloud.isSuccess) {
                                        store.markVerified(email)
                                        ok = true
                                    }
                                }
                                if (!ok) {
                                    ok = store.verifyCode(email, code)
                                }
                                confirming = false
                                if (ok) {
                                    onVerified()
                                } else {
                                    error = context.getString(R.string.email_verify_code_wrong)
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !confirming && !sending,
            ) {
                Text(stringResource(R.string.email_verify_confirm))
            }
            TextButton(
                onClick = {
                    if (sending) return@TextButton
                    scope.launch { deliverCode(forceNewLocal = true) }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !sending && !confirming,
            ) {
                Text(stringResource(R.string.email_verify_resend))
            }
            TextButton(
                onClick = {
                    store.skipForNow(email)
                    onSkip()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.email_verify_skip))
            }
        }
    }
}

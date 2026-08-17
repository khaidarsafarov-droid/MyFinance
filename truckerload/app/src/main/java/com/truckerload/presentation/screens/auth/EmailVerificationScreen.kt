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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.data.preferences.EmailVerificationStore
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors

/**
 * Soft email verification: 6-digit on-device code. Account stays usable while pending
 * ([onSkip]); until verified status remains "awaiting activation".
 *
 * The code is generated and shown in the app — it is not emailed by the client.
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
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var shownCode by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(email) {
        if (email.isBlank()) {
            onSkip()
            return@LaunchedEffect
        }
        if (store.isVerified(email)) {
            onVerified()
            return@LaunchedEffect
        }
        val generated = if (store.isPending(email)) {
            store.peekCode(email) ?: store.beginVerification(email)
        } else {
            store.beginVerification(email)
        }
        shownCode = generated
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
            shownCode?.let {
                Text(
                    text = stringResource(R.string.email_verify_on_device_code, it),
                    style = MaterialTheme.typography.titleMedium,
                    color = tc.TextPrimary,
                )
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
            )
            error?.let {
                Text(text = it, color = tc.AccentExpense, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    when {
                        code.length != 6 -> error = context.getString(R.string.email_verify_code_invalid)
                        store.verifyCode(email, code) -> onVerified()
                        else -> error = context.getString(R.string.email_verify_code_wrong)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(stringResource(R.string.email_verify_confirm))
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

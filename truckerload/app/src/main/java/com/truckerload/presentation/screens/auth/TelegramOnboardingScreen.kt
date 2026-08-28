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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.data.preferences.TelegramOnboardingStore
import com.truckerload.data.remote.TelegramTokenActivator
import com.truckerload.presentation.di.LocalAuthStore
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.components.verticalContentScroll
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelegramOnboardingScreen(
    onCompleted: () -> Unit,
    onSkip: () -> Unit,
) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authStore = LocalAuthStore.current
    val userId = authStore.currentUserIdOrNull()
    val onboardingStore = remember(userId) { TelegramOnboardingStore(context, userId) }
    var tokenInput by remember { mutableStateOf("") }
    var showToken by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun finish(skipped: Boolean) {
        onboardingStore.markCompleted()
        if (skipped) onSkip() else onCompleted()
    }

    Scaffold(
        containerColor = BentoGlassTheme.ScreenBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.telegram_onboarding_title), color = tc.TextPrimary) },
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
                .verticalContentScroll()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.telegram_onboarding_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = tc.TextSecondary,
            )
            Text(
                text = stringResource(R.string.telegram_onboarding_steps_title),
                style = MaterialTheme.typography.titleSmall,
                color = tc.TextPrimary,
            )
            Text(
                text = stringResource(R.string.telegram_onboarding_step_1),
                style = MaterialTheme.typography.bodyMedium,
                color = tc.TextPrimary,
            )
            Text(
                text = stringResource(R.string.telegram_onboarding_step_2),
                style = MaterialTheme.typography.bodyMedium,
                color = tc.TextPrimary,
            )
            Text(
                text = stringResource(R.string.telegram_onboarding_step_3),
                style = MaterialTheme.typography.bodyMedium,
                color = tc.TextPrimary,
            )
            Text(
                text = stringResource(R.string.telegram_onboarding_step_4),
                style = MaterialTheme.typography.bodyMedium,
                color = tc.TextPrimary,
            )
            OutlinedButton(
                onClick = { BotFatherLinks.open(context) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !saving,
            ) {
                Text(stringResource(R.string.telegram_onboarding_open_botfather))
            }
            OutlinedTextField(
                value = tokenInput,
                onValueChange = {
                    tokenInput = it
                    error = null
                },
                label = { Text(stringResource(R.string.settings_telegram_token_label)) },
                placeholder = { Text(stringResource(R.string.settings_telegram_token_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !saving,
                visualTransformation = if (showToken) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = AppTextFieldDefaults.outlined(),
            )
            TextButton(
                onClick = { showToken = !showToken },
                enabled = !saving,
            ) {
                Text(
                    stringResource(
                        if (showToken) R.string.settings_telegram_hide_token
                        else R.string.settings_telegram_show_token
                    ),
                    color = tc.AccentPrimary,
                )
            }
            error?.let {
                Text(it, color = tc.AccentExpense, style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = {
                    if (saving) return@Button
                    val token = tokenInput.trim()
                    if (token.isBlank()) {
                        error = context.getString(R.string.settings_telegram_token_missing)
                        return@Button
                    }
                    scope.launch {
                        saving = true
                        error = null
                        val result = TelegramTokenActivator.saveAndStart(context, token)
                        saving = false
                        result.fold(
                            onSuccess = { finish(skipped = false) },
                            onFailure = { err ->
                                error = when (err.message) {
                                    "token_missing" -> context.getString(R.string.settings_telegram_token_missing)
                                    "token_invalid" -> context.getString(R.string.settings_telegram_invalid)
                                    "token_secure_storage" -> context.getString(
                                        R.string.auth_secure_storage_fallback_banner,
                                    )
                                    else -> err.message
                                        ?: context.getString(R.string.settings_telegram_invalid)
                                }
                            },
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = tokenInput.isNotBlank() && !saving,
            ) {
                Text(
                    if (saving) {
                        stringResource(R.string.settings_telegram_checking)
                    } else {
                        stringResource(R.string.telegram_onboarding_save)
                    },
                )
            }
            TextButton(
                onClick = { if (!saving) finish(skipped = true) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving,
            ) {
                Text(stringResource(R.string.telegram_onboarding_skip), color = tc.TextSecondary)
            }
            Text(
                text = stringResource(R.string.telegram_onboarding_skip_hint),
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

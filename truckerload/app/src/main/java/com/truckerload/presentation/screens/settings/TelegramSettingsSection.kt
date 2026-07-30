package com.truckerload.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.truckerload.data.preferences.TelegramTokenStore
import com.truckerload.data.remote.TelegramBotHealth
import com.truckerload.presentation.components.BotStatusBadge
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.BentoGlassSection
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.sync.TelegramBotForegroundService
import com.truckerload.sync.TelegramSyncMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TelegramSettingsSection() {
    if (TelegramSyncMode.isServer()) {
        TelegramServerModeSection()
        return
    }

    val context = LocalContext.current
    val tc = LocalTruckColors.current
    val scope = rememberCoroutineScope()
    val tokenStore = remember { TelegramTokenStore(context) }
    var tokenInput by remember { mutableStateOf(tokenStore.getToken()) }
    var showToken by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var botActive by remember { mutableStateOf(TelegramBotForegroundService.isRunning()) }

    LaunchedEffect(Unit) {
        botActive = TelegramBotForegroundService.isRunning()
    }

    BentoGlassSection(
        title = stringResource(R.string.settings_telegram_title),
        subtitle = stringResource(R.string.settings_telegram_desc),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BotStatusBadge(active = botActive)
            Text(
                text = stringResource(
                    if (botActive) R.string.settings_telegram_status_active
                    else R.string.settings_telegram_status_inactive
                ),
                style = MaterialTheme.typography.bodySmall,
                color = if (botActive) tc.AccentProfit else tc.TextSecondary,
            )
        }

        OutlinedTextField(
            value = tokenInput,
            onValueChange = {
                tokenInput = it
                statusMessage = null
            },
            label = { Text(stringResource(R.string.settings_telegram_token_label)) },
            placeholder = { Text(stringResource(R.string.settings_telegram_token_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (showToken) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = AppTextFieldDefaults.outlined(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { showToken = !showToken },
                modifier = Modifier.weight(1f).height(44.dp),
            ) {
                Text(
                    stringResource(
                        if (showToken) R.string.settings_telegram_hide_token
                        else R.string.settings_telegram_show_token
                    )
                )
            }
            Button(
                onClick = {
                    if (testing) return@Button
                    scope.launch {
                        testing = true
                        statusMessage = context.getString(R.string.settings_telegram_checking)
                        val token = tokenInput.trim()
                        val health = withContext(Dispatchers.IO) { TelegramBotHealth.check(token) }
                        testing = false
                        statusMessage = when {
                            health.ok -> {
                                tokenStore.setToken(token)
                                TelegramBotForegroundService.stop(context)
                                TelegramBotForegroundService.start(context)
                                botActive = TelegramBotForegroundService.isRunning()
                                context.getString(R.string.settings_telegram_ok, health.username.orEmpty())
                            }
                            health.isUnauthorized -> context.getString(R.string.settings_telegram_invalid)
                            else -> health.error ?: context.getString(R.string.settings_telegram_invalid)
                        }
                    }
                },
                enabled = tokenInput.isNotBlank() && !testing,
                modifier = Modifier.weight(1f).height(44.dp),
            ) {
                Text(stringResource(R.string.settings_telegram_test))
            }
        }

        statusMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
            )
        }
    }
}

@Composable
private fun TelegramServerModeSection() {
    val tc = LocalTruckColors.current
    BentoGlassSection(
        title = stringResource(R.string.settings_telegram_title),
        subtitle = stringResource(R.string.settings_telegram_server_mode),
    ) {
        Text(
            text = stringResource(R.string.settings_telegram_server_mode),
            style = MaterialTheme.typography.bodySmall,
            color = tc.TextSecondary,
        )
    }
}

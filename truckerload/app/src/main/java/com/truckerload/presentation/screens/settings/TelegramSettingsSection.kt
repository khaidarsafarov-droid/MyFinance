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
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.data.preferences.TelegramTokenStore
import com.truckerload.data.remote.TelegramBotHealth
import com.truckerload.presentation.components.BotStatusBadge
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.BentoGlassSection
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.sync.TelegramBotForegroundService
import com.truckerload.sync.TelegramPairingCodes
import com.truckerload.sync.TelegramSyncMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    val settingsStore = remember { SettingsDataStore(context) }
    var tokenInput by remember { mutableStateOf(tokenStore.getToken()) }
    var showToken by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var pairedChatId by remember { mutableStateOf<Long?>(null) }
    var pairCodeDisplay by remember { mutableStateOf<String?>(null) }
    var pairExpiresDisplay by remember { mutableStateOf<String?>(null) }
    // Must be Boolean: companion exposes fun isRunning(), not the private AtomicBoolean.
    var botActive: Boolean by remember {
        mutableStateOf(TelegramBotForegroundService.isRunning())
    }

    LaunchedEffect(Unit) {
        botActive = TelegramBotForegroundService.isRunning()
        pairedChatId = withContext(Dispatchers.IO) { settingsStore.getTelegramChatIdOnce() }
        val pair = withContext(Dispatchers.IO) { settingsStore.getTelegramPairingCodeOnce() }
        if (pair != null && TelegramPairingCodes.isActive(pair.second, System.currentTimeMillis())) {
            pairCodeDisplay = pair.first
            pairExpiresDisplay = formatPairExpiry(pair.second)
        }
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
                                val running: Boolean = TelegramBotForegroundService.isRunning()
                                botActive = running
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

        Text(
            text = stringResource(R.string.settings_telegram_pair_title),
            style = MaterialTheme.typography.titleSmall,
            color = tc.TextPrimary,
        )
        Text(
            text = stringResource(R.string.settings_telegram_pair_desc),
            style = MaterialTheme.typography.bodySmall,
            color = tc.TextSecondary,
        )
        if (pairedChatId != null) {
            Text(
                text = stringResource(R.string.settings_telegram_paired, pairedChatId.toString()),
                style = MaterialTheme.typography.bodySmall,
                color = tc.AccentProfit,
            )
            OutlinedButton(
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) { settingsStore.clearTelegramChatId() }
                        pairedChatId = null
                        statusMessage = context.getString(R.string.settings_telegram_unpaired)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(44.dp),
            ) {
                Text(stringResource(R.string.settings_telegram_unpair))
            }
        } else {
            pairCodeDisplay?.let { code ->
                Text(
                    text = stringResource(
                        R.string.settings_telegram_pair_code_active,
                        code,
                        pairExpiresDisplay.orEmpty(),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tc.AccentPrimary,
                )
            }
            Button(
                onClick = {
                    scope.launch {
                        val (code, expires) = withContext(Dispatchers.IO) {
                            settingsStore.issueTelegramPairingCode()
                        }
                        pairCodeDisplay = code
                        pairExpiresDisplay = formatPairExpiry(expires)
                        statusMessage = context.getString(
                            R.string.settings_telegram_pair_code_issued,
                            code,
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(44.dp),
            ) {
                Text(stringResource(R.string.settings_telegram_issue_pair_code))
            }
        }
    }
}

private fun formatPairExpiry(expiresAtMillis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(expiresAtMillis))

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

package com.truckerload.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

/**
 * Telegram bot status and token setup in Settings.
 * The token is stored in encrypted [TelegramTokenStore]; the main screen never shows it —
 * use the add/change dialog to enter or update the bot token.
 */
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
    var hasToken by remember { mutableStateOf(tokenStore.hasToken()) }
    var testing by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showTokenDialog by remember { mutableStateOf(false) }
    var tokenInput by remember { mutableStateOf("") }
    var showToken by remember { mutableStateOf(false) }
    var dialogError by remember { mutableStateOf<String?>(null) }
    var botActive: Boolean by remember {
        mutableStateOf(TelegramBotForegroundService.isRunning())
    }

    LaunchedEffect(Unit) {
        hasToken = tokenStore.hasToken()
        botActive = TelegramBotForegroundService.isRunning()
    }

    if (showTokenDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!saving) showTokenDialog = false
            },
            containerColor = tc.CardBackground,
            titleContentColor = tc.TextPrimary,
            textContentColor = tc.TextPrimary,
            title = { Text(stringResource(R.string.settings_telegram_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.settings_telegram_dialog_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = tc.TextSecondary,
                    )
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = {
                            tokenInput = it
                            dialogError = null
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
                        enabled = !saving,
                    )
                    OutlinedButton(
                        onClick = { showToken = !showToken },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        enabled = !saving,
                    ) {
                        Text(
                            stringResource(
                                if (showToken) R.string.settings_telegram_hide_token
                                else R.string.settings_telegram_show_token
                            )
                        )
                    }
                    dialogError?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = tc.AccentExpense,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (saving) return@TextButton
                        val token = tokenInput.trim()
                        if (token.isBlank()) {
                            dialogError = context.getString(R.string.settings_telegram_token_missing)
                            return@TextButton
                        }
                        scope.launch {
                            saving = true
                            dialogError = null
                            val health = withContext(Dispatchers.IO) { TelegramBotHealth.check(token) }
                            saving = false
                            when {
                                health.ok -> {
                                    tokenStore.setToken(token)
                                    TelegramBotForegroundService.stop(context)
                                    TelegramBotForegroundService.start(context)
                                    hasToken = true
                                    botActive = TelegramBotForegroundService.isRunning()
                                    showTokenDialog = false
                                    tokenInput = ""
                                    showToken = false
                                    statusMessage = context.getString(
                                        R.string.settings_telegram_saved,
                                    )
                                }
                                health.isUnauthorized -> {
                                    dialogError = context.getString(R.string.settings_telegram_invalid)
                                }
                                else -> {
                                    dialogError = health.error
                                        ?: context.getString(R.string.settings_telegram_invalid)
                                }
                            }
                        }
                    },
                    enabled = tokenInput.isNotBlank() && !saving,
                ) {
                    Text(
                        text = if (saving) {
                            stringResource(R.string.settings_telegram_checking)
                        } else {
                            stringResource(R.string.common_save)
                        },
                        color = tc.AccentPrimary,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!saving) {
                            showTokenDialog = false
                            tokenInput = ""
                            showToken = false
                            dialogError = null
                        }
                    },
                    enabled = !saving,
                ) {
                    Text(stringResource(R.string.common_cancel), color = tc.TextSecondary)
                }
            },
        )
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

        Text(
            text = stringResource(
                if (hasToken) R.string.settings_telegram_token_configured
                else R.string.settings_telegram_token_missing
            ),
            style = MaterialTheme.typography.bodySmall,
            color = if (hasToken) tc.AccentProfit else tc.TextSecondary,
        )

        if (hasToken) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        if (testing) return@Button
                        scope.launch {
                            testing = true
                            statusMessage = context.getString(R.string.settings_telegram_checking)
                            val token = tokenStore.getToken()
                            if (token.isBlank()) {
                                testing = false
                                hasToken = false
                                statusMessage = context.getString(R.string.settings_telegram_token_missing)
                                return@launch
                            }
                            val health = withContext(Dispatchers.IO) { TelegramBotHealth.check(token) }
                            testing = false
                            hasToken = true
                            statusMessage = when {
                                health.ok -> {
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
                    enabled = !testing,
                    modifier = Modifier.weight(1f).height(44.dp),
                ) {
                    Text(stringResource(R.string.settings_telegram_test))
                }
                OutlinedButton(
                    onClick = {
                        tokenInput = ""
                        showToken = false
                        dialogError = null
                        showTokenDialog = true
                    },
                    modifier = Modifier.weight(1f).height(44.dp),
                ) {
                    Text(stringResource(R.string.settings_telegram_change_token))
                }
            }
        } else {
            Button(
                onClick = {
                    tokenInput = ""
                    showToken = false
                    dialogError = null
                    showTokenDialog = true
                },
                modifier = Modifier.fillMaxWidth().height(44.dp),
            ) {
                Text(stringResource(R.string.settings_telegram_add_bot))
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

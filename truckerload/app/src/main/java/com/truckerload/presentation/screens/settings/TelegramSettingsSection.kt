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
 * Telegram bot status in Settings.
 *
 * The saved token is never displayed. Users can paste a new token via «Add bot»
 * into a write-only dialog; it is validated then stored in [TelegramTokenStore].
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
    var showAddDialog by remember { mutableStateOf(false) }
    var botActive: Boolean by remember {
        mutableStateOf(TelegramBotForegroundService.isRunning())
    }

    LaunchedEffect(Unit) {
        hasToken = tokenStore.hasToken()
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

        Text(
            text = stringResource(
                if (hasToken) R.string.settings_telegram_token_configured
                else R.string.settings_telegram_token_missing
            ),
            style = MaterialTheme.typography.bodySmall,
            color = if (hasToken) tc.AccentProfit else tc.TextSecondary,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    statusMessage = null
                    showAddDialog = true
                },
                enabled = !testing && !saving,
                modifier = Modifier.weight(1f).height(44.dp),
            ) {
                Text(
                    stringResource(
                        if (hasToken) R.string.settings_telegram_change_bot
                        else R.string.settings_telegram_add_bot
                    )
                )
            }
            OutlinedButton(
                onClick = {
                    if (testing) return@OutlinedButton
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
                enabled = hasToken && !testing && !saving,
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

    if (showAddDialog) {
        AddBotTokenDialog(
            saving = saving,
            onDismiss = {
                if (!saving) showAddDialog = false
            },
            onSave = { draftToken ->
                if (saving) return@AddBotTokenDialog
                scope.launch {
                    saving = true
                    statusMessage = context.getString(R.string.settings_telegram_checking)
                    val token = draftToken.trim()
                    val health = withContext(Dispatchers.IO) { TelegramBotHealth.check(token) }
                    if (!health.ok) {
                        saving = false
                        statusMessage = when {
                            health.isUnauthorized ->
                                context.getString(R.string.settings_telegram_invalid)
                            else -> health.error
                                ?: context.getString(R.string.settings_telegram_invalid)
                        }
                        return@launch
                    }
                    try {
                        tokenStore.setToken(token)
                    } catch (e: IllegalStateException) {
                        saving = false
                        statusMessage = context.getString(R.string.auth_secure_storage_fallback_banner)
                        return@launch
                    }
                    TelegramBotForegroundService.stop(context)
                    TelegramBotForegroundService.start(context)
                    botActive = TelegramBotForegroundService.isRunning()
                    hasToken = true
                    saving = false
                    showAddDialog = false
                    statusMessage = context.getString(
                        R.string.settings_telegram_saved,
                    ) + "\n" + context.getString(
                        R.string.settings_telegram_ok,
                        health.username.orEmpty(),
                    )
                }
            },
        )
    }
}

@Composable
private fun AddBotTokenDialog(
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val tc = LocalTruckColors.current
    var tokenInput by remember { mutableStateOf("") }
    var showDraft by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.settings_telegram_add_dialog_title),
                color = tc.TextPrimary,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.settings_telegram_add_dialog_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary,
                )
                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    label = { Text(stringResource(R.string.settings_telegram_token_label)) },
                    placeholder = {
                        Text(stringResource(R.string.settings_telegram_token_placeholder))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !saving,
                    visualTransformation = if (showDraft) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = AppTextFieldDefaults.outlined(),
                )
                TextButton(
                    onClick = { showDraft = !showDraft },
                    enabled = !saving,
                ) {
                    Text(
                        stringResource(
                            if (showDraft) R.string.settings_telegram_hide_token
                            else R.string.settings_telegram_show_token
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(tokenInput) },
                enabled = tokenInput.isNotBlank() && !saving,
            ) {
                Text(
                    stringResource(
                        if (saving) R.string.settings_telegram_checking
                        else R.string.common_save
                    )
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !saving,
            ) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
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

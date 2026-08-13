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

/**
 * Telegram bot status and token entry in Settings.
 * The stored token is never pre-filled or displayed — only a fresh paste field for add/change.
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
    var editingToken by remember { mutableStateOf(false) }
    var tokenInput by remember { mutableStateOf("") }
    var showToken by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var botActive: Boolean by remember {
        mutableStateOf(TelegramBotForegroundService.isRunning())
    }

    LaunchedEffect(Unit) {
        hasToken = tokenStore.hasToken()
        botActive = TelegramBotForegroundService.isRunning()
        // Offer the add form immediately when no token is configured.
        if (!hasToken) editingToken = true
    }

    fun openEditor() {
        tokenInput = ""
        showToken = false
        statusMessage = null
        editingToken = true
    }

    fun closeEditor() {
        tokenInput = ""
        showToken = false
        editingToken = false
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

        if (editingToken) {
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
                    enabled = !busy,
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
                        if (busy) return@Button
                        scope.launch {
                            busy = true
                            statusMessage = context.getString(R.string.settings_telegram_checking)
                            val token = tokenInput.trim()
                            if (token.isBlank()) {
                                busy = false
                                statusMessage =
                                    context.getString(R.string.settings_telegram_token_missing)
                                return@launch
                            }
                            val health = withContext(Dispatchers.IO) {
                                TelegramBotHealth.check(token)
                            }
                            if (!health.ok) {
                                busy = false
                                statusMessage = when {
                                    health.isUnauthorized ->
                                        context.getString(R.string.settings_telegram_invalid)
                                    else ->
                                        health.error
                                            ?: context.getString(R.string.settings_telegram_invalid)
                                }
                                return@launch
                            }
                            val persistError = runCatching { tokenStore.setToken(token) }
                                .exceptionOrNull()
                            if (persistError != null) {
                                busy = false
                                statusMessage = context.getString(
                                    R.string.auth_secure_storage_fallback_banner,
                                )
                                return@launch
                            }
                            TelegramBotForegroundService.stop(context)
                            TelegramBotForegroundService.start(context)
                            botActive = TelegramBotForegroundService.isRunning()
                            hasToken = true
                            closeEditor()
                            busy = false
                            statusMessage = context.getString(
                                R.string.settings_telegram_ok,
                                health.username.orEmpty(),
                            )
                        }
                    },
                    enabled = tokenInput.isNotBlank() && !busy,
                    modifier = Modifier.weight(1f).height(44.dp),
                ) {
                    Text(stringResource(R.string.settings_telegram_save_token))
                }
            }

            if (hasToken) {
                OutlinedButton(
                    onClick = { if (!busy) closeEditor() },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { openEditor() },
                    enabled = !busy,
                    modifier = Modifier.weight(1f).height(44.dp),
                ) {
                    Text(
                        stringResource(
                            if (hasToken) R.string.settings_telegram_change_token
                            else R.string.settings_telegram_add_bot
                        )
                    )
                }
                Button(
                    onClick = {
                        if (busy) return@Button
                        scope.launch {
                            busy = true
                            statusMessage = context.getString(R.string.settings_telegram_checking)
                            val token = tokenStore.getToken()
                            if (token.isBlank()) {
                                busy = false
                                hasToken = false
                                editingToken = true
                                statusMessage =
                                    context.getString(R.string.settings_telegram_token_missing)
                                return@launch
                            }
                            val health = withContext(Dispatchers.IO) {
                                TelegramBotHealth.check(token)
                            }
                            busy = false
                            hasToken = true
                            statusMessage = when {
                                health.ok -> {
                                    TelegramBotForegroundService.stop(context)
                                    TelegramBotForegroundService.start(context)
                                    botActive = TelegramBotForegroundService.isRunning()
                                    context.getString(
                                        R.string.settings_telegram_ok,
                                        health.username.orEmpty(),
                                    )
                                }
                                health.isUnauthorized ->
                                    context.getString(R.string.settings_telegram_invalid)
                                else ->
                                    health.error
                                        ?: context.getString(R.string.settings_telegram_invalid)
                            }
                        }
                    },
                    enabled = hasToken && !busy,
                    modifier = Modifier.weight(1f).height(44.dp),
                ) {
                    Text(stringResource(R.string.settings_telegram_test))
                }
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

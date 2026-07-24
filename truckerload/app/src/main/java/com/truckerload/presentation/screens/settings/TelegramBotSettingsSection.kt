package com.truckerload.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.data.preferences.TelegramTokenStore
import com.truckerload.data.remote.TelegramBotHealth
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.TruckColorPalette
import com.truckerload.sync.TelegramBotForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun TelegramBotSettingsContent(
    context: android.content.Context,
    tc: TruckColorPalette
) {
    val tokenStore = remember { TelegramTokenStore(context) }
    val storedToken = remember { tokenStore.getToken() }
    var tokenInput by remember { mutableStateOf(storedToken) }
    var tokenVisible by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    OutlinedTextField(
        value = tokenInput,
        onValueChange = { tokenInput = it; statusText = null },
        label = { Text(stringResource(R.string.settings_telegram_token_label)) },
        placeholder = { Text(stringResource(R.string.settings_telegram_token_placeholder)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { tokenVisible = !tokenVisible }) {
                Icon(
                    imageVector = if (tokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = stringResource(
                        if (tokenVisible) R.string.settings_telegram_hide_token else R.string.settings_telegram_show_token,
                    ),
                )
            }
        },
        colors = AppTextFieldDefaults.outlined(),
    )
    statusText?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = if (it.startsWith("✅")) tc.AccentProfit else tc.AccentExpense,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
    Spacer(modifier = Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = {
                tokenStore.setToken(tokenInput.trim())
                TelegramBotForegroundService.start(context)
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.settings_telegram_saved),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            },
            modifier = Modifier.weight(1f).height(48.dp),
            enabled = tokenInput.isNotBlank()
        ) {
            Text(stringResource(R.string.common_save))
        }
        OutlinedButton(
            onClick = {
                isChecking = true
                statusText = null
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        TelegramBotHealth.check(tokenInput.trim())
                    }
                    statusText = when {
                        result.ok -> context.getString(
                            R.string.settings_telegram_ok,
                            result.username.orEmpty()
                        )
                        result.isUnauthorized -> context.getString(R.string.settings_telegram_invalid)
                        else -> "❌ ${result.error.orEmpty()}"
                    }
                    isChecking = false
                }
            },
            modifier = Modifier.weight(1f).height(48.dp),
            enabled = tokenInput.isNotBlank() && !isChecking
        ) {
            Text(if (isChecking) stringResource(R.string.settings_telegram_checking) else stringResource(R.string.settings_telegram_test))
        }
    }
}

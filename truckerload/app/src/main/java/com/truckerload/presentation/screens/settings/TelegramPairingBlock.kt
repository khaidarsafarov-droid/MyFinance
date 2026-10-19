package com.truckerload.presentation.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.data.preferences.TelegramTokenStore
import com.truckerload.data.remote.TelegramBotHealth
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.theme.LocalTruckColors
import kotlinx.coroutines.launch

/** Paired state with an Unpair action, or the one-time `/start <code>` needed to pair. */
@Composable
fun TelegramPairingBlock(onStatus: (String) -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val tc = LocalTruckColors.current
    val scope = rememberCoroutineScope()
    val settings = remember { SettingsDataStore(context) }
    val pairedChatId by remember { settings.observeTelegramChatId() }
        .collectAsStateWithLifecycle(initialValue = null)
    var code by remember { mutableStateOf<String?>(null) }
    var botUsername by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pairedChatId) {
        if (pairedChatId != null) return@LaunchedEffect
        code = settings.getOrCreateTelegramPairingCode()
        if (botUsername == null) {
            val token = TelegramTokenStore(context).getToken()
            if (token.isNotBlank()) {
                botUsername = TelegramBotHealth.check(token).username?.takeIf { it.isNotBlank() }
            }
        }
    }

    if (pairedChatId != null) {
        Text(
            text = stringResource(R.string.settings_telegram_paired),
            style = MaterialTheme.typography.bodySmall,
            color = tc.AccentProfit,
        )
        OutlinedButton(
            onClick = {
                scope.launch {
                    settings.clearTelegramChatId()
                    onStatus(resources.getString(R.string.settings_telegram_unpaired_done))
                }
            },
            modifier = Modifier.fillMaxWidth().height(44.dp),
        ) {
            Text(stringResource(R.string.settings_telegram_unpair))
        }
        return
    }

    val pairingCode = code ?: return
    Text(
        text = stringResource(R.string.settings_telegram_pair_hint, pairingCode),
        style = MaterialTheme.typography.bodySmall,
        color = tc.TextSecondary,
    )
    val username = botUsername ?: return
    Button(
        onClick = {
            val link = Uri.parse("https://t.me/$username?start=$pairingCode")
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, link).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        },
        modifier = Modifier.fillMaxWidth().height(44.dp),
    ) {
        Text(stringResource(R.string.settings_telegram_open_bot))
    }
}

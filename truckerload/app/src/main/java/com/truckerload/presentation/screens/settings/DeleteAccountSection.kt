package com.truckerload.presentation.screens.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.truckerload.R
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.di.LocalAccountDeletionService
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.LocalTruckColors
import kotlinx.coroutines.launch

@Composable
fun DeleteAccountSection() {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val deletion = LocalAccountDeletionService.current
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    SettingsCard {
        SettingsNavRow(
            icon = AppIcons.DeleteForever,
            title = stringResource(R.string.settings_delete_account_title),
            subtitle = stringResource(R.string.settings_delete_subtitle),
            onClick = { if (!busy) confirm = true },
            danger = true,
        )
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { if (!busy) confirm = false },
            title = {
                Text(stringResource(R.string.settings_delete_account_confirm_title), color = tc.TextPrimary)
            },
            text = {
                Text(stringResource(R.string.settings_delete_account_confirm_message), color = tc.TextSecondary)
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (busy) return@Button
                        busy = true
                        scope.launch {
                            val result = runCatching { deletion.deleteAccountAndSignOut() }
                            busy = false
                            if (result.isSuccess) confirm = false
                            android.widget.Toast.makeText(
                                context,
                                context.getString(
                                    if (result.isSuccess) {
                                        R.string.settings_delete_account_success
                                    } else {
                                        R.string.settings_delete_account_error
                                    },
                                ),
                                android.widget.Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                ) {
                    Text(stringResource(R.string.settings_delete_account_button))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { if (!busy) confirm = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

package com.truckerload.presentation.screens.settings

import com.truckerload.presentation.icons.AppIcons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.di.LocalAccountDeletionService
import com.truckerload.presentation.theme.BentoGlassSection
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

    BentoGlassSection(
        title = stringResource(R.string.settings_delete_account_title),
    ) {
        Text(
            text = stringResource(R.string.settings_delete_account_desc),
            color = tc.TextSecondary,
        )
        OutlinedButton(
            onClick = { confirm = true },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = !busy,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    AppIcons.DeleteForever,
                    contentDescription = stringResource(R.string.settings_delete_account_button),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.settings_delete_account_button))
            }
        }
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
                            runCatching { deletion.deleteAccountAndSignOut() }
                            busy = false
                            confirm = false
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.settings_delete_account_success),
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

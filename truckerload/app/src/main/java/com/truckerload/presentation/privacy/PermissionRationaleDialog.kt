package com.truckerload.presentation.privacy

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.truckerload.R
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.theme.LocalTruckColors

@Composable
fun PermissionRationaleDialog(
    title: String,
    body: String,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tc = LocalTruckColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = tc.TextPrimary) },
        text = { Text(body, color = tc.TextSecondary) },
        confirmButton = {
            Button(onClick = onContinue) {
                Text(stringResource(R.string.permission_rationale_continue))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.permission_rationale_not_now))
            }
        },
    )
}

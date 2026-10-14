package com.truckerload.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.utils.ClipboardUtils

/** The Play toast clips before the fingerprint. This dialog keeps the whole SHA-1. */
@Composable
fun DriveSha1Dialog(
    message: String,
    sha1: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.drive_sha1_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(message)
                SelectionContainer {
                    Text(text = sha1, style = MaterialTheme.typography.titleMedium)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { ClipboardUtils.copyTextWithToast(context, sha1, "sha1") },
            ) {
                Text(stringResource(R.string.copy_text))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_close))
            }
        },
    )
}

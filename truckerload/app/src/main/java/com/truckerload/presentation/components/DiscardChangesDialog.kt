package com.truckerload.presentation.components

import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.truckerload.R

@Composable
fun DiscardChangesDialog(
    onKeepEditing: () -> Unit,
    onDiscard: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        title = { Text(stringResource(R.string.discard_changes_title)) },
        text = { Text(stringResource(R.string.discard_changes_message)) },
        confirmButton = {
            TextButton(onClick = onDiscard) {
                Text(stringResource(R.string.discard_changes_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onKeepEditing) {
                Text(stringResource(R.string.discard_changes_keep))
            }
        },
    )
}

/**
 * System back and the toolbar share one check. A clean form leaves immediately.
 */
@Composable
fun rememberFormBack(
    isDirty: Boolean,
    onBack: () -> Unit,
): () -> Unit {
    var showDiscard by remember { mutableStateOf(false) }
    BackHandler(enabled = isDirty) { showDiscard = true }
    if (showDiscard) {
        DiscardChangesDialog(
            onKeepEditing = { showDiscard = false },
            onDiscard = {
                showDiscard = false
                onBack()
            },
        )
    }
    return {
        if (isDirty) showDiscard = true else onBack()
    }
}

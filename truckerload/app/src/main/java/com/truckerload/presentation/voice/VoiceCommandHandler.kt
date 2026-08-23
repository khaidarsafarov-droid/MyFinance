package com.truckerload.presentation.voice

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.truckerload.R
import com.truckerload.presentation.screens.assistant.AssistantConfirmationSheet
import com.truckerload.presentation.screens.assistant.weeklyGrossAnswerText
import com.truckerload.voice.VoiceFailReason
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlTextButton as TextButton

@Composable
fun VoiceCommandHandler(
    navController: NavHostController,
    viewModel: VoiceCommandViewModel = hiltViewModel(),
) {
    val destination by viewModel.navigateTo.collectAsStateWithLifecycle()
    val prompt by viewModel.prompt.collectAsStateWithLifecycle()
    val journalSaving by viewModel.journalSaving.collectAsStateWithLifecycle()
    LaunchedEffect(destination) {
        val route = destination ?: return@LaunchedEffect
        navController.navigate(route) { launchSingleTop = true }
        viewModel.onNavigated()
    }
    when (val current = prompt) {
        is VoicePrompt.ConfirmJournal -> AssistantConfirmationSheet(
            mutation = current.mutation,
            isSaving = journalSaving,
            onConfirm = viewModel::confirmSensitive,
            onCancel = viewModel::dismissPrompt,
            onFix = viewModel::fixJournal,
        )
        is VoicePrompt.WeeklyGross -> VoiceConfirmDialog(
            title = stringResource(R.string.voice_journal_gross_title),
            body = weeklyGrossAnswerText(current.summary),
            confirmLabel = stringResource(R.string.common_ok),
            onConfirm = viewModel::dismissPrompt,
            onDismiss = viewModel::dismissPrompt,
            showCancel = false,
        )
        is VoicePrompt.Failed -> VoiceConfirmDialog(
            title = stringResource(R.string.voice_failed_title),
            body = stringResource(current.reason.toMessageRes()),
            confirmLabel = stringResource(R.string.common_ok),
            onConfirm = viewModel::dismissPrompt,
            onDismiss = viewModel::dismissPrompt,
            showCancel = false,
        )
        null -> Unit
    }
}

@Composable
private fun VoiceConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    showCancel: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            Button(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = if (showCancel) {
            { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } }
        } else {
            null
        },
    )
}

private fun VoiceFailReason.toMessageRes(): Int = when (this) {
    VoiceFailReason.UNKNOWN -> R.string.voice_failed_unknown
}

package com.truckerload.presentation.voice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.truckerload.R
import com.truckerload.voice.VoiceFailReason
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlTextButton as TextButton
import com.truckerload.presentation.theme.BentoGlassClickableCard
import com.truckerload.presentation.theme.LocalTruckColors

@Composable
fun VoiceCommandHandler(
    navController: NavHostController,
    viewModel: VoiceCommandViewModel = hiltViewModel(),
) {
    val destination by viewModel.navigateTo.collectAsStateWithLifecycle()
    val prompt by viewModel.prompt.collectAsStateWithLifecycle()
    LaunchedEffect(destination) {
        val route = destination ?: return@LaunchedEffect
        navController.navigate(route) { launchSingleTop = true }
        viewModel.onNavigated()
    }
    when (val current = prompt) {
        is VoicePrompt.ConfirmCall -> VoiceConfirmDialog(
            title = stringResource(R.string.voice_confirm_call_title),
            body = stringResource(R.string.voice_confirm_call_body, current.peer.label),
            confirmLabel = stringResource(R.string.voice_confirm_call_action),
            onConfirm = viewModel::confirmSensitive,
            onDismiss = viewModel::dismissPrompt,
        )
        is VoicePrompt.ConfirmMessage -> VoiceConfirmDialog(
            title = stringResource(R.string.voice_confirm_message_title),
            body = stringResource(
                R.string.voice_confirm_message_body,
                current.peer.label,
                current.text.ifBlank { "—" },
            ),
            confirmLabel = stringResource(R.string.voice_confirm_message_action),
            onConfirm = viewModel::confirmSensitive,
            onDismiss = viewModel::dismissPrompt,
        )
        is VoicePrompt.PickPeer -> VoicePickPeerDialog(
            candidates = current.candidates,
            onPick = viewModel::pickPeer,
            onDismiss = viewModel::dismissPrompt,
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

@Composable
private fun VoicePickPeerDialog(
    candidates: List<VoicePeerChoice>,
    onPick: (VoicePeerChoice) -> Unit,
    onDismiss: () -> Unit,
) {
    val tc = LocalTruckColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.voice_pick_peer_title)) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(candidates, key = { it.id }) { peer ->
                    BentoGlassClickableCard(onClick = { onPick(peer) }) {
                        Text(
                            text = peer.label,
                            color = tc.TextPrimary,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

private fun VoiceFailReason.toMessageRes(): Int = when (this) {
    VoiceFailReason.UNKNOWN -> R.string.voice_failed_unknown
    VoiceFailReason.PEER_NOT_FOUND -> R.string.voice_failed_peer
    VoiceFailReason.NOT_SIGNED_IN -> R.string.voice_failed_signed_out
}

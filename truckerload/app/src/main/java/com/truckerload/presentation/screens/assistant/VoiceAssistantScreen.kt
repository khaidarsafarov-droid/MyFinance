package com.truckerload.presentation.screens.assistant

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.presentation.components.SoftAppPageScaffold
import com.truckerload.presentation.privacy.PermissionRationaleDialog
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.adaptiveHorizontalPadding

@Composable
fun VoiceAssistantScreen(
    onBack: () -> Unit,
    viewModel: VoiceAssistantViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.onMicPermissionGranted() else viewModel.onMicPermissionDenied()
    }

    SoftAppPageScaffold(
        title = stringResource(R.string.assistant_title),
        subtitle = stringResource(R.string.assistant_subtitle),
        showBack = true,
        onBack = onBack,
    ) { padding ->
        VoiceAssistantBody(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = adaptiveHorizontalPadding())
                .padding(bottom = 24.dp),
            onMicTapped = {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) viewModel.onMicTapped() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
        )
    }

    val confirm = state.result as? AssistantResult.Confirm
    if (confirm != null) {
        AssistantConfirmationSheet(
            mutation = confirm.mutation,
            isSaving = state.isSaving,
            onConfirm = viewModel::confirmMutation,
            onCancel = viewModel::dismissResult,
            onFix = viewModel::fixMutation,
        )
    }

    if (state.needsMicPermission) {
        PermissionRationaleDialog(
            title = stringResource(R.string.assistant_mic_rationale_title),
            body = stringResource(R.string.assistant_mic_rationale_body),
            onContinue = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
            onDismiss = viewModel::onMicPermissionDenied,
        )
    }
}

@Composable
private fun VoiceAssistantBody(
    state: VoiceAssistantUiState,
    onMicTapped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = phaseLabel(state.phase),
            style = MaterialTheme.typography.titleMedium,
            color = tc.TextPrimary,
            textAlign = TextAlign.Center,
        )
        if (state.transcript.isNotBlank()) {
            Text(
                text = stringResource(R.string.assistant_heard, state.transcript),
                style = MaterialTheme.typography.bodyLarge,
                color = tc.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        when (val result = state.result) {
            is AssistantResult.WeeklyGross -> {
                Text(
                    text = weeklyGrossAnswerText(result.summary),
                    style = MaterialTheme.typography.bodyLarge,
                    color = tc.TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            is AssistantResult.Ambiguous -> {
                Text(
                    text = stringResource(R.string.assistant_ambiguous),
                    style = MaterialTheme.typography.bodyLarge,
                    color = tc.TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
            is AssistantResult.Saved -> {
                Text(
                    text = stringResource(R.string.assistant_saved),
                    style = MaterialTheme.typography.bodyLarge,
                    color = tc.TextPrimary,
                    textAlign = TextAlign.Center,
                )
            }
            is AssistantResult.Failed,
            is AssistantResult.Confirm,
            null,
            -> Unit
        }
        state.errorMessageRes?.let { res ->
            Text(
                text = stringResource(res),
                style = MaterialTheme.typography.bodyMedium,
                color = tc.AccentExpense,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(12.dp))
        if (state.phase == AssistantPhase.Processing) {
            CircularProgressIndicator(modifier = Modifier.size(36.dp))
        }
        FloatingActionButton(
            onClick = onMicTapped,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(
                imageVector = if (state.phase == AssistantPhase.Listening) {
                    Icons.Default.Stop
                } else {
                    Icons.Default.Mic
                },
                contentDescription = stringResource(
                    if (state.phase == AssistantPhase.Listening) {
                        R.string.assistant_stop
                    } else {
                        R.string.assistant_listen
                    },
                ),
            )
        }
        Text(
            text = stringResource(R.string.assistant_examples),
            style = MaterialTheme.typography.bodySmall,
            color = tc.TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun phaseLabel(phase: AssistantPhase): String = stringResource(
    when (phase) {
        AssistantPhase.Idle -> R.string.assistant_phase_idle
        AssistantPhase.Listening -> R.string.assistant_phase_listening
        AssistantPhase.Processing -> R.string.assistant_phase_processing
        AssistantPhase.Ready -> R.string.assistant_phase_ready
        AssistantPhase.Error -> R.string.assistant_phase_error
    },
)

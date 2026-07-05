package com.truckerload.presentation.screens.voice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckerload.R
import com.truckerload.domain.voice.CallState
import com.truckerload.domain.voice.CallStatus
import com.truckerload.presentation.di.LocalVoiceRepository
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens

@Composable
fun IncomingCallOverlay(
    onAccept: (String) -> Unit,
    viewModel: IncomingCallViewModel = viewModel(
        factory = IncomingCallViewModel.Factory(LocalVoiceRepository.current),
    ),
) {
    val call by viewModel.incomingCall.collectAsState()
    val ringing = call
    if (ringing?.status == CallStatus.RINGING && ringing.isIncoming) {
        IncomingCallScreen(
            callState = ringing,
            onAccept = { viewModel.accept(ringing.callId) { onAccept(it) } },
            onReject = { viewModel.reject(ringing.callId) },
        )
    }
}

@Composable
fun IncomingCallScreen(
    callState: CallState,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    Dialog(
        onDismissRequest = onReject,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0A)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(UiDimens.AvatarCallLarge)
                        .clip(CircleShape)
                        .background(Color(0xFF1C1C1E)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = callState.callerName.take(1).uppercase(),
                        color = Color(0xFFC9A84C),
                        fontSize = 48.sp,
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = callState.callerName,
                    style = AppTypography.CardTitle,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.incoming_call),
                    color = Color(0xFF8E8E93),
                )
                Spacer(modifier = Modifier.height(48.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    CallActionButton(
                        icon = Icons.Default.CallEnd,
                        label = stringResource(R.string.reject_call),
                        background = Color(0xFFFF3B30),
                        onClick = onReject,
                    )
                    CallActionButton(
                        icon = Icons.Default.Call,
                        label = stringResource(R.string.accept_call),
                        background = Color(0xFF34C759),
                        onClick = onAccept,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallScreen(
    callId: String,
    onBack: () -> Unit,
    viewModel: CallViewModel = viewModel(
        factory = CallViewModel.Factory(callId, LocalVoiceRepository.current),
    ),
) {
    val state by viewModel.uiState.collectAsState()
    val call = state.call
    val tc = LocalTruckColors.current
    val peerName = if (call?.isIncoming == true) call.callerName else call?.calleeName.orEmpty()
    val micGranted = rememberMicPermission()

    Scaffold(
            containerColor = Color(0xFF0A0A0A),
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.audio_call), color = tc.TextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.endCall(onBack) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = tc.AccentPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            },
        ) { padding ->
            if (!micGranted) {
                MicPermissionPrompt(modifier = Modifier.padding(padding))
                return@Scaffold
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(UiDimens.AvatarCallActive)
                        .clip(CircleShape)
                        .background(Color(0xFF1C1C1E)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(peerName.take(1).uppercase(), color = tc.AccentPrimary, fontSize = 40.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(peerName, style = AppTypography.CardTitle, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                val statusText = when (call?.status) {
                    CallStatus.RINGING -> stringResource(R.string.incoming_call)
                    CallStatus.ACTIVE -> stringResource(R.string.call_duration, formatVoiceDuration(state.durationSeconds))
                    CallStatus.ENDED -> stringResource(R.string.call_ended)
                    CallStatus.REJECTED -> stringResource(R.string.call_rejected)
                    else -> ""
                }
                Text(statusText, color = tc.TextSecondary)
                Spacer(modifier = Modifier.height(48.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    CallActionButton(
                        icon = if (state.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        label = if (state.isMuted) stringResource(R.string.unmute) else stringResource(R.string.mute),
                        background = if (state.isMuted) Color(0xFFFF3B30) else Color(0xFF3A3A3C),
                        onClick = { viewModel.toggleMute() },
                    )
                    CallActionButton(
                        icon = Icons.Default.CallEnd,
                        label = stringResource(R.string.end_call),
                        background = Color(0xFFFF3B30),
                        onClick = { viewModel.endCall(onBack) },
                    )
                }
            }
        }
}

@Composable
private fun CallActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    background: Color,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(UiDimens.CallActionButton)
                .clip(CircleShape)
                .background(background),
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(UiDimens.CallActionIcon))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = Color(0xFF8E8E93), fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

package com.truckerload.presentation.screens.voice

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.truckerload.R
import com.truckerload.domain.voice.CallPolicy
import com.truckerload.domain.voice.CallState
import com.truckerload.domain.voice.CallStatus
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.di.LocalVoiceRepository
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.SoftUiColors
import com.truckerload.presentation.theme.UiDimens

@Composable
fun IncomingCallOverlay(
    currentRoute: String?,
    myUserId: String,
    onAccept: (String) -> Unit,
    viewModel: IncomingCallViewModel = hiltViewModel(),
) {
    val call by viewModel.incomingCall.collectAsStateWithLifecycle()
    val offerSwitch by viewModel.offerSwitch.collectAsStateWithLifecycle()
    if (!CallPolicy.shouldShowIncomingOverlay(call, currentRoute, myUserId)) return
    val ringing = call ?: return
    IncomingCallScreen(
        callState = ringing,
        offerSwitch = offerSwitch,
        onAccept = { viewModel.accept(ringing.callId) { onAccept(it) } },
        onReject = { viewModel.reject(ringing.callId) },
    )
}

@Composable
fun IncomingCallScreen(
    callState: CallState,
    offerSwitch: Boolean,
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
                .background(SoftUiColors.VoiceCallBg),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CallPeerAvatar(name = callState.callerName, size = UiDimens.AvatarCallLarge)
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = callState.callerName, style = AppTypography.CardTitle, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (offerSwitch) {
                        stringResource(R.string.call_switch)
                    } else {
                        stringResource(R.string.incoming_call)
                    },
                    color = SoftUiColors.TextSecondaryDark,
                )
                Spacer(modifier = Modifier.height(48.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    CallActionButton(
                        icon = Icons.Default.CallEnd,
                        label = stringResource(R.string.reject_call),
                        background = SoftUiColors.VoiceDanger,
                        onClick = onReject,
                    )
                    CallActionButton(
                        icon = Icons.Default.Call,
                        label = if (offerSwitch) {
                            stringResource(R.string.call_switch)
                        } else {
                            stringResource(R.string.accept_call)
                        },
                        background = SoftUiColors.VoiceSuccess,
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
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    onRedialNavigate: (String) -> Unit,
    viewModel: CallViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val call = state.call
    val tc = LocalTruckColors.current
    val peerName = call?.let(CallPolicy::peerNameFor).orEmpty()
    val micGranted = rememberMicPermission()
    val ringingOutgoing = call?.status == CallStatus.RINGING && call.isIncoming != true
    BackHandler(onBack = onBack)

    Scaffold(
        containerColor = SoftUiColors.VoiceCallBg,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.audio_call), color = tc.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.call_minimize),
                            tint = tc.AccentPrimary,
                        )
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
            CallPeerAvatar(name = peerName, size = UiDimens.AvatarCallActive)
            Spacer(modifier = Modifier.height(16.dp))
            Text(peerName, style = AppTypography.CardTitle, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            val statusText = when (call?.status) {
                CallStatus.RINGING -> if (call.isIncoming) {
                    stringResource(R.string.incoming_call)
                } else {
                    stringResource(R.string.outgoing_call)
                }
                CallStatus.ACTIVE -> stringResource(
                    R.string.call_duration,
                    formatVoiceDuration(state.durationSeconds),
                )
                CallStatus.ENDED -> stringResource(R.string.call_ended)
                CallStatus.REJECTED -> stringResource(R.string.call_rejected)
                CallStatus.MISSED -> stringResource(R.string.call_missed)
                null -> ""
            }
            Text(statusText, color = tc.TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.call_connection_quality, state.audioBitrateKbps),
                color = if (state.connectionLost) SoftUiColors.VoiceDanger else tc.TextSecondary,
                fontSize = 12.sp,
            )
            if (state.connectionLost) {
                Text(stringResource(R.string.call_connection_lost), color = SoftUiColors.VoiceDanger, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(32.dp))
            if (state.offerVoiceMessage) {
                Button(
                    onClick = { viewModel.openPeerChat(onOpenChat) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.call_leave_voice))
                }
            }
            if (call?.status == CallStatus.MISSED || call?.status == CallStatus.ENDED ||
                call?.status == CallStatus.REJECTED
            ) {
                Button(
                    onClick = { viewModel.redial(onRedialNavigate) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.call_redial))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (call?.status == CallStatus.ACTIVE || ringingOutgoing) {
                    CallActionButton(
                        icon = if (state.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        label = if (state.isMuted) stringResource(R.string.unmute) else stringResource(R.string.mute),
                        background = if (state.isMuted) SoftUiColors.VoiceDanger else Color(0xFF2E3048),
                        onClick = { viewModel.toggleMute() },
                    )
                }
                if (ringingOutgoing) {
                    CallActionButton(
                        icon = Icons.Default.CallEnd,
                        label = stringResource(R.string.cancel_call),
                        background = SoftUiColors.VoiceDanger,
                        onClick = { viewModel.cancelOutgoing(onBack) },
                    )
                } else {
                    CallActionButton(
                        icon = Icons.Default.CallEnd,
                        label = stringResource(R.string.end_call),
                        background = SoftUiColors.VoiceDanger,
                        onClick = { viewModel.endCall(onBack) },
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveCallBanner(
    currentRoute: String?,
    onReturn: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val voice = LocalVoiceRepository.current
    val activeFlow = remember(voice) { voice.watchActiveCall() }
    val call by activeFlow.collectAsStateWithLifecycle(initialValue = null)
    val active = call
    if (active == null || currentRoute.orEmpty().startsWith("call") ||
        currentRoute.orEmpty().contains("/call/")
    ) {
        return
    }
    val name = CallPolicy.peerNameFor(active).ifBlank { stringResource(R.string.audio_call) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(SoftUiColors.VoiceSuccess.copy(alpha = 0.9f))
            .clickable { onReturn(active.callId) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(name, color = Color.White, style = AppTypography.Subtitle)
        Text(stringResource(R.string.call_return), color = Color.White, fontSize = 12.sp)
    }
}

@Composable
private fun CallPeerAvatar(name: String, size: androidx.compose.ui.unit.Dp) {
    val tc = LocalTruckColors.current
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .border(3.dp, tc.AccentPrimary.copy(alpha = 0.85f), CircleShape)
            .background(SoftUiColors.SurfaceDark),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.take(1).uppercase().ifBlank { "·" },
            color = tc.AccentPrimary,
            fontSize = if (size >= UiDimens.AvatarCallLarge) 48.sp else 40.sp,
        )
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
        Text(label, color = SoftUiColors.TextSecondaryDark, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

package com.truckerload.presentation.screens.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.truckerload.presentation.components.TlTextButton as TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckerload.R
import com.truckerload.domain.voice.VoiceParticipant
import com.truckerload.domain.voice.VoiceRoom
import com.truckerload.presentation.di.LocalSocialRepository
import com.truckerload.presentation.di.LocalVoiceRepository
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassClickableCard
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.SoftUiColors
import com.truckerload.presentation.theme.UiDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceRoomsScreen(
    onBack: () -> Unit,
    onOpenRoom: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VoiceRoomsViewModel = viewModel(
        factory = VoiceRoomsViewModel.Factory(LocalVoiceRepository.current),
    ),
) {
    val state by viewModel.uiState.collectAsState()
    val tc = LocalTruckColors.current
    var showCreate by remember { mutableStateOf(false) }
    var newRoomName by remember { mutableStateOf("") }
    val micGranted = rememberMicPermission()

    Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.voice_rooms), color = tc.TextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = tc.AccentPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showCreate = true },
                    containerColor = tc.AccentPrimary,
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_room), tint = Color.Black)
                }
            },
        ) { padding ->
            if (!micGranted) {
                MicPermissionPrompt(modifier = Modifier.padding(padding))
                return@Scaffold
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.rooms, key = { it.id }) { room ->
                    VoiceRoomListItem(room = room, onClick = { onOpenRoom(room.id) })
                }
            }
        }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text(stringResource(R.string.create_room)) },
            text = {
                OutlinedTextField(
                    value = newRoomName,
                    onValueChange = { newRoomName = it },
                    label = { Text(stringResource(R.string.room_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.createRoom(newRoomName) { id ->
                            showCreate = false
                            newRoomName = ""
                            onOpenRoom(id)
                        }
                    },
                ) { Text(stringResource(R.string.join_room)) }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun VoiceRoomListItem(room: VoiceRoom, onClick: () -> Unit) {
    val tc = LocalTruckColors.current
    BentoGlassClickableCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("🎙️", style = MaterialTheme.typography.headlineMedium)
            Column(modifier = Modifier.weight(1f)) {
                Text(room.name, style = AppTypography.CardTitle, color = tc.TextPrimary)
                Text(
                    text = stringResource(R.string.participants_count, room.participants.size),
                    style = AppTypography.Subtitle,
                    color = tc.TextSecondary,
                )
            }
            val speaking = room.participants.count { it.isSpeaking }
            if (speaking > 0) {
                Text(
                    text = "🟢 $speaking",
                    color = Color(0xFF34C759),
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceRoomScreen(
    roomId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VoiceRoomViewModel = viewModel(
        factory = VoiceRoomViewModel.Factory(
            roomId = roomId,
            voiceRepository = LocalVoiceRepository.current,
            socialRepository = LocalSocialRepository.current,
        ),
    ),
) {
    val state by viewModel.uiState.collectAsState()
    val tc = LocalTruckColors.current
    val room = state.room
    val micGranted = rememberMicPermission()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A1B2E)),
    ) {
            if (!micGranted) {
                MicPermissionPrompt()
                return@Box
            }
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { viewModel.leave(onBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = tc.AccentPrimary)
                    }
                    Text(
                        text = "🎙️ ${room?.name.orEmpty()}",
                        style = AppTypography.CardTitle,
                        color = tc.AccentPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(R.string.participants_count, room?.participants?.size ?: 0),
                        color = tc.TextSecondary,
                        fontSize = 12.sp,
                    )
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(room?.participants.orEmpty(), key = { it.userId }) { participant ->
                        VoiceParticipantItem(participant = participant)
                    }
                }

                BentoGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.bitrate, state.audioBitrate / 1000),
                        color = tc.TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    VoiceControlButton(
                        icon = if (state.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        label = if (state.isMuted) stringResource(R.string.unmute) else stringResource(R.string.mute),
                        tint = if (state.isMuted) Color(0xFFFF3B30) else tc.AccentPrimary,
                        onClick = { viewModel.toggleMute() },
                    )
                    VoiceControlButton(
                        icon = if (state.isDeafened) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        label = if (state.isDeafened) stringResource(R.string.undeafen) else stringResource(R.string.deafen),
                        tint = if (state.isDeafened) Color(0xFFFF3B30) else tc.TextSecondary,
                        onClick = { viewModel.toggleDeafen() },
                    )
                    VoiceControlButton(
                        icon = Icons.Default.CallEnd,
                        label = stringResource(R.string.leave_room),
                        tint = Color(0xFFFF3B30),
                        onClick = { viewModel.leave(onBack) },
                    )
                }

                Text(
                    text = stringResource(R.string.call_duration, formatVoiceDuration(state.durationSeconds)),
                    color = tc.TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    textAlign = TextAlign.Center,
                )
            }
    }
}

@Composable
private fun VoiceParticipantItem(participant: VoiceParticipant) {
    val tc = LocalTruckColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(UiDimens.AvatarVoiceGrid)
                    .clip(CircleShape)
                    .background(SoftUiColors.SurfaceDark),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = participant.displayName.take(1).uppercase(),
                    color = if (participant.isMe) tc.AccentPrimary else Color.White,
                    fontSize = 22.sp,
                )
            }
            if (participant.isSpeaking) {
                Box(
                    modifier = Modifier
                        .size(UiDimens.AvatarVoiceSpeakingRing)
                        .border(2.dp, Color(0xFF34C759), CircleShape),
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (participant.isMe) "${participant.displayName} (Вы)" else participant.displayName,
            color = if (participant.isMe) tc.AccentPrimary else tc.TextPrimary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        if (participant.isMuted) {
            Icon(
                Icons.Default.MicOff,
                contentDescription = null,
                tint = Color(0xFFFF3B30),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun VoiceControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick, modifier = Modifier.size(56.dp)) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(28.dp))
        }
        Text(label, fontSize = 10.sp, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun rememberMicPermission(): Boolean {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted = it }
    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(Manifest.permission.RECORD_AUDIO)
    }
    return granted
}

@Composable
internal fun MicPermissionPrompt(modifier: Modifier = Modifier) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.mic_permission_required),
            style = AppTypography.CardTitle,
            color = tc.TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) }) {
            Text(stringResource(R.string.grant_mic_permission), color = tc.AccentPrimary)
        }
    }
}

fun formatVoiceDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}

package com.truckerload.presentation.screens.voice

import androidx.hilt.navigation.compose.hiltViewModel
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
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckerload.R
import com.truckerload.domain.voice.VoiceParticipant
import com.truckerload.domain.voice.VoiceRoom
import com.truckerload.domain.voice.VoiceRoomRole
import com.truckerload.domain.voice.VoiceTransportKind
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
    viewModel: VoiceRoomsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val tc = LocalTruckColors.current
    var showCreate by remember { mutableStateOf(false) }
    var newRoomName by remember { mutableStateOf("") }
    var newRoomDescription by remember { mutableStateOf("") }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    val micGranted = rememberMicPermission()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearError()
    }

        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.voice_rooms), color = tc.TextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = tc.AccentPrimary)
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
                    VoiceRoomListItem(
                        room = room,
                        canDelete = room.canDelete(state.currentUserId),
                        onClick = { onOpenRoom(room.id) },
                        onDelete = { pendingDeleteId = room.id },
                    )
                }
            }
        }

    if (showCreate) {
        VoiceRoomCreateDialog(
            name = newRoomName,
            description = newRoomDescription,
            onNameChange = { newRoomName = it },
            onDescriptionChange = { newRoomDescription = it },
            onDismiss = { showCreate = false },
            onConfirm = {
                viewModel.createRoom(newRoomName, newRoomDescription) { id ->
                    showCreate = false
                    newRoomName = ""
                    newRoomDescription = ""
                    onOpenRoom(id)
                }
            },
        )
    }
    pendingDeleteId?.let { roomId ->
        val roomName = state.rooms.firstOrNull { it.id == roomId }?.name.orEmpty()
        VoiceRoomDeleteDialog(
            roomName = roomName,
            onDismiss = { pendingDeleteId = null },
            onConfirm = {
                viewModel.deleteRoom(roomId)
                pendingDeleteId = null
            },
        )
    }
}

@Composable
private fun VoiceRoomListItem(
    room: VoiceRoom,
    canDelete: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val tc = LocalTruckColors.current
    BentoGlassClickableCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Mic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(room.name, style = AppTypography.CardTitle, color = tc.TextPrimary)
                if (room.description.isNotBlank()) {
                    Text(
                        text = room.description,
                        style = AppTypography.Subtitle,
                        color = tc.TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = pluralStringResource(R.plurals.participants_count, room.participants.size, room.participants.size),
                    style = AppTypography.Subtitle,
                    color = tc.TextSecondary,
                )
            }
            val speaking = room.participants.count { it.isSpeaking }
            if (speaking > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Circle,
                        contentDescription = null,
                        tint = SoftUiColors.VoiceSuccess,
                        modifier = Modifier.size(8.dp),
                    )
                    Text(
                        text = speaking.toString(),
                        color = SoftUiColors.VoiceSuccess,
                        fontSize = 12.sp,
                    )
                }
            }
            if (canDelete) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.common_delete),
                        tint = SoftUiColors.VoiceDanger,
                    )
                }
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
    viewModel: VoiceRoomViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val tc = LocalTruckColors.current
    val room = state.room
    val micGranted = rememberMicPermission()
    val canManage = room?.canManage(state.currentUserId) == true
    val canDelete = room?.canDelete(state.currentUserId) == true
    var menuExpanded by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var showModerator by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SoftUiColors.VoiceCallBg),
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = tc.AccentPrimary)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Mic,
                                contentDescription = null,
                                tint = tc.AccentPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = room?.name.orEmpty(),
                                style = AppTypography.CardTitle,
                                color = tc.AccentPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (!room?.description.isNullOrBlank()) {
                            Text(
                                text = room?.description.orEmpty(),
                                color = tc.TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Text(
                        text = pluralStringResource(
                            R.plurals.participants_count,
                            room?.participants?.size ?: 0,
                            room?.participants?.size ?: 0,
                        ),
                        color = tc.TextSecondary,
                        fontSize = 12.sp,
                    )
                    if (canManage) {
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.voice_room_manage),
                                    tint = tc.AccentPrimary,
                                )
                            }
                            VoiceRoomOwnerMenu(
                                expanded = menuExpanded,
                                canDelete = canDelete,
                                onDismiss = { menuExpanded = false },
                                onEdit = {
                                    editName = room?.name.orEmpty()
                                    editDescription = room?.description.orEmpty()
                                    showEdit = true
                                },
                                onModerator = { showModerator = true },
                                onMuteAll = { viewModel.muteAll() },
                                onDelete = { showDelete = true },
                            )
                        }
                    }
                }
                state.errorMessage?.let { message ->
                    Text(
                        text = if (message == "room_gone") {
                            stringResource(R.string.voice_room_gone)
                        } else if (message == "room_full") {
                            stringResource(R.string.voice_room_full)
                        } else {
                            message
                        },
                        color = SoftUiColors.VoiceDanger,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp),
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
                        VoiceParticipantItem(
                            participant = participant,
                            isModerator = participant.userId.isNotBlank() &&
                                participant.userId == room?.moderatorId,
                            roleLabel = if (participant.isMe) {
                                if (state.role == VoiceRoomRole.LISTENER) {
                                    stringResource(R.string.voice_role_listener)
                                } else {
                                    stringResource(R.string.voice_role_speaker)
                                }
                            } else {
                                null
                            },
                            onKick = if (canManage && !participant.isMe) {
                                { viewModel.kick(participant.userId) }
                            } else {
                                null
                            },
                        )
                    }
                }

                BentoGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    val transportLabel = when (state.transport) {
                        VoiceTransportKind.LIVEKIT -> stringResource(R.string.voice_transport_sfu)
                        VoiceTransportKind.MESH -> stringResource(R.string.voice_transport_mesh)
                        VoiceTransportKind.NONE -> ""
                    }
                    val bitrateText = if (state.role == VoiceRoomRole.LISTENER) {
                        stringResource(R.string.voice_listener_bitrate)
                    } else {
                        stringResource(R.string.bitrate, state.audioBitrate / 1000)
                    }
                    Text(
                        text = listOf(bitrateText, transportLabel).filter { it.isNotBlank() }.joinToString(" · "),
                        color = tc.TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }

                VoiceRoleToggle(
                    role = state.role,
                    onRoleChange = viewModel::setRole,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    VoiceControlButton(
                        icon = if (state.isMuted) Icons.Default.MicOff else Icons.Outlined.Mic,
                        label = if (state.isMuted) stringResource(R.string.unmute) else stringResource(R.string.mute),
                        tint = if (state.isMuted) SoftUiColors.VoiceDanger else tc.AccentPrimary,
                        onClick = { viewModel.toggleMute() },
                        enabled = state.role == VoiceRoomRole.SPEAKER,
                    )
                    VoiceControlButton(
                        icon = if (state.isDeafened) {
                            Icons.AutoMirrored.Filled.VolumeOff
                        } else {
                            Icons.AutoMirrored.Filled.VolumeUp
                        },
                        label = if (state.isDeafened) stringResource(R.string.undeafen) else stringResource(R.string.deafen),
                        tint = if (state.isDeafened) SoftUiColors.VoiceDanger else tc.TextSecondary,
                        onClick = { viewModel.toggleDeafen() },
                    )
                    VoiceControlButton(
                        icon = Icons.Default.CallEnd,
                        label = stringResource(R.string.leave_room),
                        tint = SoftUiColors.VoiceDanger,
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
    if (showEdit) {
        VoiceRoomEditDialog(
            name = editName,
            description = editDescription,
            onNameChange = { editName = it },
            onDescriptionChange = { editDescription = it },
            onDismiss = { showEdit = false },
            onSave = {
                viewModel.updateRoom(name = editName.trim(), description = editDescription.trim())
                showEdit = false
            },
        )
    }
    if (showModerator) {
        VoiceRoomModeratorDialog(
            participants = room?.participants.orEmpty(),
            currentModeratorId = room?.moderatorId.orEmpty(),
            onDismiss = { showModerator = false },
            onPick = { userId ->
                viewModel.updateRoom(moderatorId = userId)
                showModerator = false
            },
            onClear = {
                viewModel.updateRoom(clearModerator = true)
                showModerator = false
            },
        )
    }
    if (showDelete) {
        VoiceRoomDeleteDialog(
            roomName = room?.name.orEmpty(),
            onDismiss = { showDelete = false },
            onConfirm = {
                showDelete = false
                viewModel.deleteRoom(onBack)
            },
        )
    }
}


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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.truckerload.R
import com.truckerload.domain.voice.VoiceParticipant
import com.truckerload.domain.voice.VoiceRoomRole
import com.truckerload.presentation.components.TlTextButton as TextButton
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.SoftUiColors
import com.truckerload.presentation.theme.UiDimens

@Composable
fun VoiceRoomCreateDialog(
    name: String,
    description: String,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_room)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.room_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text(stringResource(R.string.voice_room_description)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.join_room))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

@Composable
fun VoiceRoomEditDialog(
    name: String,
    description: String,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.voice_room_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.room_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text(stringResource(R.string.voice_room_description)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

@Composable
fun VoiceRoomDeleteDialog(
    roomName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.voice_room_delete_title)) },
        text = { Text(stringResource(R.string.voice_room_delete_message, roomName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.common_delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

@Composable
fun VoiceRoomModeratorDialog(
    participants: List<VoiceParticipant>,
    currentModeratorId: String,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
    onClear: () -> Unit,
) {
    val others = participants.filterNot { it.isMe }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.voice_room_assign_moderator)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (others.isEmpty()) {
                    Text(stringResource(R.string.voice_room_no_other_participants))
                } else {
                    others.forEach { participant ->
                        TextButton(
                            onClick = { onPick(participant.userId) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            val label = if (participant.userId == currentModeratorId) {
                                stringResource(R.string.voice_room_moderator_current, participant.displayName)
                            } else {
                                participant.displayName
                            }
                            Text(label)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (currentModeratorId.isNotBlank()) {
                TextButton(onClick = onClear) { Text(stringResource(R.string.voice_room_clear_moderator)) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

@Composable
fun VoiceRoomOwnerMenu(
    expanded: Boolean,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onModerator: () -> Unit,
    onMuteAll: () -> Unit = {},
    onDelete: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.voice_room_edit)) },
            onClick = {
                onDismiss()
                onEdit()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.voice_mute_all)) },
            onClick = {
                onDismiss()
                onMuteAll()
            },
        )
        if (canDelete) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.voice_room_assign_moderator)) },
                onClick = {
                    onDismiss()
                    onModerator()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.common_delete)) },
                onClick = {
                    onDismiss()
                    onDelete()
                },
            )
        }
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
fun rememberMicCallGate(): (() -> Unit) -> Unit {
    val context = LocalContext.current
    val pending = remember { mutableStateOf<(() -> Unit)?>(null) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { ok ->
        if (ok) pending.value?.invoke()
        pending.value = null
    }
    return { action ->
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            action()
        } else {
            pending.value = action
            launcher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}

@Composable
internal fun MicPermissionPrompt(modifier: Modifier = Modifier) {
    val tc = LocalTruckColors.current
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

@Composable
internal fun VoiceParticipantItem(
    participant: VoiceParticipant,
    isModerator: Boolean = false,
    roleLabel: String? = null,
    onKick: (() -> Unit)? = null,
) {
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
                        .border(2.dp, SoftUiColors.VoiceSuccess, CircleShape),
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (participant.isMe) "${participant.displayName} (${stringResource(R.string.social_you)})" else participant.displayName,
            color = if (participant.isMe) tc.AccentPrimary else tc.TextPrimary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        if (isModerator) {
            Text(
                text = stringResource(R.string.voice_room_moderator_badge),
                color = tc.AccentPrimary,
                fontSize = 9.sp,
            )
        }
        if (!roleLabel.isNullOrBlank()) {
            Text(
                text = roleLabel,
                color = tc.TextSecondary,
                fontSize = 9.sp,
            )
        }
        if (participant.isMuted) {
            Icon(
                Icons.Default.MicOff,
                contentDescription = null,
                tint = SoftUiColors.VoiceDanger,
                modifier = Modifier.size(14.dp),
            )
        }
        if (onKick != null) {
            Text(
                text = stringResource(R.string.voice_kick),
                color = SoftUiColors.VoiceDanger,
                fontSize = 9.sp,
                modifier = Modifier.clickable(onClick = onKick),
            )
        }
    }
}

@Composable
internal fun VoiceRoleToggle(
    role: VoiceRoomRole,
    onRoleChange: (VoiceRoomRole) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        VoiceRoleChip(
            selected = role == VoiceRoomRole.SPEAKER,
            label = stringResource(R.string.voice_role_speaker),
            onClick = { onRoleChange(VoiceRoomRole.SPEAKER) },
            modifier = Modifier.weight(1f),
            selectedColor = tc.AccentPrimary,
        )
        VoiceRoleChip(
            selected = role == VoiceRoomRole.LISTENER,
            label = stringResource(R.string.voice_role_listener),
            onClick = { onRoleChange(VoiceRoomRole.LISTENER) },
            modifier = Modifier.weight(1f),
            selectedColor = tc.TextSecondary,
        )
    }
}

@Composable
private fun VoiceRoleChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color,
) {
    val tc = LocalTruckColors.current
    val border = if (selected) selectedColor else tc.TextSecondary.copy(alpha = 0.4f)
    Text(
        text = label,
        color = if (selected) selectedColor else tc.TextSecondary,
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(CircleShape)
            .border(1.dp, border, CircleShape)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    )
}

@Composable
internal fun VoiceControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val color = if (enabled) tint else tint.copy(alpha = 0.4f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
    ) {
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(64.dp)) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(32.dp))
        }
        Text(label, fontSize = 10.sp, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

fun formatVoiceDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}

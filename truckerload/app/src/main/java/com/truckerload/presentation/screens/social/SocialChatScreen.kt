package com.truckerload.presentation.screens.social

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.data.community.ActiveCommunityChat
import com.truckerload.data.social.VoiceNoteRecorder
import com.truckerload.domain.voice.CallConfig
import com.truckerload.domain.voice.CallPolicy
import com.truckerload.domain.voice.CallPrivacy
import com.truckerload.presentation.di.LocalCallPrivacyStore
import com.truckerload.presentation.di.LocalVoiceRepository
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialChatScreen(
    chatId: String,
    onBack: () -> Unit,
    onOpenPeerProfile: (String) -> Unit = {},
    onStartCall: (String, String) -> Unit = { _, _ -> },
    onOpenVoiceRoom: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SocialChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tc = LocalTruckColors.current
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val voiceRecorder = remember { VoiceNoteRecorder(context) }
    var isRecording by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val bitmap = AvatarCropUtils.decodeSampledBitmap(stream) ?: return@rememberLauncherForActivityResult
            viewModel.sendImage(bitmap, uiState.inputText)
            viewModel.setInput("")
        }
    }

    LaunchedEffect(uiState.allMessages.size) {
        if (uiState.allMessages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.allMessages.lastIndex)
        }
    }

    DisposableEffect(chatId) {
        ActiveCommunityChat.chatId = chatId
        onDispose {
            if (voiceRecorder.isRecording()) {
                voiceRecorder.stop()
            }
            if (ActiveCommunityChat.chatId == chatId) {
                ActiveCommunityChat.chatId = null
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = BentoGlassTheme.ScreenBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.chatTitle)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (uiState.chatRating > 0) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = tc.TextSecondary,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = "%.1f".format(uiState.chatRating),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = tc.TextSecondary,
                                )
                            }
                            if (uiState.participantCount > 0) {
                                if (uiState.chatRating > 0) {
                                    Text(
                                        text = "·",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = tc.TextSecondary,
                                    )
                                }
                                Text(
                                    text = "${uiState.participantCount} ${stringResource(R.string.social_participants)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = tc.TextSecondary,
                                )
                            }
                            if (uiState.onlineCount > 0) {
                                if (uiState.chatRating > 0 || uiState.participantCount > 0) {
                                    Text(
                                        text = "·",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = tc.TextSecondary,
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.Circle,
                                    contentDescription = null,
                                    tint = tc.AccentProfit,
                                    modifier = Modifier.size(8.dp),
                                )
                                Text(
                                    text = uiState.onlineCount.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = tc.TextSecondary,
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    val privacyStore = LocalCallPrivacyStore.current
                    val showPrivateCall = uiState.peerId != null && CallPolicy.canShowCallButton(
                        blocked = uiState.isBlocked,
                        calleePrivacy = CallPrivacy.EVERYONE,
                        isContact = uiState.isContact,
                    )
                    val showGroupCall = !uiState.isPrivate && uiState.peerId == null &&
                        CallPolicy.canStartGroupCall(
                            callsEnabled = privacyStore.groupCallsEnabled(chatId),
                            adminsOnly = privacyStore.groupAdminsOnly(chatId),
                            isAdmin = uiState.isGroupManager,
                            isMember = true,
                        )
                    if (showPrivateCall) {
                        IconButton(
                            onClick = {
                                uiState.peerId?.let { onStartCall(it, uiState.peerName.ifBlank { uiState.chatTitle }) }
                            },
                        ) {
                            Icon(Icons.Default.Call, contentDescription = stringResource(R.string.social_call_peer))
                        }
                    } else if (showGroupCall || (!uiState.isPrivate && privacyStore.groupCallsEnabled(chatId))) {
                        IconButton(
                            onClick = { viewModel.startGroupCall(onOpenVoiceRoom) },
                        ) {
                            Icon(Icons.Default.Call, contentDescription = stringResource(R.string.group_start_call))
                        }
                    }
                    ChatSafetyMenu(
                        enabled = uiState.peerId != null,
                        onOpenProfile = { uiState.peerId?.let(onOpenPeerProfile) },
                        onBlock = { viewModel.blockPeer(onBack) },
                        onReport = viewModel::reportPeer,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BentoGlassTheme.ScreenBackground,
                    titleContentColor = tc.TextPrimary,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val feedback = uiState.errorMessage
            if (!feedback.isNullOrBlank()) {
                Text(
                    text = if (feedback == "reported") {
                        stringResource(R.string.social_report_sent)
                    } else {
                        feedback
                    },
                    color = if (feedback == "reported") tc.AccentPrimary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            if (!uiState.isPrivate) {
                val voice = LocalVoiceRepository.current
                val groupRoom by voice.watchRoom(CallConfig.groupRoomId(chatId), "")
                    .collectAsStateWithLifecycle(initialValue = null)
                val activeGroup = groupRoom
                if (activeGroup?.isActive == true && activeGroup.participants.isNotEmpty()) {
                    TextButton(
                        onClick = { onOpenVoiceRoom(activeGroup.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.group_call_join))
                    }
                }
            }
            uiState.replyTo?.let { reply ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(tc.SurfaceSecondary)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.social_reply_to), style = MaterialTheme.typography.labelSmall, color = tc.AccentPrimary)
                        Text(reply.text, style = MaterialTheme.typography.bodySmall, color = tc.TextSecondary, maxLines = 1)
                    }
                    IconButton(onClick = viewModel::cancelReply) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_close))
                    }
                }
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (uiState.hasMore) {
                    item {
                        TextButton(
                            onClick = viewModel::loadMore,
                            enabled = !uiState.isLoadingMore,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.social_load_more))
                        }
                    }
                }
                items(uiState.allMessages, key = { it.id }) { message ->
                    SocialMessageBubble(
                        message = message,
                        onReply = { viewModel.setReplyTo(message) },
                        onReaction = { emoji -> viewModel.addReaction(message.id, emoji) },
                        onRedial = uiState.peerId?.let { peer ->
                            { onStartCall(peer, uiState.peerName.ifBlank { uiState.chatTitle }) }
                        },
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                IconButton(onClick = { imagePicker.launch("image/*") }) {
                    Icon(Icons.Default.Image, contentDescription = stringResource(R.string.social_attach_image), tint = tc.AccentPrimary)
                }
                IconButton(
                    onClick = {
                        if (isRecording) {
                            val (file, durationMs) = voiceRecorder.stop()
                            isRecording = false
                            file?.let { viewModel.sendVoiceNote(it, durationMs) }
                        } else {
                            voiceRecorder.start()
                            isRecording = true
                        }
                    },
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isRecording) {
                            stringResource(R.string.social_stop_recording)
                        } else {
                            stringResource(R.string.social_record_voice)
                        },
                        tint = if (isRecording) tc.AccentPrimary else tc.TextSecondary,
                    )
                }
                OutlinedTextField(
                    value = uiState.inputText,
                    onValueChange = viewModel::setInput,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.social_message_hint)) },
                    colors = AppTextFieldDefaults.outlined(),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                )
                IconButton(
                    onClick = viewModel::sendMessage,
                    enabled = uiState.inputText.isNotBlank(),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.social_send),
                        tint = if (uiState.inputText.isNotBlank()) tc.AccentPrimary else tc.TextSecondary,
                    )
                }
            }
        }
    }
}

package com.truckerload.presentation.screens.social

import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.truckerload.presentation.components.TlTextButton as TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.truckerload.R
import com.truckerload.data.social.VoiceNoteRecorder
import com.truckerload.domain.social.MessageType
import com.truckerload.domain.social.ReactionEmoji
import com.truckerload.domain.social.SocialMessage
import com.truckerload.presentation.di.LocalSocialRepository
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialChatScreen(
    chatId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SocialChatViewModel = viewModel(
        key = chatId,
        factory = SocialChatViewModel.Factory(chatId, LocalSocialRepository.current),
    ),
) {
    val uiState by viewModel.uiState.collectAsState()
    val tc = LocalTruckColors.current
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val voiceRecorder = remember { VoiceNoteRecorder(context) }
    var isRecording by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val bitmap = BitmapFactory.decodeStream(stream) ?: return@rememberLauncherForActivityResult
            viewModel.sendImage(bitmap, uiState.inputText)
            viewModel.setInput("")
        }
    }

    LaunchedEffect(uiState.allMessages.size) {
        if (uiState.allMessages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.allMessages.lastIndex)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (voiceRecorder.isRecording()) {
                voiceRecorder.stop()
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
                        Text(
                            text = buildString {
                                if (uiState.chatRating > 0) append("⭐ ${"%.1f".format(uiState.chatRating)} · ")
                                append("${uiState.participantCount} ${stringResource(R.string.social_participants)}")
                                if (uiState.onlineCount > 0) append(" · 🟢 ${uiState.onlineCount}")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = tc.TextSecondary,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
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
                        Icon(Icons.Default.Close, contentDescription = null)
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = tc.TextPrimary,
                        unfocusedTextColor = tc.TextPrimary,
                        cursorColor = tc.AccentPrimary,
                        focusedBorderColor = tc.AccentPrimary,
                        unfocusedBorderColor = tc.Divider,
                    ),
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SocialMessageBubble(
    message: SocialMessage,
    onReply: () -> Unit,
    onReaction: (String) -> Unit,
) {
    val tc = LocalTruckColors.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    val time = remember(message.sentAt) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.sentAt))
    }
    val bubbleColor = when {
        message.isAnnouncement -> tc.AccentPrimary.copy(alpha = 0.15f)
        message.isMine -> tc.AccentPrimary.copy(alpha = 0.2f)
        else -> tc.SurfaceSecondary
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(4.dp)
                .clickable(onClick = onReply),
            horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start,
        ) {
            if (!message.isMine) {
                Text(
                    text = "👤 ${message.senderName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = tc.TextSecondary,
                )
            }
            message.replyPreview?.let { preview ->
                Text(
                    text = "↩ $preview",
                    style = MaterialTheme.typography.labelSmall,
                    color = tc.AccentPrimary,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            if (message.isAnnouncement) {
                Text("📌 ${stringResource(R.string.social_announcement)}", style = MaterialTheme.typography.labelSmall, color = tc.AccentPrimary)
            }
            Column(
                modifier = Modifier
                    .background(color = bubbleColor, shape = RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                when (message.messageType) {
                    MessageType.IMAGE -> {
                        message.attachmentUrl?.let { path ->
                            AsyncImage(
                                model = File(path),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop,
                            )
                        }
                        if (message.text.isNotBlank()) {
                            Text(
                                text = message.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = tc.TextPrimary,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                    MessageType.VOICE -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.clickable {
                                val path = message.attachmentUrl ?: return@clickable
                                mediaPlayer?.release()
                                mediaPlayer = MediaPlayer().apply {
                                    setDataSource(path)
                                    prepare()
                                    start()
                                    setOnCompletionListener { release() }
                                }
                            },
                        ) {
                            Text("🎤", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${stringResource(R.string.social_voice_message)} · ${message.durationMs / 1000}s",
                                style = MaterialTheme.typography.bodyMedium,
                                color = tc.TextPrimary,
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = tc.TextPrimary,
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "🕐 $time", style = MaterialTheme.typography.labelSmall, color = tc.TextSecondary)
                message.locationLabel?.let {
                    Text(text = "📍 $it", style = MaterialTheme.typography.labelSmall, color = tc.TextSecondary)
                }
            }
            if (message.hashtags.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    message.hashtags.forEach { tag ->
                        Text("#$tag", style = MaterialTheme.typography.labelSmall, color = tc.AccentPrimary, fontWeight = FontWeight.Medium)
                    }
                }
            }
            if (message.reactions.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                    message.reactions.forEach { reaction ->
                        Text(
                            text = "${reaction.reaction} ${reaction.count}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (reaction.includesMe) tc.AccentPrimary else tc.TextSecondary,
                            modifier = Modifier
                                .background(tc.SurfaceSecondary, RoundedCornerShape(12.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                ReactionEmoji.entries.forEach { reaction ->
                    Text(
                        text = reaction.emoji,
                        modifier = Modifier
                            .clickable { onReaction(reaction.emoji) }
                            .padding(2.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

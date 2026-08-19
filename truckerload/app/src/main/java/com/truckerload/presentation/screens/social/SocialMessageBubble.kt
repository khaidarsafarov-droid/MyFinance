package com.truckerload.presentation.screens.social

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.truckerload.R
import com.truckerload.domain.social.MessageType
import com.truckerload.domain.social.ReactionEmoji
import com.truckerload.domain.social.SocialMessage
import com.truckerload.presentation.theme.LocalTruckColors
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SocialMessageBubble(
    message: SocialMessage,
    onReply: () -> Unit,
    onReaction: (String) -> Unit,
) {
    val tc = LocalTruckColors.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var pickerOpen by remember { mutableStateOf(false) }
    val time = remember(message.sentAt) {
        if (message.sentAt <= 0L) {
            ""
        } else {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.sentAt))
        }
    }
    val bubbleColor = when {
        message.isAnnouncement -> tc.AccentPrimary.copy(alpha = 0.15f)
        message.isMine -> tc.AccentPrimary.copy(alpha = 0.2f)
        else -> tc.SurfaceSecondary
    }
    val visibleReactions = message.reactions.filter { it.count > 0 }

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
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onReply,
                ),
            horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start,
        ) {
            if (!message.isMine) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = tc.TextSecondary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        color = tc.TextSecondary,
                    )
                }
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PushPin,
                        contentDescription = null,
                        tint = tc.AccentPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = stringResource(R.string.social_announcement),
                        style = MaterialTheme.typography.labelSmall,
                        color = tc.AccentPrimary,
                    )
                }
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
                                contentDescription = stringResource(R.string.social_attach_photo),
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
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = tc.AccentPrimary,
                                modifier = Modifier.size(18.dp),
                            )
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
                if (time.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = tc.TextSecondary,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(text = time, style = MaterialTheme.typography.labelSmall, color = tc.TextSecondary)
                    }
                }
                message.locationLabel?.let {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = tc.TextSecondary,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(text = it, style = MaterialTheme.typography.labelSmall, color = tc.TextSecondary)
                    }
                }
            }
            if (message.hashtags.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    message.hashtags.forEach { tag ->
                        Text("#$tag", style = MaterialTheme.typography.labelSmall, color = tc.AccentPrimary, fontWeight = FontWeight.Medium)
                    }
                }
            }
            if (visibleReactions.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                    visibleReactions.forEach { reaction ->
                        Text(
                            text = "${reaction.reaction} ${reaction.count}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (reaction.includesMe) tc.AccentPrimary else tc.TextSecondary,
                            modifier = Modifier
                                .background(tc.SurfaceSecondary, RoundedCornerShape(12.dp))
                                .clickable { onReaction(reaction.reaction) }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.EmojiEmotions,
                    contentDescription = stringResource(R.string.social_pick_reaction),
                    tint = tc.TextSecondary,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { pickerOpen = !pickerOpen },
                )
                if (pickerOpen) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ReactionEmoji.entries.forEach { reaction ->
                            Text(
                                text = reaction.emoji,
                                modifier = Modifier
                                    .clickable {
                                        onReaction(reaction.emoji)
                                        pickerOpen = false
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

package com.truckerload.presentation.screens.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.social.ChatType
import com.truckerload.domain.social.SocialChat
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassClickableCard
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun ChatListItem(chat: SocialChat, onClick: () -> Unit) {
    val tc = LocalTruckColors.current
    val time = remember(chat.lastMessageAt) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(chat.lastMessageAt))
    }
    BentoGlassClickableCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = UiDimens.ChatListItemMinHeight)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = if (chat.type == ChatType.PRIVATE) Icons.Default.Person else Icons.Default.Groups,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(chat.title, style = AppTypography.CardTitle, color = tc.TextPrimary)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (chat.rating > 0) {
                        val trustLabel = stringResource(
                            R.string.trust_rating_value,
                            "%.1f".format(chat.rating),
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.semantics { contentDescription = trustLabel },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = tc.TextSecondary,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = stringResource(
                                    R.string.trust_rating_short,
                                    "%.1f".format(chat.rating),
                                ),
                                style = AppTypography.Subtitle,
                                color = tc.TextSecondary,
                            )
                        }
                    }
                    if (chat.participantCount > 0) {
                        if (chat.rating > 0) {
                            Text("·", style = AppTypography.Subtitle, color = tc.TextSecondary)
                        }
                        Icon(
                            imageVector = Icons.Filled.People,
                            contentDescription = null,
                            tint = tc.TextSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = chat.participantCount.toString(),
                            style = AppTypography.Subtitle,
                            color = tc.TextSecondary,
                        )
                    }
                    if (chat.lastMessage.isNotBlank()) {
                        if (chat.rating > 0 || chat.participantCount > 0) {
                            Text("·", style = AppTypography.Subtitle, color = tc.TextSecondary)
                        }
                        Text(
                            text = chat.lastMessage,
                            style = AppTypography.Subtitle,
                            color = tc.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                if (chat.description.isNotBlank()) {
                    Text(
                        text = chat.description,
                        style = AppTypography.Subtitle,
                        color = tc.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (chat.lastMessageAt > 0L) {
                    Text(time, style = AppTypography.Subtitle, color = tc.TextSecondary)
                }
                if (chat.unreadCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.MarkChatUnread,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(chat.unreadCount.toString(), style = AppTypography.Subtitle)
                    }
                } else if (chat.onlineCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Circle,
                            contentDescription = null,
                            tint = tc.AccentProfit,
                            modifier = Modifier.size(8.dp),
                        )
                        Text(chat.onlineCount.toString(), style = AppTypography.Subtitle)
                    }
                }
            }
        }
    }
}

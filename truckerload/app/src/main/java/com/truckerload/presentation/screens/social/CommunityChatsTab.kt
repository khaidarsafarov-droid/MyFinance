package com.truckerload.presentation.screens.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.social.ChatType
import com.truckerload.domain.social.SocialChat
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassClickableCard
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlTextButton as TextButton

@Composable
internal fun ChatsTabContent(
    groupChats: List<SocialChat>,
    privateChats: List<SocialChat>,
    peers: List<com.truckerload.domain.social.SocialPeerProfile>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onCreateGroup: (String) -> Unit,
    onCreatePrivateWithPeer: (String) -> Unit,
    onChatClick: (String) -> Unit,
    onOpenVoiceRooms: () -> Unit,
    onOpenFriends: () -> Unit,
) {
    val tc = LocalTruckColors.current
    var showGroupDialog by remember { mutableStateOf(false) }
    var showPeerPicker by remember { mutableStateOf(false) }
    var chatNameInput by remember { mutableStateOf("") }

    if (showGroupDialog) {
        AlertDialog(
            onDismissRequest = { showGroupDialog = false },
            title = { Text(stringResource(R.string.social_create_group)) },
            text = {
                OutlinedTextField(
                    value = chatNameInput,
                    onValueChange = { chatNameInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.social_chat_name_hint)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCreateGroup(chatNameInput)
                        chatNameInput = ""
                        showGroupDialog = false
                    },
                    enabled = chatNameInput.isNotBlank(),
                ) {
                    Text(stringResource(R.string.common_add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showGroupDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (showPeerPicker) {
        AlertDialog(
            onDismissRequest = { showPeerPicker = false },
            title = { Text(stringResource(R.string.social_select_peer)) },
            text = {
                if (peers.isEmpty()) {
                    Text(stringResource(R.string.community_empty_peers))
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(peers, key = { it.id }) { peer ->
                            BentoGlassClickableCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    onCreatePrivateWithPeer(peer.id)
                                    showPeerPicker = false
                                },
                            ) {
                                Text(
                                    text = peer.displayName,
                                    modifier = Modifier.padding(16.dp),
                                    color = tc.TextPrimary,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (peers.isEmpty()) {
                    TextButton(onClick = {
                        showPeerPicker = false
                        onOpenFriends()
                    }) {
                        Text(stringResource(R.string.community_add_friends))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPeerPicker = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.social_search_chats)) },
                singleLine = true,
                colors = AppTextFieldDefaults.outlined(),
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        chatNameInput = ""
                        showGroupDialog = true
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.social_create_group))
                }
                Button(
                    onClick = {
                        chatNameInput = ""
                        showPeerPicker = true
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.social_create_private))
                }
            }
        }
        item {
            Text(
                text = stringResource(R.string.group_chat),
                style = AppTypography.CardTitle,
            )
        }
        items(groupChats, key = { it.id }) { chat ->
            ChatListItem(chat = chat, onClick = { onChatClick(chat.id) })
        }
        if (groupChats.isEmpty() && privateChats.isEmpty()) {
            item {
                CommunityAddFriendsHint(onOpenFriends = onOpenFriends)
            }
        }
        item {
            Text(
                text = stringResource(R.string.private_chat),
                style = AppTypography.CardTitle,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        items(privateChats, key = { it.id }) { chat ->
            ChatListItem(chat = chat, onClick = { onChatClick(chat.id) })
        }
        item {
            BentoGlassClickableCard(modifier = Modifier.fillMaxWidth(), onClick = onOpenVoiceRooms) {
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
                        Text(
                            text = stringResource(R.string.voice_rooms),
                            style = AppTypography.CardTitle,
                            color = tc.TextPrimary,
                        )
                        Text(
                            text = stringResource(R.string.voice_rooms_subtitle),
                            style = AppTypography.Subtitle,
                            color = tc.TextSecondary,
                        )
                    }
                }
            }
        }
    }
}

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
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = tc.TextSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "%.1f".format(chat.rating),
                        style = AppTypography.Subtitle,
                        color = tc.TextSecondary,
                    )
                    Text("·", style = AppTypography.Subtitle, color = tc.TextSecondary)
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
                    Text("·", style = AppTypography.Subtitle, color = tc.TextSecondary)
                    Text(
                        text = chat.lastMessage,
                        style = AppTypography.Subtitle,
                        color = tc.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
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
                Text(time, style = AppTypography.Subtitle, color = tc.TextSecondary)
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

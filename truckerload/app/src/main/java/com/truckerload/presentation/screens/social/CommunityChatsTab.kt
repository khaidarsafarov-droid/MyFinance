package com.truckerload.presentation.screens.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.data.preferences.CommunityHintArea
import com.truckerload.domain.social.SocialChat
import com.truckerload.presentation.di.LocalSettingsDataStore
import com.truckerload.presentation.screens.social.friends.FriendsDirectoryPanel
import com.truckerload.presentation.screens.social.friends.FriendsDirectoryViewModel
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassClickableCard
import com.truckerload.presentation.theme.LocalTruckColors
import kotlinx.coroutines.launch
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlTextButton as TextButton

@Composable
internal fun ChatsTabContent(
    groupChats: List<SocialChat>,
    privateChats: List<SocialChat>,
    peers: List<com.truckerload.domain.social.SocialPeerProfile>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onCreateGroup: (String, String) -> Unit,
    onCreatePrivateWithPeer: (String) -> Unit,
    onChatClick: (String) -> Unit,
    onOpenFriends: () -> Unit,
    friendsDirectoryViewModel: FriendsDirectoryViewModel,
) {
    val tc = LocalTruckColors.current
    val settingsDataStore = LocalSettingsDataStore.current
    val hintScope = rememberCoroutineScope()
    fun markChatsUsed() {
        hintScope.launch { settingsDataStore.markCommunityHintUsed(CommunityHintArea.CHATS) }
    }
    val hasChats = groupChats.isNotEmpty() || privateChats.isNotEmpty()
    LaunchedEffect(hasChats) {
        if (hasChats) settingsDataStore.markCommunityHintUsed(CommunityHintArea.CHATS)
    }
    var showGroupDialog by remember { mutableStateOf(false) }
    var showPeerPicker by remember { mutableStateOf(false) }
    var chatNameInput by remember { mutableStateOf("") }
    var chatDescriptionInput by remember { mutableStateOf("") }

    if (showGroupDialog) {
        AlertDialog(
            onDismissRequest = { showGroupDialog = false },
            title = { Text(stringResource(R.string.social_create_group)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = chatNameInput,
                        onValueChange = { chatNameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.social_chat_name_hint)) },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = chatDescriptionInput,
                        onValueChange = { chatDescriptionInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.social_group_description)) },
                        minLines = 2,
                        maxLines = 4,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCreateGroup(chatNameInput, chatDescriptionInput)
                        chatNameInput = ""
                        chatDescriptionInput = ""
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
                    TextButton(onClick = { showPeerPicker = false }) {
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
            Text(
                text = stringResource(R.string.social_chat_safety_hint),
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        markChatsUsed()
                        chatNameInput = ""
                        showGroupDialog = true
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.social_create_group))
                }
                Button(
                    onClick = {
                        markChatsUsed()
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
            FriendsDirectoryPanel(
                viewModel = friendsDirectoryViewModel,
                onOpenMap = onOpenFriends,
            )
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
    }
}

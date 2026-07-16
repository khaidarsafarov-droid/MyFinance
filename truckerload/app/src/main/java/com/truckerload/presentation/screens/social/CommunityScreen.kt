package com.truckerload.presentation.screens.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import com.truckerload.presentation.components.LocalOpenDrawer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.truckerload.presentation.components.TlButton as Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.truckerload.presentation.components.TlTextButton as TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckerload.R
import com.truckerload.domain.social.LeaderboardCategory
import com.truckerload.domain.social.SocialChat
import com.truckerload.presentation.di.LocalSocialRepository
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassClickableCard
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens
import com.truckerload.presentation.utils.MoneyFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    onOpenChat: (String) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenVoiceRooms: () -> Unit,
    onOpenStatus: () -> Unit = {},
    onOpenGroups: () -> Unit = {},
    onOpenGroupDetail: (String) -> Unit = {},
    onOpenPeerProfile: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    chatsViewModel: ChatsViewModel = viewModel(
        factory = ChatsViewModel.Factory(LocalSocialRepository.current),
    ),
    communityViewModel: CommunityViewModel = viewModel(
        factory = CommunityViewModel.Factory(LocalSocialRepository.current),
    ),
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.chats),
        stringResource(R.string.leaderboard),
        stringResource(R.string.challenges),
    )
    val chatsState by chatsViewModel.uiState.collectAsState()
    val communityState by communityViewModel.uiState.collectAsState()
    val leaderboard by communityViewModel.leaderboard.collectAsState()
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val openDrawer = LocalOpenDrawer.current
    val challenge = communityState.challenge

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(chatsState.errorMessage) {
        chatsState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = context.getString(R.string.social_chat_create_error, message),
            )
            chatsViewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = BentoGlassTheme.ScreenBackground,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.community)) },
                navigationIcon = {
                    IconButton(onClick = openDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.common_menu), tint = tc.TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenStatus) {
                        Icon(Icons.Default.Star, contentDescription = stringResource(R.string.social_statuses))
                    }
                    IconButton(onClick = onOpenGroups) {
                        Icon(Icons.Default.Groups, contentDescription = stringResource(R.string.social_groups))
                    }
                    IconButton(onClick = onOpenProfile) {
                        Icon(Icons.Default.Person, contentDescription = stringResource(R.string.profile))
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
            TabRow(selectedTabIndex = tabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = tabIndex == index,
                        onClick = { tabIndex = index },
                        text = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
            when (tabIndex) {
                0 -> ChatsTabContent(
                    groupChats = chatsState.groupChats,
                    privateChats = chatsState.privateChats,
                    peers = chatsState.peers,
                    searchQuery = chatsState.searchQuery,
                    onSearchChange = chatsViewModel::setSearchQuery,
                    onCreateGroup = { name ->
                        chatsViewModel.createGroupChat(name) { chatId -> onOpenGroupDetail(chatId) }
                    },
                    onCreatePrivateWithPeer = { peerId ->
                        chatsViewModel.createPrivateChatWithPeer(peerId) { chatId -> onOpenChat(chatId) }
                    },
                    onChatClick = onOpenChat,
                    onOpenVoiceRooms = onOpenVoiceRooms,
                )
                1 -> LeaderboardTabContent(
                    entries = leaderboard,
                    onCategoryChange = communityViewModel::setLeaderboardCategory,
                    onPeerClick = onOpenPeerProfile,
                )
                2 -> challenge?.let { activeChallenge ->
                    ChallengesTabContent(
                        challenge = activeChallenge,
                        joined = communityState.challengeJoined,
                        isJoining = communityState.isJoiningChallenge,
                        onJoin = communityViewModel::joinChallenge,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatsTabContent(
    groupChats: List<SocialChat>,
    privateChats: List<SocialChat>,
    peers: List<com.truckerload.domain.social.SocialPeerProfile>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onCreateGroup: (String) -> Unit,
    onCreatePrivateWithPeer: (String) -> Unit,
    onChatClick: (String) -> Unit,
    onOpenVoiceRooms: () -> Unit,
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
            },
            confirmButton = {},
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
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = tc.TextPrimary,
                    unfocusedTextColor = tc.TextPrimary,
                    cursorColor = tc.AccentPrimary,
                    focusedBorderColor = tc.AccentPrimary,
                    unfocusedBorderColor = tc.Divider,
                ),
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
                    Text("🎙️", style = MaterialTheme.typography.headlineMedium)
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
private fun ChatListItem(chat: SocialChat, onClick: () -> Unit) {
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
            Text(chat.avatarEmoji, style = MaterialTheme.typography.headlineMedium)
            Column(modifier = Modifier.weight(1f)) {
                Text(chat.title, style = AppTypography.CardTitle, color = tc.TextPrimary)
                Text(
                    text = "⭐ ${"%.1f".format(chat.rating)} · 📍 ${chat.participantCount} · ${chat.lastMessage}",
                    style = AppTypography.Subtitle,
                    color = tc.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
                    Text("🔴 ${chat.unreadCount}", style = AppTypography.Subtitle)
                } else if (chat.onlineCount > 0) {
                    Text("🟢 ${chat.onlineCount}", style = AppTypography.Subtitle)
                }
            }
        }
    }
}

@Composable
private fun LeaderboardTabContent(
    entries: List<com.truckerload.domain.social.LeaderboardEntry>,
    onCategoryChange: (LeaderboardCategory) -> Unit,
    onPeerClick: (String) -> Unit,
) {
    val tc = LocalTruckColors.current
    var categoryIndex by remember { mutableIntStateOf(0) }
    val categories = LeaderboardCategory.entries
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            TabRow(selectedTabIndex = categoryIndex) {
                categories.forEachIndexed { index, category ->
                    Tab(
                        selected = categoryIndex == index,
                        onClick = {
                            categoryIndex = index
                            onCategoryChange(category)
                        },
                        text = { Text(category.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
        }
        items(entries, key = { "${it.rank}_${it.displayName}" }) { entry ->
            val peerId = entry.userId
            val clickable = !entry.isMe && !peerId.isNullOrBlank()
            BentoGlassClickableCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { peerId?.takeIf { clickable }?.let(onPeerClick) },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = when (entry.rank) {
                            1 -> "🥇"
                            2 -> "🥈"
                            3 -> "🥉"
                            else -> "${entry.rank}."
                        } + " ${entry.displayName}",
                        style = AppTypography.CardTitle,
                        color = if (entry.isMe) tc.AccentPrimary else tc.TextPrimary,
                    )
                    Text(
                        text = "⭐ ${"%.1f".format(entry.rating)}  ${MoneyFormat.formatNumber(entry.score)} ${entry.trend}",
                        style = AppTypography.Subtitle,
                        color = tc.TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChallengesTabContent(
    challenge: com.truckerload.domain.social.Challenge,
    joined: Boolean,
    isJoining: Boolean,
    onJoin: () -> Unit,
) {
    val tc = LocalTruckColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.weekly_challenge), style = AppTypography.CardTitle, color = tc.TextPrimary)
                Text("🏆 ${challenge.title}", style = AppTypography.Subtitle, modifier = Modifier.padding(top = 8.dp))
                Text(challenge.description, style = AppTypography.Subtitle, color = tc.TextSecondary)
                Text(
                    text = stringResource(R.string.my_position) + ": #${challenge.myPosition} (${MoneyFormat.formatNumber(challenge.myScore)} mi)",
                    style = AppTypography.Subtitle,
                    color = tc.AccentPrimary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        challenge.leaderboard.forEach { entry ->
            BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${entry.rank}. ${entry.displayName} — ${MoneyFormat.formatNumber(entry.score)} mi",
                    modifier = Modifier.padding(16.dp),
                    style = AppTypography.Subtitle,
                )
            }
        }
        Text(
            text = if (joined) {
                stringResource(R.string.social_challenge_joined)
            } else {
                stringResource(R.string.social_join_challenge)
            },
            style = AppTypography.Subtitle,
            color = tc.TextSecondary,
        )
        if (!joined) {
            Button(
                onClick = onJoin,
                enabled = !isJoining,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.social_join_challenge))
            }
        }
    }
}

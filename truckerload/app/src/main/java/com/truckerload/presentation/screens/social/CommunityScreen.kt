package com.truckerload.presentation.screens.social

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.data.preferences.CommunityHintArea
import com.truckerload.presentation.components.SoftActionChip
import com.truckerload.presentation.components.SoftAppPageScaffold
import com.truckerload.presentation.di.LocalSettingsDataStore
import kotlinx.coroutines.launch

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
    onOpenFriends: () -> Unit = {},
    modifier: Modifier = Modifier,
    chatsViewModel: ChatsViewModel = hiltViewModel(),
    communityViewModel: CommunityViewModel = hiltViewModel(),
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.chats),
        stringResource(R.string.leaderboard),
        stringResource(R.string.challenges),
    )
    val chatsState by chatsViewModel.uiState.collectAsStateWithLifecycle()
    val communityState by communityViewModel.uiState.collectAsStateWithLifecycle()
    val leaderboard by communityViewModel.leaderboard.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val challenge = communityState.challenge
    val snackbarHostState = remember { SnackbarHostState() }
    val settingsDataStore = LocalSettingsDataStore.current
    val hintScope = rememberCoroutineScope()

    LaunchedEffect(chatsState.errorMessage) {
        chatsState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = context.getString(R.string.social_chat_create_error, message),
            )
            chatsViewModel.clearError()
        }
    }

    LaunchedEffect(communityState.errorMessage) {
        communityState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            communityViewModel.clearError()
        }
    }

    SoftAppPageScaffold(
        title = stringResource(R.string.community),
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        actions = {
            SoftActionChip(
                icon = Icons.Default.Star,
                contentDescription = stringResource(R.string.social_statuses),
                onClick = onOpenStatus,
            )
            SoftActionChip(
                icon = Icons.Default.Groups,
                contentDescription = stringResource(R.string.social_groups),
                onClick = onOpenGroups,
            )
            SoftActionChip(
                icon = Icons.Default.Person,
                contentDescription = stringResource(R.string.profile),
                onClick = onOpenProfile,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            PrimaryTabRow(selectedTabIndex = tabIndex) {
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
                    onCreateGroup = { name, description ->
                        chatsViewModel.createGroupChat(name, description) { chatId -> onOpenGroupDetail(chatId) }
                    },
                    onCreatePrivateWithPeer = { peerId ->
                        chatsViewModel.createPrivateChatWithPeer(peerId) { chatId -> onOpenChat(chatId) }
                    },
                    onChatClick = onOpenChat,
                    onOpenVoiceRooms = onOpenVoiceRooms,
                    onOpenFriends = onOpenFriends,
                )
                1 -> LeaderboardTabContent(
                    entries = leaderboard,
                    onCategoryChange = communityViewModel::setLeaderboardCategory,
                    onPeerClick = onOpenPeerProfile,
                    onOpenFriends = onOpenFriends,
                )

                2 -> if (challenge == null) {
                    DisposableEffect(Unit) {
                        onDispose {
                            hintScope.launch {
                                settingsDataStore.markCommunityHintUsed(CommunityHintArea.CHALLENGES)
                            }
                        }
                    }
                    Column(modifier = Modifier.padding(16.dp)) {
                        CommunityFirstUseHint(
                            area = CommunityHintArea.CHALLENGES,
                            message = stringResource(R.string.community_empty_challenge),
                            hasContent = false,
                        )
                    }
                } else {
                    ChallengesTabContent(
                        challenge = challenge,
                        joined = communityState.challengeJoined,
                        isJoining = communityState.isJoiningChallenge,
                        onJoin = communityViewModel::joinChallenge,
                        onOpenFriends = onOpenFriends,
                    )
                }
            }
        }
    }
}

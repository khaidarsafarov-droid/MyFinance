package com.truckerload.presentation.screens.social

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.presentation.components.LocalOpenDrawer
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors

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

    LaunchedEffect(communityState.errorMessage) {
        communityState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            communityViewModel.clearError()
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

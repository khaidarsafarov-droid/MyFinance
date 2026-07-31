package com.truckerload.presentation.navigation

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.truckerload.R
import com.truckerload.presentation.components.navigateToMainRoute
import com.truckerload.presentation.di.LocalProfileRepository
import com.truckerload.presentation.di.LocalVoiceRepository
import com.truckerload.presentation.screens.social.CommunityScreen
import com.truckerload.presentation.screens.social.GroupDetailScreen
import com.truckerload.presentation.screens.social.GroupsScreen
import com.truckerload.presentation.screens.social.PeerProfileScreen
import com.truckerload.presentation.screens.social.ProfileEditScreen
import com.truckerload.presentation.screens.social.ProfileScreen
import com.truckerload.presentation.screens.social.SocialChatScreen
import com.truckerload.presentation.screens.social.StatusScreen
import com.truckerload.presentation.screens.voice.CallScreen
import com.truckerload.presentation.screens.voice.VoiceRoomScreen
import com.truckerload.presentation.screens.voice.VoiceRoomsScreen
import com.truckerload.presentation.theme.tabEnterTransition
import com.truckerload.presentation.theme.tabExitTransition
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

fun NavGraphBuilder.socialNavGraph(navController: NavHostController) {
    composable(
        route = Routes.COMMUNITY,
        enterTransition = { tabEnterTransition() },
        exitTransition = { tabExitTransition() },
        popEnterTransition = { tabEnterTransition() },
        popExitTransition = { tabExitTransition() },
    ) {
        CommunityScreen(
            onOpenChat = { chatId -> navController.navigate(Routes.socialChat(chatId)) },
            onOpenProfile = { navController.navigate(Routes.PROFILE) },
            onOpenVoiceRooms = { navController.navigate(Routes.VOICE_ROOMS) },
            onOpenStatus = { navController.navigate(Routes.STATUS) },
            onOpenGroups = { navController.navigate(Routes.GROUPS) },
            onOpenGroupDetail = { chatId -> navController.navigate(Routes.groupDetail(chatId)) },
            onOpenPeerProfile = { peerId -> navController.navigate(Routes.peerProfile(peerId)) },
        )
    }
    composable(Routes.STATUS) {
        StatusScreen(onBack = { navController.popBackStack() })
    }
    composable(Routes.GROUPS) {
        GroupsScreen(
            onBack = { navController.popBackStack() },
            onOpenGroup = { chatId -> navController.navigate(Routes.groupDetail(chatId)) },
            onOpenChat = { chatId -> navController.navigate(Routes.socialChat(chatId)) },
        )
    }
    composable(
        route = Routes.GROUP_DETAIL,
        arguments = listOf(navArgument("chatId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val chatId = backStackEntry.arguments?.getString("chatId").orEmpty()
        GroupDetailScreen(
            chatId = chatId,
            onBack = { navController.popBackStack() },
            onOpenChat = { navController.navigate(Routes.socialChat(it)) },
        )
    }
    composable(
        route = Routes.PROFILE_PEER,
        arguments = listOf(navArgument("peerId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val peerId = backStackEntry.arguments?.getString("peerId").orEmpty()
        val voiceRepository = LocalVoiceRepository.current
        val profileRepository = LocalProfileRepository.current
        val scope = rememberCoroutineScope()
        val callerFallbackName = stringResource(R.string.social_you)
        PeerProfileScreen(
            peerId = peerId,
            onBack = { navController.popBackStack() },
            onOpenChat = { chatId -> navController.navigate(Routes.socialChat(chatId)) },
            onStartCall = { calleeId, calleeName ->
                scope.launch {
                    val callerName = profileRepository.watchMyProfile().first().displayName.ifBlank { callerFallbackName }
                    voiceRepository.startCall(calleeId, calleeName, callerName)
                        .getOrNull()
                        ?.let { call -> navController.navigate(Routes.call(call.callId)) }
                }
            },
        )
    }
    composable(
        route = Routes.PROFILE,
        enterTransition = { tabEnterTransition() },
        exitTransition = { tabExitTransition() },
        popEnterTransition = { tabEnterTransition() },
        popExitTransition = { tabExitTransition() },
    ) {
        ProfileScreen(
            onBack = { navigateToMainRoute(Routes.HOME, navController) },
            onEdit = { navController.navigate(Routes.PROFILE_EDIT) },
            showBack = false,
        )
    }
    composable(Routes.PROFILE_EDIT) {
        ProfileEditScreen(
            onBack = { navController.popBackStack() },
            onSaved = { navController.popBackStack() },
        )
    }
    composable(
        route = Routes.SOCIAL_CHAT,
        arguments = listOf(navArgument("chatId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val chatId = backStackEntry.arguments?.getString("chatId").orEmpty()
        SocialChatScreen(
            chatId = chatId,
            onBack = { navController.popBackStack() },
        )
    }
    composable(Routes.VOICE_ROOMS) {
        VoiceRoomsScreen(
            onBack = { navController.popBackStack() },
            onOpenRoom = { roomId -> navController.navigate(Routes.voiceRoom(roomId)) },
        )
    }
    composable(
        route = Routes.VOICE_ROOM,
        arguments = listOf(navArgument("roomId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val roomId = backStackEntry.arguments?.getString("roomId").orEmpty()
        VoiceRoomScreen(
            roomId = roomId,
            onBack = { navController.popBackStack() },
        )
    }
    composable(
        route = Routes.CALL,
        arguments = listOf(navArgument("callId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val callId = backStackEntry.arguments?.getString("callId").orEmpty()
        CallScreen(
            callId = callId,
            onBack = { navController.popBackStack() },
        )
    }
}

package com.truckerload.presentation.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckerload.presentation.di.LocalAuthStore
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.di.LocalSocialRepository
import com.truckerload.presentation.di.LocalVoiceRepository
import com.truckerload.presentation.screens.home.HomeViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import android.net.Uri
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.truckerload.R
import com.truckerload.presentation.theme.tabEnterTransition
import com.truckerload.presentation.theme.tabExitTransition
import com.truckerload.presentation.components.AdaptiveScaffold
import com.truckerload.presentation.components.DrawerDestination
import com.truckerload.presentation.components.navigateToMainRoute
import com.truckerload.presentation.utils.AdaptiveScreenContainer
import com.truckerload.presentation.utils.isTablet
import com.truckerload.presentation.screens.add.AddDieselScreen
import com.truckerload.presentation.screens.add.AddLoadScreen
import com.truckerload.presentation.screens.add.AddPaycheckScreen
import com.truckerload.presentation.screens.about.AboutAppScreen
import com.truckerload.presentation.screens.detail.LoadDetailScreen
import com.truckerload.presentation.screens.edit.EditLoadScreen
import com.truckerload.presentation.screens.home.HomeScreen
import com.truckerload.presentation.screens.goal.WeeklyGoalScreen
import com.truckerload.presentation.screens.tax.TaxTrackerScreen
import com.truckerload.presentation.screens.maintenance.MaintenanceScreen
import com.truckerload.presentation.screens.advisor.FinancialAdvisorScreen
import com.truckerload.presentation.screens.map.FriendsLiveMapScreen
import com.truckerload.presentation.screens.map.MapScreen
import com.truckerload.presentation.screens.auth.ProfileSetupScreen
import com.truckerload.presentation.di.LocalUserProfileStore
import com.truckerload.presentation.screens.settings.SettingsScreen
import com.truckerload.presentation.screens.analytics.AnalyticsScreen
import com.truckerload.presentation.screens.stats.StatsScreen
import com.truckerload.presentation.screens.camera.CameraFlowScreen
import com.truckerload.presentation.screens.scanner.ScannerFlowScreen
import com.truckerload.presentation.screens.scanner.ScanGalleryScreen
import com.truckerload.presentation.screens.attach.AttachLoadPickScreen
import com.truckerload.presentation.screens.attach.AttachPickMode
import com.truckerload.presentation.screens.gallery.PhotoDetailScreen
import com.truckerload.presentation.screens.gallery.PhotoGalleryScreen
import com.truckerload.presentation.screens.social.CommunityScreen
import com.truckerload.presentation.screens.social.ProfileEditScreen
import com.truckerload.presentation.screens.social.ProfileScreen
import com.truckerload.presentation.screens.social.GroupDetailScreen
import com.truckerload.presentation.screens.social.GroupsScreen
import com.truckerload.presentation.screens.social.PeerProfileScreen
import com.truckerload.presentation.screens.social.SocialChatScreen
import com.truckerload.presentation.screens.social.StatusScreen
import com.truckerload.presentation.screens.voice.CallScreen
import com.truckerload.presentation.screens.voice.IncomingCallOverlay
import com.truckerload.presentation.screens.voice.VoiceRoomScreen
import com.truckerload.presentation.screens.voice.VoiceRoomsScreen
import com.truckerload.widget.WidgetDeepLink

/**
 * Central navigation contract for Compose destinations and deep-link route builders.
 * Constants define route patterns; helper functions URL-encode path segments before navigation.
 */
object Routes {
    const val HOME = "home"
    const val STATS = "stats"
    const val ANALYTICS = "analytics"
    const val COMMUNITY = "community"
    const val PROFILE = "profile"
    const val PROFILE_EDIT = "profile_edit"
    const val PROFILE_SETUP = "profile_setup"
    const val PROFILE_PEER = "profile_peer/{peerId}"
    const val SOCIAL_CHAT = "social_chat/{chatId}"
    const val ADVANCED_STATS = "advanced_stats"
    const val MAP = "map"
    const val LOAD_DETAIL = "load_detail/{loadId}"
    const val ADD_LOAD = "add_load"
    const val EDIT_LOAD = "edit_load/{loadId}?focusFinish={focusFinish}"
    const val ADD_PAYCHECK = "add_paycheck"
    const val ADD_DIESEL = "add_diesel"
    const val TAX_TRACKER = "tax_tracker"
    const val MAINTENANCE = "maintenance"
    const val FINANCIAL_ADVISOR = "financial_advisor"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val FRIENDS_LIVE = "friends_live"
    const val CAMERA = "camera"
    const val CAMERA_FOR_LOAD = "camera_load/{loadId}/{tripId}/{loadDate}"
    const val SCANNER = "scanner"
    const val SCANNER_FOR_LOAD = "scanner_load/{loadId}/{tripId}/{loadDate}"
    /** Widget camera/scan: pick one of the last loads, then open attached capture. */
    const val ATTACH_PICK = "attach_pick/{mode}"
    const val SCAN_GALLERY = "scan_gallery"
    const val PHOTO_GALLERY = "photo_gallery"
    const val PHOTO_DETAIL = "photo_detail/{photoId}"

    fun loadDetail(loadId: String) = "load_detail/${encodePathSegment(loadId)}"
    fun editLoad(loadId: String, focusFinish: Boolean = false) =
        "edit_load/${encodePathSegment(loadId)}?focusFinish=$focusFinish"
    fun photoDetail(photoId: String) = "photo_detail/${encodePathSegment(photoId)}"
    fun socialChat(chatId: String) = "social_chat/${encodePathSegment(chatId)}"
    fun cameraForLoad(loadId: String, tripId: String, loadDate: String): String {
        return "camera_load/${encodePathSegment(loadId)}/${encodePathSegment(tripId)}/${encodePathSegment(loadDate)}"
    }

    fun scannerForLoad(loadId: String, tripId: String, loadDate: String): String {
        return "scanner_load/${encodePathSegment(loadId)}/${encodePathSegment(tripId)}/${encodePathSegment(loadDate)}"
    }

    fun attachPick(mode: String): String = "attach_pick/${encodePathSegment(mode)}"

    private fun encodePathSegment(value: String): String =
        Uri.encode(value.ifBlank { "_" }) ?: "_"

    const val VOICE_ROOMS = "voice_rooms"
    const val VOICE_ROOM = "voice_room/{roomId}"
    const val CALL = "call/{callId}"
    const val STATUS = "status"
    const val GROUPS = "groups"
    const val GROUP_DETAIL = "group_detail/{chatId}"

    fun groupDetail(chatId: String) = "group_detail/${encodePathSegment(chatId)}"
    fun peerProfile(peerId: String) = "profile_peer/${encodePathSegment(peerId)}"

    fun voiceRoom(roomId: String) = "voice_room/${encodePathSegment(roomId)}"
    fun call(callId: String) = "call/${encodePathSegment(callId)}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph(
    navController: androidx.navigation.NavHostController = rememberNavController(),
    deepLinkRoute: String? = null,
    onDeepLinkHandled: () -> Unit = {}
) {
    val authStore = LocalAuthStore.current
    val isLoggedIn by authStore.isLoggedIn.collectAsStateWithLifecycle()
    var showMainContent by remember { mutableStateOf(true) }
    var hasShownAuth by remember { mutableStateOf(false) }
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            hasShownAuth = true
            showMainContent = false
        } else {
            if (hasShownAuth) {
                showMainContent = false
                delay(200)
            }
            showMainContent = true
        }
    }
    val context = LocalContext.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val tablet = isTablet()

    if (!isLoggedIn) {
        // Auth UI is hosted by MainActivity (account switch recreates user-scoped deps).
        return
    }
    if (!showMainContent) {
        return
    }

    val socialRepository = LocalSocialRepository.current
    val userProfileStore = LocalUserProfileStore.current
    val setupComplete by userProfileStore.setupComplete.collectAsStateWithLifecycle()
    val authEmail by authStore.email.collectAsStateWithLifecycle()
    var needsSetup by remember { mutableStateOf<Boolean?>(null) }
    var needsEmailVerify by remember { mutableStateOf<Boolean?>(null) }
    val emailVerifyStore = remember(context) {
        com.truckerload.data.preferences.EmailVerificationStore(context.applicationContext)
    }
    LaunchedEffect(isLoggedIn, setupComplete, authEmail) {
        if (!isLoggedIn) {
            needsSetup = null
            needsEmailVerify = null
            return@LaunchedEffect
        }
        socialRepository.ensureInitialized()
        val setup = socialRepository.needsProfileSetup()
        needsSetup = setup
        if (setup) {
            needsEmailVerify = false
            return@LaunchedEffect
        }
        val provider = authStore.authProvider()
        val supabaseConfigured = com.truckerload.data.remote.SupabaseAuthService(context.applicationContext)
            .isConfigured()
        needsEmailVerify = supabaseConfigured &&
            provider == com.truckerload.data.preferences.AuthProvider.EMAIL &&
            authEmail.isNotBlank() &&
            emailVerifyStore.isPending(authEmail)
    }
    if (needsSetup == true) {
        ProfileSetupScreen(
            onCompleted = {
                needsSetup = false
                // After wizard, start soft email verification for email accounts.
                if (authStore.authProvider() == com.truckerload.data.preferences.AuthProvider.EMAIL &&
                    authEmail.isNotBlank() &&
                    !emailVerifyStore.isVerified(authEmail)
                ) {
                    emailVerifyStore.beginVerification(authEmail)
                    needsEmailVerify = true
                } else {
                    needsEmailVerify = false
                }
            },
        )
        return
    }
    if (needsSetup == null || needsEmailVerify == null) {
        return
    }
    if (needsEmailVerify == true) {
        com.truckerload.presentation.screens.auth.EmailVerificationScreen(
            email = authEmail,
            onVerified = { needsEmailVerify = false },
            onSkip = { needsEmailVerify = false },
        )
        return
    }

    // Handle widget deep links only after NavHost is about to be composed (setup gates passed).
    LaunchedEffect(deepLinkRoute, isLoggedIn, showMainContent, needsSetup, needsEmailVerify) {
        if (!isLoggedIn || !showMainContent) return@LaunchedEffect
        if (needsSetup != false || needsEmailVerify != false) return@LaunchedEffect
        val route = deepLinkRoute ?: return@LaunchedEffect
        val destination = WidgetDeepLink.resolveNavRoute(route) ?: return@LaunchedEffect
        when (destination) {
            Routes.HOME -> {
                navController.navigate(Routes.HOME) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
                onDeepLinkHandled()
            }
            Routes.ADD_LOAD -> {
                navController.navigate(Routes.ADD_LOAD)
                onDeepLinkHandled()
            }
            Routes.ANALYTICS -> {
                navController.navigate(Routes.ANALYTICS) {
                    launchSingleTop = true
                }
                onDeepLinkHandled()
            }
            Routes.STATS -> {
                navController.navigate(Routes.STATS) {
                    launchSingleTop = true
                }
                onDeepLinkHandled()
            }
            else -> {
                // Widget camera/scan resolve to attach_pick/{camera|scanner}.
                if (destination.startsWith("attach_pick/")) {
                    navController.navigate(destination) { launchSingleTop = true }
                    onDeepLinkHandled()
                }
            }
        }
    }

    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route
    val phoneMainRoutes = listOf(Routes.HOME, Routes.STATS, Routes.COMMUNITY, Routes.PROFILE)
    val showMainNavigation = if (tablet) {
        currentRoute != Routes.ADD_PAYCHECK && currentRoute != Routes.ADD_DIESEL &&
            currentRoute != Routes.CAMERA && currentRoute != Routes.SCANNER &&
            currentRoute != Routes.CAMERA_FOR_LOAD && currentRoute != Routes.SCANNER_FOR_LOAD &&
            currentRoute != Routes.SCAN_GALLERY && currentRoute != Routes.PHOTO_GALLERY &&
            !currentRoute.orEmpty().startsWith("attach_pick") &&
            !currentRoute.orEmpty().startsWith("photo_detail") &&
            !currentRoute.orEmpty().startsWith("profile_peer") &&
            !currentRoute.orEmpty().startsWith("social_chat")
    } else {
        currentRoute in phoneMainRoutes
    }

    AdaptiveScaffold(
        showMainNavigation = showMainNavigation,
        currentRoute = currentRoute,
        onNavigate = { route -> navigateToMainRoute(route, navController) },
        onDrawerNavigate = { destination ->
            when (destination) {
                DrawerDestination.PROFILE -> navigateToMainRoute(Routes.PROFILE, navController)
                DrawerDestination.SETTINGS -> navController.navigate(Routes.SETTINGS) { launchSingleTop = true }
                DrawerDestination.REPORTS -> navController.navigate(Routes.ANALYTICS) { launchSingleTop = true }
                DrawerDestination.MAP -> navController.navigate(Routes.MAP) { launchSingleTop = true }
                DrawerDestination.FRIENDS_LIVE -> navController.navigate(Routes.FRIENDS_LIVE) { launchSingleTop = true }
                DrawerDestination.DOCUMENTS -> navController.navigate(Routes.SCAN_GALLERY) { launchSingleTop = true }
                DrawerDestination.MAINTENANCE -> navController.navigate(Routes.MAINTENANCE) { launchSingleTop = true }
                DrawerDestination.TAX_TRACKER -> navController.navigate(Routes.TAX_TRACKER) { launchSingleTop = true }
                DrawerDestination.ADD_PAYCHECK -> navController.navigate(Routes.ADD_PAYCHECK) { launchSingleTop = true }
                DrawerDestination.ADD_DIESEL -> navController.navigate(Routes.ADD_DIESEL) { launchSingleTop = true }
                DrawerDestination.SCANNER -> navController.navigate(Routes.SCANNER) { launchSingleTop = true }
                DrawerDestination.CAMERA -> navController.navigate(Routes.CAMERA) { launchSingleTop = true }
                DrawerDestination.ABOUT -> navController.navigate(Routes.ABOUT) { launchSingleTop = true }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
        AdaptiveScreenContainer(modifier = Modifier.padding(padding)) {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(
                route = Routes.HOME,
                enterTransition = { tabEnterTransition() },
                exitTransition = { tabExitTransition() },
                popEnterTransition = { tabEnterTransition() },
                popExitTransition = { tabExitTransition() },
            ) {
                HomeScreen(
                    onLoadClick = { navController.navigate(Routes.loadDetail(it)) },
                    onAddLoad = { navController.navigate(Routes.ADD_LOAD) },
                    onStats = { navController.navigate(Routes.ANALYTICS) },
                    onWeeklyGoal = { navController.navigate(Routes.STATS) },
                    onSettings = { navController.navigate(Routes.SETTINGS) },
                    onLoadCamera = { loadId, tripId, loadDate ->
                        navController.navigate(Routes.cameraForLoad(loadId, tripId, loadDate))
                    },
                    onLoadScan = { loadId, tripId, loadDate ->
                        navController.navigate(Routes.scannerForLoad(loadId, tripId, loadDate))
                    },
                )
            }
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
                val socialRepository = LocalSocialRepository.current
                val scope = rememberCoroutineScope()
                val callerFallbackName = stringResource(R.string.social_you)
                PeerProfileScreen(
                    peerId = peerId,
                    onBack = { navController.popBackStack() },
                    onOpenChat = { chatId -> navController.navigate(Routes.socialChat(chatId)) },
                    onStartCall = { calleeId, calleeName ->
                        scope.launch {
                            val callerName = socialRepository.watchMyProfile().first().displayName.ifBlank { callerFallbackName }
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
            composable(
                route = Routes.ANALYTICS,
                enterTransition = { tabEnterTransition() },
                exitTransition = { tabExitTransition() },
                popEnterTransition = { tabEnterTransition() },
                popExitTransition = { tabExitTransition() },
            ) {
                AnalyticsScreen(
                    onBack = { navController.popBackStack() },
                    onLoadClick = { loadId -> navController.navigate(Routes.loadDetail(loadId)) },
                    onAdvancedStats = { navController.navigate(Routes.ADVANCED_STATS) },
                    onOpenMap = { navController.navigate(Routes.MAP) },
                )
            }
            composable(Routes.ADVANCED_STATS) {
                StatsScreen(
                    onBack = { navController.popBackStack() },
                    showBack = !tablet,
                    onOpenMap = { navController.navigate(Routes.MAP) },
                    onFinancialAdvisor = { navController.navigate(Routes.FINANCIAL_ADVISOR) },
                    onDieselDetail = { navController.navigate(Routes.ADD_DIESEL) },
                    onNetProfitDetail = { navController.navigate(Routes.FINANCIAL_ADVISOR) },
                    onPaycheckDetail = { navController.navigate(Routes.ADD_PAYCHECK) },
                )
            }
            composable(
                route = Routes.STATS,
                enterTransition = { tabEnterTransition() },
                exitTransition = { tabExitTransition() },
                popEnterTransition = { tabEnterTransition() },
                popExitTransition = { tabExitTransition() },
            ) {
                WeeklyGoalScreen()
            }
            composable(Routes.MAP) {
                MapScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.FRIENDS_LIVE) {
                FriendsLiveMapScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.TAX_TRACKER) {
                TaxTrackerScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.MAINTENANCE) {
                MaintenanceScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.FINANCIAL_ADVISOR) {
                FinancialAdvisorScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.SETTINGS,
                enterTransition = { tabEnterTransition() },
                exitTransition = { tabExitTransition() },
                popEnterTransition = { tabEnterTransition() },
                popExitTransition = { tabExitTransition() },
            ) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    showBack = !tablet
                )
            }
            composable(Routes.ABOUT) {
                AboutAppScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.LOAD_DETAIL,
                arguments = listOf(navArgument("loadId") { type = NavType.StringType })
            ) { backStackEntry ->
                val loadId = Uri.decode(backStackEntry.arguments?.getString("loadId").orEmpty())
                LoadDetailScreen(
                    loadId = loadId,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Routes.editLoad(loadId)) },
                    onEditFinish = { navController.navigate(Routes.editLoad(loadId, focusFinish = true)) },
                    onDelete = { navController.popBackStack() },
                    onPhotoClick = { navController.navigate(Routes.photoDetail(it)) },
                )
            }
            composable(Routes.ADD_LOAD) { addLoadEntry ->
                val loadRepository = LocalLoadRepository.current
                val homeEntry = remember(addLoadEntry) {
                    runCatching { navController.getBackStackEntry(Routes.HOME) }.getOrNull()
                }
                val homeViewModel: HomeViewModel? = homeEntry?.let {
                    viewModel(it, factory = HomeViewModel.Factory(loadRepository, isBotConfigured = false, context))
                }
                AddLoadScreen(
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                    onOptimisticInsert = homeViewModel?.let { { load -> it.applyOptimisticUpdate(load) } },
                    onRevertOptimistic = homeViewModel?.let { { id -> it.revertOptimisticUpdate(id) } }
                )
            }
            composable(
                route = Routes.EDIT_LOAD,
                arguments = listOf(
                    navArgument("loadId") { type = NavType.StringType },
                    navArgument("focusFinish") {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                ),
            ) { editBackStackEntry ->
                val loadId = Uri.decode(editBackStackEntry.arguments?.getString("loadId").orEmpty())
                val focusFinish = editBackStackEntry.arguments?.getBoolean("focusFinish") == true
                val loadRepository = LocalLoadRepository.current
                val homeEntry = remember(editBackStackEntry) {
                    runCatching { navController.getBackStackEntry(Routes.HOME) }.getOrNull()
                }
                val homeViewModel: HomeViewModel? = homeEntry?.let {
                    viewModel(it, factory = HomeViewModel.Factory(loadRepository, isBotConfigured = false, context))
                }
                EditLoadScreen(
                    loadId = loadId,
                    focusFinish = focusFinish,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                    onOptimisticUpdate = homeViewModel?.let { { load -> it.applyOptimisticUpdate(load) } },
                    onRevertOptimistic = homeViewModel?.let { { id -> it.revertOptimisticUpdate(id) } }
                )
            }
            composable(Routes.ADD_PAYCHECK) {
                AddPaycheckScreen(onSaved = { navController.popBackStack() }, onBack = { navController.popBackStack() })
            }
            composable(Routes.ADD_DIESEL) {
                AddDieselScreen(onSaved = { navController.popBackStack() }, onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.ATTACH_PICK,
                arguments = listOf(
                    navArgument("mode") { type = NavType.StringType },
                ),
            ) { entry ->
                val modeArg = Uri.decode(entry.arguments?.getString("mode").orEmpty())
                val mode = when (modeArg) {
                    "scanner" -> AttachPickMode.SCANNER
                    else -> AttachPickMode.CAMERA
                }
                AttachLoadPickScreen(
                    mode = mode,
                    onCancel = { navController.popBackStack() },
                    onAddLoad = {
                        navController.popBackStack()
                        navController.navigate(Routes.ADD_LOAD) { launchSingleTop = true }
                    },
                    onLoadSelected = { load ->
                        navController.popBackStack()
                        val dest = when (mode) {
                            AttachPickMode.CAMERA -> Routes.cameraForLoad(load.id, load.tripId, load.date)
                            AttachPickMode.SCANNER -> Routes.scannerForLoad(load.id, load.tripId, load.date)
                        }
                        navController.navigate(dest) { launchSingleTop = true }
                    },
                )
            }
            composable(Routes.CAMERA) {
                CameraFlowScreen(
                    onFinished = { navController.popBackStack() },
                    onOpenGallery = { navController.navigate(Routes.PHOTO_GALLERY) },
                )
            }
            composable(
                route = Routes.CAMERA_FOR_LOAD,
                arguments = listOf(
                    navArgument("loadId") { type = NavType.StringType },
                    navArgument("tripId") { type = NavType.StringType },
                    navArgument("loadDate") { type = NavType.StringType },
                ),
            ) { entry ->
                val loadId = Uri.decode(entry.arguments?.getString("loadId").orEmpty())
                val tripId = Uri.decode(entry.arguments?.getString("tripId").orEmpty())
                val loadDate = Uri.decode(entry.arguments?.getString("loadDate").orEmpty())
                    .takeIf { it != "_" }.orEmpty()
                CameraFlowScreen(
                    onFinished = {
                        if (loadId.isNotBlank() && loadId != "_") {
                            navController.popBackStack()
                            navController.navigate(Routes.loadDetail(loadId)) {
                                launchSingleTop = true
                            }
                        } else {
                            navController.popBackStack()
                        }
                    },
                    onOpenGallery = { navController.navigate(Routes.PHOTO_GALLERY) },
                    attachLoadId = loadId.takeIf { it.isNotBlank() && it != "_" },
                    attachTripId = tripId.takeIf { it.isNotBlank() && it != "_" },
                    attachLoadDate = loadDate,
                )
            }
            composable(Routes.SCANNER) {
                ScannerFlowScreen(
                    onFinished = { navController.popBackStack() },
                    onOpenGallery = { navController.navigate(Routes.SCAN_GALLERY) },
                    onCameraFallback = {
                        navController.popBackStack()
                        navController.navigate(Routes.CAMERA) { launchSingleTop = true }
                    },
                )
            }
            composable(
                route = Routes.SCANNER_FOR_LOAD,
                arguments = listOf(
                    navArgument("loadId") { type = NavType.StringType },
                    navArgument("tripId") { type = NavType.StringType },
                    navArgument("loadDate") { type = NavType.StringType },
                ),
            ) { entry ->
                val loadId = Uri.decode(entry.arguments?.getString("loadId").orEmpty())
                val tripId = Uri.decode(entry.arguments?.getString("tripId").orEmpty())
                val loadDate = Uri.decode(entry.arguments?.getString("loadDate").orEmpty())
                    .takeIf { it != "_" }.orEmpty()
                ScannerFlowScreen(
                    onFinished = { navController.popBackStack() },
                    onOpenGallery = { navController.navigate(Routes.SCAN_GALLERY) },
                    onCameraFallback = {
                        navController.popBackStack()
                        if (loadId.isNotBlank() && loadId != "_") {
                            navController.navigate(
                                Routes.cameraForLoad(loadId, tripId, loadDate),
                            ) { launchSingleTop = true }
                        } else {
                            navController.navigate(Routes.CAMERA) { launchSingleTop = true }
                        }
                    },
                    attachLoadId = loadId.takeIf { it.isNotBlank() && it != "_" },
                    attachTripId = tripId.takeIf { it.isNotBlank() && it != "_" },
                    attachLoadDate = loadDate,
                )
            }
            composable(Routes.SCAN_GALLERY) {
                ScanGalleryScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.PHOTO_GALLERY) {
                PhotoGalleryScreen(
                    onBack = { navController.popBackStack() },
                    onPhotoClick = { navController.navigate(Routes.photoDetail(it)) },
                )
            }
            composable(
                route = Routes.PHOTO_DETAIL,
                arguments = listOf(navArgument("photoId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val photoId = backStackEntry.arguments?.getString("photoId").orEmpty()
                PhotoDetailScreen(
                    photoId = photoId,
                    onBack = { navController.popBackStack() },
                )
            }
        }
        }
            IncomingCallOverlay(
                onAccept = { callId ->
                    navController.navigate(Routes.call(callId)) { launchSingleTop = true }
                },
            )
        }
    }
}

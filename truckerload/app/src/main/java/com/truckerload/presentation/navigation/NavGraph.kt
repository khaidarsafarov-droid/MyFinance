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
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.truckerload.presentation.di.LocalAuthStore
import com.truckerload.presentation.di.LocalProfileRepository
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.CircularProgressIndicator
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.truckerload.presentation.theme.rememberReduceMotion
import com.truckerload.presentation.theme.tabEnterTransition
import com.truckerload.presentation.theme.tabExitTransition
import com.truckerload.presentation.components.AdaptiveScaffold
import com.truckerload.presentation.components.DrawerDestination
import com.truckerload.presentation.components.navigateToMainRoute
import com.truckerload.presentation.components.shouldShowPhoneBottomBar
import com.truckerload.presentation.utils.AdaptiveScreenContainer
import com.truckerload.presentation.utils.isTablet
import com.truckerload.presentation.utils.useNavigationRail
import com.truckerload.presentation.utils.useTwoPaneLayout
import com.truckerload.presentation.screens.home.HomeScreen
import com.truckerload.presentation.screens.auth.ProfileSetupScreen
import com.truckerload.presentation.di.LocalUserProfileStore
import com.truckerload.widget.WidgetDeepLink
import kotlinx.coroutines.launch

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
    LaunchedEffect(isLoggedIn) {
        showMainContent = isLoggedIn
    }
    val context = LocalContext.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val tablet = isTablet()
    val navigationRail = useNavigationRail()

    if (!isLoggedIn) {
        // Auth UI is hosted by MainActivity (account switch recreates user-scoped deps).
        return
    }
    if (!showMainContent) {
        return
    }

    val profileRepository = LocalProfileRepository.current
    val userProfileStore = LocalUserProfileStore.current
    val setupComplete by userProfileStore.setupComplete.collectAsStateWithLifecycle()
    val authEmail by authStore.email.collectAsStateWithLifecycle()
    var needsSetup by remember { mutableStateOf<Boolean?>(null) }
    var needsEmailVerify by remember { mutableStateOf<Boolean?>(null) }
    var needsTelegramOnboarding by remember { mutableStateOf<Boolean?>(null) }
    val emailVerifyStore = remember(context) {
        com.truckerload.data.preferences.EmailVerificationStore(context.applicationContext)
    }
    val registrationService = com.truckerload.presentation.di.LocalRegistrationService.current
    val registrationScope = androidx.compose.runtime.rememberCoroutineScope()
    LaunchedEffect(isLoggedIn, setupComplete, authEmail) {
        if (!isLoggedIn) {
            needsSetup = null
            needsEmailVerify = null
            needsTelegramOnboarding = null
            return@LaunchedEffect
        }
        val provider = authStore.authProvider()
        val emailPending =
            provider == com.truckerload.data.preferences.AuthProvider.EMAIL &&
                authEmail.isNotBlank() &&
                emailVerifyStore.isPending(authEmail)
        // Step 2 (verification) happens before the profile wizard.
        needsEmailVerify = emailPending
        if (setupComplete) {
            needsSetup = false
            needsTelegramOnboarding = com.truckerload.data.preferences.TelegramOnboardingStore(
                context,
                authStore.currentUserIdOrNull(),
            ).shouldPrompt(context)
            launch {
                profileRepository.syncIdentityFromUserProfile()
                profileRepository.maybeMarkSetupCompleteFromExistingProfile()
            }
            return@LaunchedEffect
        }
        profileRepository.syncIdentityFromUserProfile()
        profileRepository.maybeMarkSetupCompleteFromExistingProfile()
        needsSetup = registrationService.needsRequiredOnboarding() ||
            profileRepository.needsProfileSetup()
        needsTelegramOnboarding = if (needsSetup == true) {
            false
        } else {
            com.truckerload.data.preferences.TelegramOnboardingStore(
                context,
                authStore.currentUserIdOrNull(),
            ).shouldPrompt(context)
        }
    }
    if (needsEmailVerify == true) {
        com.truckerload.presentation.screens.auth.EmailVerificationScreen(
            email = authEmail,
            onVerified = {
                needsEmailVerify = false
                registrationScope.launch {
                    runCatching { registrationService.markVerified() }
                }
            },
            onSkip = {
                needsEmailVerify = false
                registrationScope.launch {
                    runCatching { registrationService.skipVerificationForNow() }
                }
            },
        )
        return
    }
    if (needsSetup == true) {
        ProfileSetupScreen(
            onCompleted = {
                needsSetup = false
                needsTelegramOnboarding = com.truckerload.data.preferences
                    .TelegramOnboardingStore(context, authStore.currentUserIdOrNull())
                    .shouldPrompt(context)
            },
        )
        return
    }
    if (needsSetup == null || needsEmailVerify == null || needsTelegramOnboarding == null) {
        // Lightweight placeholder instead of a blank frame (reads as a freeze).
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }
    if (needsTelegramOnboarding == true) {
        com.truckerload.presentation.screens.auth.TelegramOnboardingScreen(
            onCompleted = { needsTelegramOnboarding = false },
            onSkip = { needsTelegramOnboarding = false },
        )
        return
    }

    // Handle widget deep links only after NavHost is about to be composed (setup gates passed).
    LaunchedEffect(deepLinkRoute, isLoggedIn, showMainContent, needsSetup, needsEmailVerify, needsTelegramOnboarding) {
        if (!isLoggedIn || !showMainContent) return@LaunchedEffect
        if (needsSetup != false || needsEmailVerify != false || needsTelegramOnboarding != false) {
            return@LaunchedEffect
        }
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
            Routes.ADD_DIESEL -> {
                navController.navigate(Routes.ADD_DIESEL) { launchSingleTop = true }
                onDeepLinkHandled()
            }
            else -> {
                if (destination.startsWith("attach_pick/")) {
                    navController.navigate(destination) { launchSingleTop = true }
                    onDeepLinkHandled()
                }
            }
        }
    }

    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route
    val showMainNavigation = if (tablet) {
        currentRoute != Routes.ADD_PAYCHECK && currentRoute != Routes.ADD_DIESEL &&
            currentRoute != Routes.DIESEL &&
            currentRoute != Routes.VOICE_ASSISTANT &&
            currentRoute != Routes.CAMERA && currentRoute != Routes.SCANNER &&
            currentRoute != Routes.CAMERA_FOR_LOAD && currentRoute != Routes.SCANNER_FOR_LOAD &&
            currentRoute != Routes.SCAN_GALLERY && currentRoute != Routes.PHOTO_GALLERY &&
            !currentRoute.orEmpty().startsWith("attach_pick") &&
            !currentRoute.orEmpty().startsWith("photo_detail")
    } else {
        shouldShowPhoneBottomBar(currentRoute)
    }

    val reduceMotion = rememberReduceMotion()

    AdaptiveScaffold(
        showMainNavigation = showMainNavigation,
        currentRoute = currentRoute,
        onNavigate = { route -> navigateToMainRoute(route, navController) },
        onDrawerNavigate = { destination ->
            when (destination) {
                DrawerDestination.SETTINGS -> navController.navigate(Routes.SETTINGS) { launchSingleTop = true }
                DrawerDestination.REPORTS -> navController.navigate(Routes.ANALYTICS) { launchSingleTop = true }
                DrawerDestination.MAP -> navController.navigate(Routes.MAP) { launchSingleTop = true }
                DrawerDestination.DOCUMENTS -> navController.navigate(Routes.SCAN_GALLERY) { launchSingleTop = true }
                DrawerDestination.MAINTENANCE -> navController.navigate(Routes.MAINTENANCE) { launchSingleTop = true }
                DrawerDestination.TAX_TRACKER -> navController.navigate(Routes.TAX_TRACKER) { launchSingleTop = true }
                DrawerDestination.ADD_PAYCHECK -> navController.navigate(Routes.ADD_PAYCHECK) { launchSingleTop = true }
                DrawerDestination.DIESEL -> navController.navigate(Routes.DIESEL) { launchSingleTop = true }
                DrawerDestination.VOICE_ASSISTANT -> navController.navigate(Routes.VOICE_ASSISTANT) { launchSingleTop = true }
                DrawerDestination.CAMERA -> navController.navigate(Routes.CAMERA) { launchSingleTop = true }
                DrawerDestination.SCANNER -> navController.navigate(Routes.SCANNER) { launchSingleTop = true }
                DrawerDestination.ABOUT -> navController.navigate(Routes.ABOUT) { launchSingleTop = true }
                DrawerDestination.IMPROVE -> navController.navigate(Routes.IMPROVE) { launchSingleTop = true }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
        AdaptiveScreenContainer(
            modifier = Modifier.padding(padding),
            useFullWidth = navigationRail && showMainNavigation,
        ) {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(
                route = Routes.HOME,
                enterTransition = { tabEnterTransition(reduceMotion) },
                exitTransition = { tabExitTransition(reduceMotion) },
                popEnterTransition = { tabEnterTransition(reduceMotion) },
                popExitTransition = { tabExitTransition(reduceMotion) },
            ) {
                if (useTwoPaneLayout()) {
                    JournalListDetailHost(navController = navController)
                } else {
                    HomeScreen(
                        onLoadClick = { navController.navigate(Routes.loadDetail(it)) },
                        onAddLoad = { navController.navigate(Routes.ADD_LOAD) },
                        onStats = { navController.navigate(Routes.ANALYTICS) },
                        onWeeklyGoal = { navigateToMainRoute(Routes.STATS, navController) },
                        onSettings = { navController.navigate(Routes.SETTINGS) },
                        onCamera = { navController.navigate(Routes.CAMERA) { launchSingleTop = true } },
                        onScan = { navController.navigate(Routes.SCANNER) { launchSingleTop = true } },
                        onAddDiesel = { navController.navigate(Routes.ADD_DIESEL) { launchSingleTop = true } },
                        onVoiceAssistant = { navController.navigate(Routes.VOICE_ASSISTANT) { launchSingleTop = true } },
                        onLoadCamera = { loadId, tripId, loadDate ->
                            navController.navigate(Routes.cameraForLoad(loadId, tripId, loadDate))
                        },
                        onLoadScan = { loadId, tripId, loadDate ->
                            navController.navigate(Routes.scannerForLoad(loadId, tripId, loadDate))
                        },
                        onOpenPrivacy = { navController.navigate(Routes.PRIVACY_SETTINGS) },
                    )
                }
            }
            profileNavGraph(navController, reduceMotion)
            loadsNavGraph(navController)
            toolsNavGraph(navController, tablet, reduceMotion)
        }
        }
        }
    }
}

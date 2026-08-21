package com.truckerload.presentation.navigation

import android.net.Uri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.truckerload.presentation.screens.about.AboutAppScreen
import com.truckerload.presentation.screens.advisor.FinancialAdvisorScreen
import com.truckerload.presentation.screens.assistant.VoiceAssistantScreen
import com.truckerload.presentation.screens.analytics.AnalyticsScreen
import com.truckerload.presentation.screens.attach.AttachLoadPickScreen
import com.truckerload.presentation.screens.attach.AttachPickMode
import com.truckerload.presentation.screens.camera.CameraFlowScreen
import com.truckerload.presentation.screens.gallery.PhotoDetailScreen
import com.truckerload.presentation.screens.gallery.PhotoGalleryScreen
import com.truckerload.presentation.screens.goal.WeeklyGoalScreen
import com.truckerload.presentation.screens.maintenance.MaintenanceScreen
import com.truckerload.presentation.screens.social.friends.map.FriendsLiveMapScreen
import com.truckerload.presentation.screens.map.MapScreen
import com.truckerload.presentation.screens.scanner.ScanGalleryScreen
import com.truckerload.presentation.screens.scanner.ScannerFlowScreen
import com.truckerload.presentation.screens.privacy.PrivacySettingsScreen
import com.truckerload.presentation.screens.settings.SettingsScreen
import com.truckerload.presentation.screens.stats.StatsScreen
import com.truckerload.presentation.theme.tabEnterTransition
import com.truckerload.presentation.theme.tabExitTransition

fun NavGraphBuilder.toolsNavGraph(
    navController: NavHostController,
    tablet: Boolean,
    reduceMotion: Boolean,
) {
    composable(
        route = Routes.ANALYTICS,
        enterTransition = { tabEnterTransition(reduceMotion) },
        exitTransition = { tabExitTransition(reduceMotion) },
        popEnterTransition = { tabEnterTransition(reduceMotion) },
        popExitTransition = { tabExitTransition(reduceMotion) },
    ) {
        AnalyticsScreen(
            onBack = { navController.popBackStack() },
            onLoadClick = { loadId -> navController.navigate(Routes.loadDetail(loadId)) },
            onAdvancedStats = { navController.navigate(Routes.ADVANCED_STATS) },
            onOpenMap = { navController.navigate(Routes.MAP) },
        )
    }
    composable(
        route = Routes.ADVANCED_STATS,
        enterTransition = { tabEnterTransition(reduceMotion) },
        exitTransition = { tabExitTransition(reduceMotion) },
        popEnterTransition = { tabEnterTransition(reduceMotion) },
        popExitTransition = { tabExitTransition(reduceMotion) },
    ) {
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
        enterTransition = { tabEnterTransition(reduceMotion) },
        exitTransition = { tabExitTransition(reduceMotion) },
        popEnterTransition = { tabEnterTransition(reduceMotion) },
        popExitTransition = { tabExitTransition(reduceMotion) },
    ) {
        WeeklyGoalScreen()
    }
    composable(
        route = Routes.MAP,
        enterTransition = { tabEnterTransition(reduceMotion) },
        exitTransition = { tabExitTransition(reduceMotion) },
        popEnterTransition = { tabEnterTransition(reduceMotion) },
        popExitTransition = { tabExitTransition(reduceMotion) },
    ) {
        MapScreen(onBack = { navController.popBackStack() })
    }
    composable(Routes.FRIENDS_LIVE) {
        FriendsLiveMapScreen(
            onBack = { navController.popBackStack() },
        )
    }
    composable(Routes.MAINTENANCE) {
        MaintenanceScreen(onBack = { navController.popBackStack() })
    }
    composable(Routes.FINANCIAL_ADVISOR) {
        FinancialAdvisorScreen(onBack = { navController.popBackStack() })
    }
    composable(Routes.VOICE_ASSISTANT) {
        VoiceAssistantScreen(onBack = { navController.popBackStack() })
    }
    composable(
        route = Routes.SETTINGS,
        enterTransition = { tabEnterTransition(reduceMotion) },
        exitTransition = { tabExitTransition(reduceMotion) },
        popEnterTransition = { tabEnterTransition(reduceMotion) },
        popExitTransition = { tabExitTransition(reduceMotion) },
    ) {
        SettingsScreen(
            onBack = { navController.popBackStack() },
            showBack = !tablet,
            onOpenPrivacy = { navController.navigate(Routes.PRIVACY_SETTINGS) },
        )
    }
    composable(
        route = Routes.PRIVACY_SETTINGS,
        enterTransition = { tabEnterTransition(reduceMotion) },
        exitTransition = { tabExitTransition(reduceMotion) },
        popEnterTransition = { tabEnterTransition(reduceMotion) },
        popExitTransition = { tabExitTransition(reduceMotion) },
    ) {
        PrivacySettingsScreen(
            onBack = { navController.popBackStack() },
            showBack = !tablet,
        )
    }
    composable(Routes.ABOUT) {
        AboutAppScreen(
            onBack = { navController.popBackStack() },
        )
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

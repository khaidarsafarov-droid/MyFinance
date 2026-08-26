package com.truckerload.presentation.navigation

import android.net.Uri
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.truckerload.presentation.screens.about.AboutAppScreen
import com.truckerload.presentation.screens.feedback.ImprovementFeedbackScreen
import com.truckerload.presentation.screens.analytics.AnalyticsScreen
import com.truckerload.presentation.screens.attach.AttachLoadPickScreen
import com.truckerload.presentation.screens.attach.AttachPickMode
import com.truckerload.presentation.screens.camera.CameraFlowScreen
import com.truckerload.presentation.screens.gallery.PhotoDetailScreen
import com.truckerload.presentation.screens.gallery.PhotoGalleryScreen
import com.truckerload.presentation.screens.goal.WeeklyGoalScreen
import com.truckerload.presentation.screens.maintenance.MaintenanceScreen
import com.truckerload.presentation.screens.map.MapScreen
import com.truckerload.presentation.screens.scanner.ScanGalleryScreen
import com.truckerload.presentation.screens.scanner.ScannerFlowScreen
import com.truckerload.presentation.screens.privacy.PrivacySettingsScreen
import com.truckerload.presentation.screens.settings.SettingsScreen
import com.truckerload.presentation.screens.tax.TaxTrackerScreen
import com.truckerload.presentation.theme.ProvideLoadSharedElementScopes
import com.truckerload.presentation.theme.navForwardEnter
import com.truckerload.presentation.theme.navForwardExit
import com.truckerload.presentation.theme.navModalEnter
import com.truckerload.presentation.theme.navModalExit
import com.truckerload.presentation.theme.navModalPopEnter
import com.truckerload.presentation.theme.navModalPopExit
import com.truckerload.presentation.theme.navPopEnter
import com.truckerload.presentation.theme.navPopExit
import com.truckerload.presentation.theme.tabEnterTransition
import com.truckerload.presentation.theme.tabExitTransition

@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.toolsNavGraph(
    navController: NavHostController,
    tablet: Boolean,
    reduceMotion: Boolean,
    sharedTransitionScope: SharedTransitionScope,
) {
    composable(
        route = Routes.ANALYTICS,
        enterTransition = { tabEnterTransition(reduceMotion) },
        exitTransition = { tabExitTransition(reduceMotion) },
        popEnterTransition = { tabEnterTransition(reduceMotion) },
        popExitTransition = { tabExitTransition(reduceMotion) },
    ) {
        ProvideLoadSharedElementScopes(
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = this,
        ) {
            AnalyticsScreen(
                onBack = { navController.popBackStack() },
                onLoadClick = { loadId -> navController.navigate(Routes.loadDetail(loadId)) },
                onAbout = { navController.navigate(Routes.ABOUT) { launchSingleTop = true } },
                onImprove = { navController.navigate(Routes.IMPROVE) { launchSingleTop = true } },
            )
        }
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
    composable(
        route = Routes.MAINTENANCE,
        enterTransition = { navForwardEnter(reduceMotion) },
        exitTransition = { navForwardExit(reduceMotion) },
        popEnterTransition = { navPopEnter(reduceMotion) },
        popExitTransition = { navPopExit(reduceMotion) },
    ) {
        MaintenanceScreen(onBack = { navController.popBackStack() })
    }
    composable(
        route = Routes.TAX_TRACKER,
        enterTransition = { navForwardEnter(reduceMotion) },
        exitTransition = { navForwardExit(reduceMotion) },
        popEnterTransition = { navPopEnter(reduceMotion) },
        popExitTransition = { navPopExit(reduceMotion) },
    ) {
        TaxTrackerScreen(onBack = { navController.popBackStack() })
    }
    composable(
        route = Routes.SETTINGS,
        enterTransition = { navForwardEnter(reduceMotion) },
        exitTransition = { navForwardExit(reduceMotion) },
        popEnterTransition = { navPopEnter(reduceMotion) },
        popExitTransition = { navPopExit(reduceMotion) },
    ) {
        SettingsScreen(
            onBack = { navController.popBackStack() },
            showBack = !tablet,
            onOpenPrivacy = { navController.navigate(Routes.PRIVACY_SETTINGS) },
        )
    }
    composable(
        route = Routes.PRIVACY_SETTINGS,
        enterTransition = { navForwardEnter(reduceMotion) },
        exitTransition = { navForwardExit(reduceMotion) },
        popEnterTransition = { navPopEnter(reduceMotion) },
        popExitTransition = { navPopExit(reduceMotion) },
    ) {
        PrivacySettingsScreen(
            onBack = { navController.popBackStack() },
            showBack = !tablet,
        )
    }
    composable(
        route = Routes.ABOUT,
        enterTransition = { navForwardEnter(reduceMotion) },
        exitTransition = { navForwardExit(reduceMotion) },
        popEnterTransition = { navPopEnter(reduceMotion) },
        popExitTransition = { navPopExit(reduceMotion) },
    ) {
        AboutAppScreen(
            onBack = { navController.popBackStack() },
            onWriteImprove = { navController.navigate(Routes.IMPROVE) { launchSingleTop = true } },
        )
    }
    composable(
        route = Routes.IMPROVE,
        enterTransition = { navForwardEnter(reduceMotion) },
        exitTransition = { navForwardExit(reduceMotion) },
        popEnterTransition = { navPopEnter(reduceMotion) },
        popExitTransition = { navPopExit(reduceMotion) },
    ) {
        ImprovementFeedbackScreen(
            onBack = { navController.popBackStack() },
        )
    }
    composable(
        route = Routes.ATTACH_PICK,
        arguments = listOf(
            navArgument("mode") { type = NavType.StringType },
        ),
        enterTransition = { navModalEnter(reduceMotion) },
        exitTransition = { navModalExit(reduceMotion) },
        popEnterTransition = { navModalPopEnter(reduceMotion) },
        popExitTransition = { navModalPopExit(reduceMotion) },
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
    composable(
        route = Routes.CAMERA,
        enterTransition = { navModalEnter(reduceMotion) },
        exitTransition = { navModalExit(reduceMotion) },
        popEnterTransition = { navModalPopEnter(reduceMotion) },
        popExitTransition = { navModalPopExit(reduceMotion) },
    ) {
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
        enterTransition = { navModalEnter(reduceMotion) },
        exitTransition = { navModalExit(reduceMotion) },
        popEnterTransition = { navModalPopEnter(reduceMotion) },
        popExitTransition = { navModalPopExit(reduceMotion) },
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
    composable(
        route = Routes.SCANNER,
        enterTransition = { navModalEnter(reduceMotion) },
        exitTransition = { navModalExit(reduceMotion) },
        popEnterTransition = { navModalPopEnter(reduceMotion) },
        popExitTransition = { navModalPopExit(reduceMotion) },
    ) {
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
        enterTransition = { navModalEnter(reduceMotion) },
        exitTransition = { navModalExit(reduceMotion) },
        popEnterTransition = { navModalPopEnter(reduceMotion) },
        popExitTransition = { navModalPopExit(reduceMotion) },
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
    composable(
        route = Routes.SCAN_GALLERY,
        enterTransition = { navModalEnter(reduceMotion) },
        exitTransition = { navModalExit(reduceMotion) },
        popEnterTransition = { navModalPopEnter(reduceMotion) },
        popExitTransition = { navModalPopExit(reduceMotion) },
    ) {
        ScanGalleryScreen(onBack = { navController.popBackStack() })
    }
    composable(
        route = Routes.PHOTO_GALLERY,
        enterTransition = { navModalEnter(reduceMotion) },
        exitTransition = { navModalExit(reduceMotion) },
        popEnterTransition = { navModalPopEnter(reduceMotion) },
        popExitTransition = { navModalPopExit(reduceMotion) },
    ) {
        PhotoGalleryScreen(
            onBack = { navController.popBackStack() },
            onPhotoClick = { navController.navigate(Routes.photoDetail(it)) },
        )
    }
    composable(
        route = Routes.PHOTO_DETAIL,
        arguments = listOf(navArgument("photoId") { type = NavType.StringType }),
        enterTransition = { navModalEnter(reduceMotion) },
        exitTransition = { navModalExit(reduceMotion) },
        popEnterTransition = { navModalPopEnter(reduceMotion) },
        popExitTransition = { navModalPopExit(reduceMotion) },
    ) { backStackEntry ->
        val photoId = backStackEntry.arguments?.getString("photoId").orEmpty()
        PhotoDetailScreen(
            photoId = photoId,
            onBack = { navController.popBackStack() },
        )
    }
}

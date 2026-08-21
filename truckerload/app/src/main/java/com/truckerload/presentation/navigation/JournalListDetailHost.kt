package com.truckerload.presentation.navigation

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.truckerload.presentation.components.ListDetailEmptyPane
import com.truckerload.presentation.components.ListDetailLayout
import com.truckerload.presentation.screens.detail.LoadDetailScreen
import com.truckerload.presentation.screens.home.HomeScreen

private const val PANE_EMPTY = "pane_empty"

/**
 * Expanded-width journal: load list on the left, detail (nested NavHost) on the right.
 * Edit / photo / camera actions still use the parent [navController].
 */
@Composable
fun JournalListDetailHost(
    navController: NavHostController,
) {
    val detailNavController = rememberNavController()

    ListDetailLayout(
        listContent = {
            HomeScreen(
                onLoadClick = { loadId ->
                    detailNavController.navigate(Routes.loadDetail(loadId)) {
                        popUpTo(PANE_EMPTY) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onAddLoad = { navController.navigate(Routes.ADD_LOAD) },
                onStats = { navController.navigate(Routes.ANALYTICS) },
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
        },
        detailContent = {
            NavHost(
                navController = detailNavController,
                startDestination = PANE_EMPTY,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(PANE_EMPTY) {
                    ListDetailEmptyPane()
                }
                composable(
                    route = Routes.LOAD_DETAIL,
                    arguments = listOf(navArgument("loadId") { type = NavType.StringType }),
                ) { entry ->
                    val loadId = Uri.decode(entry.arguments?.getString("loadId").orEmpty())
                    LoadDetailScreen(
                        loadId = loadId,
                        onBack = {
                            detailNavController.popBackStack(PANE_EMPTY, inclusive = false)
                        },
                        onEdit = { navController.navigate(Routes.editLoad(loadId)) },
                        onEditFinish = {
                            navController.navigate(Routes.editLoad(loadId, focusFinish = true))
                        },
                        onDelete = {
                            detailNavController.popBackStack(PANE_EMPTY, inclusive = false)
                        },
                        onPhotoClick = { navController.navigate(Routes.photoDetail(it)) },
                        onOpenPrivacy = { navController.navigate(Routes.PRIVACY_SETTINGS) },
                    )
                }
            }
        },
    )
}

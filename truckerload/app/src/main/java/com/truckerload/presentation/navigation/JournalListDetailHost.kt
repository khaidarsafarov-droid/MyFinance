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
import com.truckerload.presentation.theme.navForwardEnter
import com.truckerload.presentation.theme.navForwardExit
import com.truckerload.presentation.theme.navPopEnter
import com.truckerload.presentation.theme.navPopExit
import com.truckerload.presentation.theme.navSharedElementEnter
import com.truckerload.presentation.theme.navSharedElementExit
import com.truckerload.presentation.theme.rememberReduceMotion

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
    val reduceMotion = rememberReduceMotion()

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
                onWeeklyGoal = { navController.navigate(Routes.STATS) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onAddDiesel = { navController.navigate(Routes.ADD_DIESEL) { launchSingleTop = true } },
                onOpenDiesel = { navController.navigate(Routes.DIESEL) },
                onOpenPaycheck = { navController.navigate(Routes.PAYCHECK) },
                onOpenExpense = { navController.navigate(Routes.MISC_EXPENSES) },
                onOpenScanner = { navController.navigate(Routes.SCANNER) },
                onAvatar = { navController.navigate(Routes.PROFILE) },
                onLoadCamera = { loadId, tripId, loadDate ->
                    navController.navigate(Routes.cameraForLoad(loadId, tripId, loadDate))
                },
                onLoadScan = { loadId, tripId, loadDate ->
                    navController.navigate(Routes.scannerForLoad(loadId, tripId, loadDate))
                },
                onOpenPrivacy = { navController.navigate(Routes.PRIVACY_SETTINGS) },
                onOpenPaywall = {
                    navController.navigate(Routes.PAYWALL) { launchSingleTop = true }
                },
            )
        },
        detailContent = {
            NavHost(
                navController = detailNavController,
                startDestination = PANE_EMPTY,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { navForwardEnter(reduceMotion) },
                exitTransition = { navForwardExit(reduceMotion) },
                popEnterTransition = { navPopEnter(reduceMotion) },
                popExitTransition = { navPopExit(reduceMotion) },
            ) {
                composable(PANE_EMPTY) {
                    ListDetailEmptyPane()
                }
                composable(
                    route = Routes.LOAD_DETAIL,
                    arguments = listOf(navArgument("loadId") { type = NavType.StringType }),
                    enterTransition = { navSharedElementEnter(reduceMotion) },
                    exitTransition = { navSharedElementExit(reduceMotion) },
                    popEnterTransition = { navSharedElementEnter(reduceMotion) },
                    popExitTransition = { navSharedElementExit(reduceMotion) },
                ) { entry ->
                    val loadId = Uri.decode(entry.arguments?.getString("loadId").orEmpty())
                    LoadDetailScreen(
                        loadId = loadId,
                        onBack = {
                            detailNavController.popBackStack(PANE_EMPTY, inclusive = false)
                        },
                        onEdit = { navController.navigate(Routes.editLoad(loadId)) },
                        onEditMiles = { navController.navigate(Routes.editLoad(loadId, focusMiles = true)) },
                        onEditFinish = {
                            navController.navigate(Routes.editLoad(loadId, focusFinish = true))
                        },
                        onDelete = {
                            detailNavController.popBackStack(PANE_EMPTY, inclusive = false)
                        },
                        onPhotoClick = { navController.navigate(Routes.photoDetail(it)) },
                        onScanClick = { navController.navigate(Routes.scanDetail(it)) },
                        onCamera = { id, tripId, loadDate ->
                            navController.navigate(Routes.cameraForLoad(id, tripId, loadDate))
                        },
                        onScanner = { id, tripId, loadDate ->
                            navController.navigate(Routes.scannerForLoad(id, tripId, loadDate))
                        },
                        onOpenPrivacy = { navController.navigate(Routes.PRIVACY_SETTINGS) },
                    )
                }
            }
        },
    )
}

package com.truckerload.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.truckerload.R
import com.truckerload.presentation.components.ListDetailEmptyPane
import com.truckerload.presentation.components.ListDetailLayout
import com.truckerload.presentation.screens.add.AddDieselScreen
import com.truckerload.presentation.screens.diesel.DieselJournalScreen
import com.truckerload.presentation.theme.navForwardEnter
import com.truckerload.presentation.theme.navForwardExit
import com.truckerload.presentation.theme.navPopEnter
import com.truckerload.presentation.theme.navPopExit
import com.truckerload.presentation.theme.rememberReduceMotion

private const val DIESEL_PANE_EMPTY = "diesel_pane_empty"

/**
 * Expanded-width diesel journal: fill-ups on the left, add/edit form on the right,
 * like [JournalListDetailHost] for loads. Scan still uses the parent [navController].
 */
@Composable
fun DieselListDetailHost(
    navController: NavHostController,
) {
    val detailNavController = rememberNavController()
    val reduceMotion = rememberReduceMotion()
    // No launchSingleTop: a new dieselId must get a fresh entry (and ViewModel).
    fun openEditor(route: String) {
        detailNavController.navigate(route) {
            popUpTo(DIESEL_PANE_EMPTY) { inclusive = false }
        }
    }
    val closeEditor: () -> Unit = {
        detailNavController.popBackStack(DIESEL_PANE_EMPTY, inclusive = false)
    }

    ListDetailLayout(
        listContent = {
            DieselJournalScreen(
                onBack = { navController.popBackStack() },
                onAdd = { openEditor(Routes.ADD_DIESEL) },
                onScan = { navController.navigate(Routes.SCANNER) },
                onEditDiesel = { id -> openEditor(Routes.editDiesel(id)) },
            )
        },
        detailContent = {
            NavHost(
                navController = detailNavController,
                startDestination = DIESEL_PANE_EMPTY,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { navForwardEnter(reduceMotion) },
                exitTransition = { navForwardExit(reduceMotion) },
                popEnterTransition = { navPopEnter(reduceMotion) },
                popExitTransition = { navPopExit(reduceMotion) },
            ) {
                composable(DIESEL_PANE_EMPTY) {
                    ListDetailEmptyPane(
                        titleRes = R.string.tablet_diesel_empty_title,
                        bodyRes = R.string.tablet_diesel_empty_body,
                    )
                }
                composable(
                    route = Routes.ADD_DIESEL_WITH_ID,
                    arguments = listOf(
                        navArgument("dieselId") {
                            type = NavType.IntType
                            defaultValue = -1
                        },
                    ),
                ) {
                    AddDieselScreen(onSaved = closeEditor, onBack = closeEditor)
                }
            }
        },
    )
}

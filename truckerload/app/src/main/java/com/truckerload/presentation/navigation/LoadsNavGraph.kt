package com.truckerload.presentation.navigation

import android.net.Uri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.truckerload.presentation.screens.add.AddDieselScreen
import com.truckerload.presentation.screens.add.AddLoadScreen
import com.truckerload.presentation.screens.add.AddPaycheckScreen
import com.truckerload.presentation.screens.diesel.DieselJournalScreen
import com.truckerload.presentation.screens.paycheck.PaycheckJournalScreen
import com.truckerload.presentation.screens.detail.LoadDetailScreen
import com.truckerload.presentation.screens.edit.EditLoadScreen
import com.truckerload.presentation.screens.expenses.MiscExpenseScreen
import com.truckerload.presentation.screens.home.HomeViewModel

fun NavGraphBuilder.loadsNavGraph(navController: NavHostController) {
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
            onOpenPrivacy = { navController.navigate(Routes.PRIVACY_SETTINGS) },
        )
    }
    composable(Routes.ADD_LOAD) { addLoadEntry ->
        val homeEntry = remember(addLoadEntry) {
            runCatching { navController.getBackStackEntry(Routes.HOME) }.getOrNull()
        }
        val homeViewModel: HomeViewModel? = homeEntry?.let {
            hiltViewModel(it)
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
        val homeEntry = remember(editBackStackEntry) {
            runCatching { navController.getBackStackEntry(Routes.HOME) }.getOrNull()
        }
        val homeViewModel: HomeViewModel? = homeEntry?.let {
            hiltViewModel(it)
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
    composable(Routes.PAYCHECK) {
        PaycheckJournalScreen(
            onBack = { navController.popBackStack() },
            onAdd = { navController.navigate(Routes.ADD_PAYCHECK) { launchSingleTop = true } },
        )
    }
    composable(Routes.ADD_PAYCHECK) {
        AddPaycheckScreen(onSaved = { navController.popBackStack() }, onBack = { navController.popBackStack() })
    }
    composable(Routes.DIESEL) {
        DieselJournalScreen(
            onBack = { navController.popBackStack() },
            onAdd = { navController.navigate(Routes.ADD_DIESEL) { launchSingleTop = true } },
            onEditDiesel = { id ->
                navController.navigate(Routes.editDiesel(id)) { launchSingleTop = true }
            },
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
        AddDieselScreen(onSaved = { navController.popBackStack() }, onBack = { navController.popBackStack() })
    }
    composable(Routes.MISC_EXPENSES) {
        MiscExpenseScreen(onBack = { navController.popBackStack() })
    }
}

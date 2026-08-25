package com.truckerload.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.truckerload.presentation.components.navigateToMainRoute
import com.truckerload.presentation.screens.social.ProfileScreen
import com.truckerload.presentation.theme.tabEnterTransition
import com.truckerload.presentation.theme.tabExitTransition

fun NavGraphBuilder.profileNavGraph(navController: NavHostController, reduceMotion: Boolean) {
    composable(
        route = Routes.PROFILE,
        enterTransition = { tabEnterTransition(reduceMotion) },
        exitTransition = { tabExitTransition(reduceMotion) },
        popEnterTransition = { tabEnterTransition(reduceMotion) },
        popExitTransition = { tabExitTransition(reduceMotion) },
    ) {
        ProfileScreen(
            onBack = { navigateToMainRoute(Routes.HOME, navController) },
            showBack = false,
        )
    }
}

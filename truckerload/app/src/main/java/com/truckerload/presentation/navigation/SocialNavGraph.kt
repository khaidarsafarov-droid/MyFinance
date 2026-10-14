package com.truckerload.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.truckerload.presentation.components.mainTabSlideSign
import com.truckerload.presentation.components.navigateToMainRoute
import com.truckerload.presentation.screens.social.ProfileScreen
import com.truckerload.presentation.theme.mainTabEnter
import com.truckerload.presentation.theme.mainTabExit

fun NavGraphBuilder.profileNavGraph(navController: NavHostController, reduceMotion: Boolean) {
    composable(
        route = Routes.PROFILE,
        enterTransition = {
            mainTabEnter(
                reduceMotion,
                mainTabSlideSign(initialState.destination.route, targetState.destination.route),
            )
        },
        exitTransition = {
            mainTabExit(
                reduceMotion,
                mainTabSlideSign(initialState.destination.route, targetState.destination.route),
            )
        },
        popEnterTransition = {
            mainTabEnter(
                reduceMotion,
                mainTabSlideSign(initialState.destination.route, targetState.destination.route),
            )
        },
        popExitTransition = {
            mainTabExit(
                reduceMotion,
                mainTabSlideSign(initialState.destination.route, targetState.destination.route),
            )
        },
    ) {
        ProfileScreen(
            onBack = { navigateToMainRoute(Routes.HOME, navController) },
            showBack = false,
        )
    }
}

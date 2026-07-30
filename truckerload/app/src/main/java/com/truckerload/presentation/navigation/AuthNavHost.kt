package com.truckerload.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.truckerload.data.preferences.AuthStore
import com.truckerload.presentation.screens.login.LoginScreen

/**
 * Auth gate before the main app. Android: Google Sign-In only.
 * Apple / iCloud Sign in with Apple will live on the iOS client later.
 */
@Composable
fun AuthNavHost(
    authStore: AuthStore,
    onLoginSuccess: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AuthRoutes.LOGIN
    ) {
        composable(AuthRoutes.LOGIN) {
            LoginScreen(onSignedIn = onLoginSuccess)
        }
    }
}

object AuthRoutes {
    const val LOGIN = "auth_login"
}

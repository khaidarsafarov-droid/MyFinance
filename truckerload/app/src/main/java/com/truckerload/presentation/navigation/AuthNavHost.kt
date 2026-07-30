package com.truckerload.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.truckerload.data.preferences.AuthStore
import com.truckerload.presentation.screens.auth.SignUpScreen
import com.truckerload.presentation.screens.login.LoginScreen

/**
 * Auth gate before the main app.
 * Android: Google or email/password. Sessions persist across launches.
 * iOS (planned): Sign in with Apple / iCloud.
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
            LoginScreen(
                onCreateAccount = { navController.navigate(AuthRoutes.SIGNUP) }
            )
        }
        composable(AuthRoutes.SIGNUP) {
            SignUpScreen(
                onBack = { navController.popBackStack() },
                onSuccess = onLoginSuccess,
            )
        }
    }
}

object AuthRoutes {
    const val LOGIN = "auth_login"
    const val SIGNUP = "auth_signup"
}

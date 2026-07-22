package com.truckerload.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.truckerload.data.preferences.AuthStore
import com.truckerload.presentation.screens.auth.SignUpScreen
import com.truckerload.presentation.screens.login.LoginScreen

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

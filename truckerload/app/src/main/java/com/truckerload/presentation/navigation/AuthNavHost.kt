package com.truckerload.presentation.navigation

import androidx.compose.runtime.Composable
import com.truckerload.data.preferences.AuthStore
import com.truckerload.presentation.screens.auth.FirstRunNameScreen

/**
 * First launch gate: local first + last name. No Google/email account.
 * Google Drive backup is connected later from Settings.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun AuthNavHost(
    authStore: AuthStore,
    onLoginSuccess: () -> Unit,
) {
    FirstRunNameScreen()
}

package com.truckerload.presentation.navigation

import androidx.compose.runtime.Composable
import com.truckerload.data.preferences.AuthStore
import com.truckerload.presentation.screens.auth.FirstRunNameScreen

/**
 * First launch gate: optional first + last name. Skip is allowed.
 * Backups are files the user saves from Settings.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun AuthNavHost(
    authStore: AuthStore,
    onLoginSuccess: () -> Unit,
) {
    FirstRunNameScreen()
}

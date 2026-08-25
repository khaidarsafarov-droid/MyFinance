package com.truckerload.presentation.screens.auth

import androidx.compose.runtime.Composable

/**
 * Required onboarding after credentials. Profile itself is read-only;
 * Google sign-in is used later for cloud backup, not social editing.
 */
@Composable
fun ProfileSetupScreen(
    onCompleted: () -> Unit,
) {
    RegistrationFlowScreen(onCompleted = onCompleted)
}

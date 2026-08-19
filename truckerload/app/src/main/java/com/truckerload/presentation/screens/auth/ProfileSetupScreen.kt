package com.truckerload.presentation.screens.auth

import androidx.compose.runtime.Composable

/**
 * Required onboarding after credentials. Optional professional / community steps
 * can be skipped and completed later from Profile.
 */
@Composable
fun ProfileSetupScreen(
    onCompleted: () -> Unit,
) {
    RegistrationFlowScreen(onCompleted = onCompleted)
}

package com.truckerload.presentation.voice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController

@Composable
fun VoiceCommandHandler(
    navController: NavHostController,
    viewModel: VoiceCommandViewModel = hiltViewModel(),
) {
    val destination by viewModel.navigateTo.collectAsStateWithLifecycle()
    LaunchedEffect(destination) {
        val route = destination ?: return@LaunchedEffect
        navController.navigate(route) { launchSingleTop = true }
        viewModel.onNavigated()
    }
}

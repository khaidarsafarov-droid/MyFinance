package com.truckerload.presentation.screens.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Stable
class FriendsMapLocationPermissionState(
    hasPermissionInitial: Boolean,
) {
    var hasPermission by mutableStateOf(hasPermissionInitial)
        internal set

    internal var requestAction: () -> Unit = {}

    fun requestPermission() = requestAction()
}

@Composable
fun rememberFriendsMapLocationPermission(): FriendsMapLocationPermissionState {
    val context = LocalContext.current
    val state = remember {
        FriendsMapLocationPermissionState(context.hasFriendsLocationPermission())
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        state.hasPermission = result.values.any { it }
    }
    state.requestAction = {
        launcher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }
    return state
}

private fun android.content.Context.hasFriendsLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

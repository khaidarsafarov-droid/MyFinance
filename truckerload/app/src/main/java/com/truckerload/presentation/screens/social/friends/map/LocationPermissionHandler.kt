package com.truckerload.presentation.screens.social.friends.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.truckerload.R
import com.truckerload.presentation.privacy.PermissionRationaleDialog

data class FriendsMapLocationPermission(
    val hasPermission: Boolean,
    val requestPermission: () -> Unit,
)

@Composable
fun rememberFriendsMapLocationPermission(
    requestOnLaunch: Boolean = true,
): FriendsMapLocationPermission {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var showRationale by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        hasPermission = result.values.any { it }
    }
    val requestSystemPermission: () -> Unit = remember(permissionLauncher) {
        {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }
    val requestPermission: () -> Unit = remember(hasPermission) {
        {
            if (!hasPermission) showRationale = true
        }
    }
    LaunchedEffect(requestOnLaunch) {
        if (requestOnLaunch && !hasPermission) {
            showRationale = true
        }
    }
    if (showRationale && !hasPermission) {
        PermissionRationaleDialog(
            title = stringResource(R.string.permission_rationale_location_title),
            body = stringResource(R.string.permission_rationale_location_body),
            onContinue = {
                showRationale = false
                requestSystemPermission()
            },
            onDismiss = { showRationale = false },
        )
    }
    return FriendsMapLocationPermission(
        hasPermission = hasPermission,
        requestPermission = requestPermission,
    )
}

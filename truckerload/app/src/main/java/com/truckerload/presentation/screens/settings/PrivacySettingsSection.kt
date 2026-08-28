package com.truckerload.presentation.screens.settings

import com.truckerload.presentation.icons.AppIcons

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.truckerload.R
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.theme.BentoGlassSection
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.utils.RuntimePermissionSnapshot

@Composable
fun PrivacySettingsSection(
    modifier: Modifier = Modifier,
    onOpenPrivacy: () -> Unit = {},
) {
    val context = LocalContext.current
    val tc = LocalTruckColors.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissions by remember { mutableStateOf(RuntimePermissionSnapshot.from(context)) }
    fun refreshPermissions() {
        permissions = RuntimePermissionSnapshot.from(context)
    }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val systemSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        refreshPermissions()
    }
    val cameraGranted = permissions.cameraGranted
    val locationGranted = permissions.locationGranted
    val notificationsGranted = permissions.notificationsGranted

    BentoGlassSection(
        title = stringResource(R.string.settings_privacy_title),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenPrivacy)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    AppIcons.Lock,
                    contentDescription = null,
                    tint = tc.AccentPrimary,
                    modifier = Modifier.size(20.dp),
                )
                Column {
                    Text(
                        text = stringResource(R.string.privacy_screen_title),
                        color = tc.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(R.string.privacy_settings_row_subtitle),
                        color = tc.TextSecondary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            Icon(
                AppIcons.ChevronRight,
                contentDescription = null,
                tint = tc.TextSecondary,
                modifier = Modifier.size(20.dp),
            )
        }

        PrivacyPermissionRow(
            icon = AppIcons.LocationOn,
            title = stringResource(R.string.settings_privacy_location),
            status = permissionStatusLabel(locationGranted),
            granted = locationGranted,
        )
        PrivacyPermissionRow(
            icon = AppIcons.CameraAlt,
            title = stringResource(R.string.settings_privacy_camera),
            status = permissionStatusLabel(cameraGranted),
            granted = cameraGranted,
        )
        PrivacyPermissionRow(
            icon = AppIcons.Notifications,
            title = stringResource(R.string.settings_privacy_notifications),
            status = permissionStatusLabel(notificationsGranted),
            granted = notificationsGranted,
        )

        Text(
            text = stringResource(R.string.settings_privacy_data_title),
            color = tc.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = stringResource(R.string.settings_privacy_data_loads),
            color = tc.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )

        OutlinedButton(
            onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                systemSettingsLauncher.launch(intent)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                AppIcons.PrivacyTip,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.settings_privacy_system_settings),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun PrivacyPermissionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    status: String,
    granted: Boolean,
) {
    val tc = LocalTruckColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = if (granted) tc.AccentPrimary else tc.TextSecondary,
                modifier = Modifier.size(20.dp),
            )
            Text(title, color = tc.TextPrimary, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            text = status,
            color = if (granted) tc.AccentProfit else tc.TextSecondary,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun permissionStatusLabel(granted: Boolean): String =
    stringResource(
        if (granted) R.string.settings_privacy_status_granted
        else R.string.settings_privacy_status_denied,
    )

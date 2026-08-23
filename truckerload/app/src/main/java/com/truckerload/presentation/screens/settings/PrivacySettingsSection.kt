package com.truckerload.presentation.screens.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.truckerload.R
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.theme.BentoGlassSection
import com.truckerload.presentation.theme.LocalTruckColors

@Composable
fun PrivacySettingsSection(
    modifier: Modifier = Modifier,
    onOpenPrivacy: () -> Unit = {},
) {
    val context = LocalContext.current
    val tc = LocalTruckColors.current

    val cameraGranted = permissionGranted(context, Manifest.permission.CAMERA)
    val micGranted = permissionGranted(context, Manifest.permission.RECORD_AUDIO)
    val locationGranted = permissionGranted(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
        permissionGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    val notificationsGranted = if (Build.VERSION.SDK_INT >= 33) {
        permissionGranted(context, Manifest.permission.POST_NOTIFICATIONS)
    } else {
        true
    }

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
                    Icons.Default.Lock,
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
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = tc.TextSecondary,
                modifier = Modifier.size(20.dp),
            )
        }

        PrivacyPermissionRow(
            icon = Icons.Default.LocationOn,
            title = stringResource(R.string.settings_privacy_location),
            status = permissionStatusLabel(locationGranted),
            granted = locationGranted,
        )
        PrivacyPermissionRow(
            icon = Icons.Default.CameraAlt,
            title = stringResource(R.string.settings_privacy_camera),
            status = permissionStatusLabel(cameraGranted),
            granted = cameraGranted,
        )
        PrivacyPermissionRow(
            icon = Icons.Default.Mic,
            title = stringResource(R.string.settings_privacy_mic),
            status = permissionStatusLabel(micGranted),
            granted = micGranted,
        )
        PrivacyPermissionRow(
            icon = Icons.Default.Notifications,
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
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                Icons.Default.PrivacyTip,
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

private fun permissionGranted(context: android.content.Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

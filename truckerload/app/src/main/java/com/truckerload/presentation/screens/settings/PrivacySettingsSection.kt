package com.truckerload.presentation.screens.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.di.LocalCallPrivacyStore
import com.truckerload.presentation.di.LocalSettingsDataStore
import com.truckerload.domain.voice.CallPrivacy
import com.truckerload.presentation.theme.AppSwitchDefaults
import com.truckerload.presentation.theme.BentoGlassSection
import com.truckerload.presentation.theme.LocalTruckColors
import kotlinx.coroutines.launch

@Composable
fun PrivacySettingsSection(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val tc = LocalTruckColors.current
    val settingsDataStore = LocalSettingsDataStore.current
    val scope = rememberCoroutineScope()
    val sharePath by settingsDataStore.sharePathWithFriends.collectAsStateWithLifecycle(false)
    val batterySaver by settingsDataStore.locationBatterySaver.collectAsStateWithLifecycle(false)

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

        WhoCanCallRows()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_privacy_share_path),
                    color = tc.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Switch(
                checked = sharePath,
                onCheckedChange = { enabled ->
                    scope.launch { settingsDataStore.saveSharePathWithFriends(enabled) }
                },
                colors = AppSwitchDefaults.colors(),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_privacy_battery_saver),
                    color = tc.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Switch(
                checked = batterySaver,
                onCheckedChange = { enabled ->
                    scope.launch { settingsDataStore.saveLocationBatterySaver(enabled) }
                },
                colors = AppSwitchDefaults.colors(),
            )
        }

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
private fun WhoCanCallRows() {
    val store = LocalCallPrivacyStore.current
    val privacy by store.privacy.collectAsStateWithLifecycle()
    val tc = LocalTruckColors.current
    Text(
        text = stringResource(R.string.call_who_can_call),
        color = tc.TextPrimary,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 8.dp),
    )
    CallPrivacy.entries.forEach { option ->
        val label = when (option) {
            CallPrivacy.EVERYONE -> stringResource(R.string.call_privacy_everyone)
            CallPrivacy.CONTACTS -> stringResource(R.string.call_privacy_contacts)
            CallPrivacy.NOBODY -> stringResource(R.string.call_privacy_nobody)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = privacy == option,
                onClick = { store.setPrivacy(option) },
            )
            Text(label, color = tc.TextPrimary, style = MaterialTheme.typography.bodyMedium)
        }
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

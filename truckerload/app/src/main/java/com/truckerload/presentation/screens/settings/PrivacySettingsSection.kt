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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.truckerload.presentation.di.LocalSettingsDataStore
import com.truckerload.presentation.theme.BentoGlassSection
import com.truckerload.presentation.theme.LocalTruckColors
import kotlinx.coroutines.launch

@Composable
fun PrivacySettingsSection(
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val settingsDataStore = LocalSettingsDataStore.current
    val scope = rememberCoroutineScope()
    val sharePath by settingsDataStore.sharePathWithFriends.collectAsStateWithLifecycle(initialValue = false)

    val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
    val locationGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    val cameraGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA,
    ) == PackageManager.PERMISSION_GRANTED
    val micGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED

    BentoGlassSection(
        title = stringResource(R.string.settings_privacy_title),
        subtitle = stringResource(R.string.settings_privacy_desc),
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    Icons.Default.PrivacyTip,
                    contentDescription = null,
                    tint = tc.AccentPrimary,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    stringResource(R.string.settings_privacy_permissions_heading),
                    color = tc.TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            PermissionStatusLine(
                label = stringResource(R.string.settings_privacy_notifications),
                granted = notificationsGranted,
            )
            PermissionStatusLine(
                label = stringResource(R.string.settings_privacy_location),
                granted = locationGranted,
            )
            PermissionStatusLine(
                label = stringResource(R.string.settings_privacy_camera),
                granted = cameraGranted,
            )
            PermissionStatusLine(
                label = stringResource(R.string.settings_privacy_microphone),
                granted = micGranted,
            )

            SettingsToggleRow(
                icon = {},
                label = stringResource(R.string.settings_privacy_share_path),
                checked = sharePath,
                onCheckedChange = { enabled ->
                    scope.launch { settingsDataStore.saveSharePathWithFriends(enabled) }
                },
            )
            Text(
                text = stringResource(R.string.settings_privacy_share_path_desc),
                style = MaterialTheme.typography.labelSmall,
                color = tc.TextSecondary,
            )

            OutlinedButton(
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(top = 4.dp),
            ) {
                Text(stringResource(R.string.settings_privacy_system_settings))
            }
        }
    }
}

@Composable
private fun PermissionStatusLine(
    label: String,
    granted: Boolean,
) {
    val tc = LocalTruckColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = tc.TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = stringResource(
                if (granted) R.string.settings_privacy_granted else R.string.settings_privacy_not_granted,
            ),
            color = if (granted) tc.AccentProfit else tc.AccentExpense,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

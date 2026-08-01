package com.truckerload.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.presentation.di.LocalSettingsDataStore
import com.truckerload.presentation.theme.AppSwitchDefaults
import com.truckerload.presentation.theme.BentoGlassSection
import com.truckerload.presentation.theme.LocalTruckColors
import kotlinx.coroutines.launch

@Composable
fun PrivacySettingsSection() {
    val settingsDataStore = LocalSettingsDataStore.current
    val tc = LocalTruckColors.current
    val scope = rememberCoroutineScope()
    val sharePath by settingsDataStore.sharePathWithFriends.collectAsStateWithLifecycle(initialValue = false)

    BentoGlassSection(
        title = stringResource(R.string.settings_privacy_title),
        subtitle = stringResource(R.string.settings_privacy_desc),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Outlined.Security,
                contentDescription = null,
                tint = tc.AccentPrimary,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = stringResource(R.string.settings_privacy_data_title),
                color = tc.TextPrimary,
                style = MaterialTheme.typography.titleSmall,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.settings_privacy_data_loads),
                color = tc.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(R.string.settings_privacy_data_telegram),
                color = tc.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(R.string.settings_privacy_data_backup),
                color = tc.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
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
                    Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = tc.AccentPrimary,
                    modifier = Modifier.size(22.dp),
                )
                Column {
                    Text(
                        text = stringResource(R.string.friends_share_path_toggle),
                        color = tc.TextPrimary,
                    )
                    Text(
                        text = stringResource(R.string.friends_share_path_hint),
                        color = tc.TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Switch(
                checked = sharePath,
                onCheckedChange = { enabled ->
                    scope.launch { settingsDataStore.saveSharePathWithFriends(enabled) }
                },
                colors = AppSwitchDefaults.colors(),
            )
        }
    }
}

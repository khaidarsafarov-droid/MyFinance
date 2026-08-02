package com.truckerload.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.di.LocalSettingsDataStore
import com.truckerload.presentation.theme.AppSwitchDefaults
import com.truckerload.presentation.theme.BentoGlassSection
import com.truckerload.presentation.theme.LocalTruckColors
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun NotificationSettingsSection(
    quietHoursEnabled: Boolean,
    quietHoursStart: Int,
    quietHoursEnd: Int,
    notifyMissingWeek: Boolean,
    notifyMaintenance: Boolean,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
    val settingsDataStore = LocalSettingsDataStore.current
    val scope = rememberCoroutineScope()

    BentoGlassSection(
        title = stringResource(R.string.settings_notifications_title),
        subtitle = stringResource(R.string.settings_notifications_desc),
        modifier = modifier,
    ) {
        SettingsToggleRow(
            title = stringResource(R.string.settings_notify_missing_week),
            checked = notifyMissingWeek,
            onCheckedChange = { enabled ->
                scope.launch { settingsDataStore.saveNotifyMissingWeek(enabled) }
            },
        )
        SettingsToggleRow(
            title = stringResource(R.string.settings_notify_maintenance),
            checked = notifyMaintenance,
            onCheckedChange = { enabled ->
                scope.launch { settingsDataStore.saveNotifyMaintenance(enabled) }
            },
        )
        SettingsToggleRow(
            title = stringResource(R.string.settings_quiet_hours_title),
            checked = quietHoursEnabled,
            onCheckedChange = { enabled ->
                scope.launch { settingsDataStore.saveQuietHoursEnabled(enabled) }
            },
        )
        Text(
            text = stringResource(R.string.settings_quiet_hours_desc),
            style = MaterialTheme.typography.bodySmall,
            color = tc.TextSecondary,
        )
        if (quietHoursEnabled) {
            QuietHourSlider(
                label = stringResource(R.string.settings_quiet_hours_start),
                hour = quietHoursStart,
                onChange = { hour ->
                    scope.launch { settingsDataStore.saveQuietHoursStart(hour) }
                },
            )
            QuietHourSlider(
                label = stringResource(R.string.settings_quiet_hours_end),
                hour = quietHoursEnd,
                onChange = { hour ->
                    scope.launch { settingsDataStore.saveQuietHoursEnd(hour) }
                },
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val tc = LocalTruckColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = tc.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f).padding(end = 12.dp),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = AppSwitchDefaults.colors(),
        )
    }
}

@Composable
private fun QuietHourSlider(
    label: String,
    hour: Int,
    onChange: (Int) -> Unit,
) {
    val tc = LocalTruckColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, color = tc.TextPrimary, style = MaterialTheme.typography.bodySmall)
            Text(
                text = String.format(Locale.US, "%02d:00", hour),
                color = tc.AccentPrimary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Slider(
            value = hour.toFloat(),
            onValueChange = { onChange(it.toInt().coerceIn(0, 23)) },
            valueRange = 0f..23f,
            steps = 22,
        )
    }
}

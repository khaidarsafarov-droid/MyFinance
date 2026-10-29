package com.truckerload.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.sync.MaintenanceReminderCoordinator
import com.truckerload.presentation.di.LocalSettingsDataStore
import com.truckerload.presentation.theme.LocalTruckColors
import kotlinx.coroutines.launch

@Composable
fun NotificationSettingsSection(
    notifyMissingWeek: Boolean,
    notifyMaintenance: Boolean,
    modifier: Modifier = Modifier,
) {
    val settingsDataStore = LocalSettingsDataStore.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(modifier = modifier) {
        SettingsSwitchRow(
            icon = com.truckerload.presentation.icons.AppIcons.Notifications,
            title = stringResource(R.string.settings_notify_missing_week),
            subtitle = stringResource(R.string.settings_notify_missing_subtitle),
            checked = notifyMissingWeek,
            onCheckedChange = { enabled ->
                scope.launch { settingsDataStore.saveNotifyMissingWeek(enabled) }
            },
        )
        SettingsSwitchRow(
            icon = com.truckerload.presentation.icons.AppIcons.Build,
            title = stringResource(R.string.settings_notify_maintenance),
            subtitle = stringResource(R.string.settings_notify_maint_subtitle),
            checked = notifyMaintenance,
            onCheckedChange = { enabled ->
                scope.launch {
                    settingsDataStore.saveNotifyMaintenance(enabled)
                    MaintenanceReminderCoordinator.sync(context)
                }
            },
        )
    }
}

@Composable
internal fun QuietHoursEditor(
    quietHoursEnabled: Boolean,
    quietHoursStart: Int,
    quietHoursEnd: Int,
) {
    val settingsDataStore = LocalSettingsDataStore.current
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        SettingsSwitchRow(
            icon = com.truckerload.presentation.icons.AppIcons.Schedule,
            title = stringResource(R.string.settings_quiet_hours_title),
            subtitle = stringResource(R.string.settings_quiet_hours_desc),
            checked = quietHoursEnabled,
            onCheckedChange = { enabled ->
                scope.launch { settingsDataStore.saveQuietHoursEnabled(enabled) }
            },
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
                text = SettingsPremiumState.hourLabel(hour),
                color = tc.AccentPrimary,
                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
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

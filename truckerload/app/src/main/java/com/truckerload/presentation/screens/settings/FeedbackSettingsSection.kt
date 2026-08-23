package com.truckerload.presentation.screens.settings

import com.truckerload.presentation.icons.AppIcons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.theme.AppSwitchDefaults
import com.truckerload.presentation.theme.BentoGlassSection
import com.truckerload.presentation.theme.LocalTruckColors

@Composable
fun FeedbackSettingsSection(
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
    var soundEnabled by remember { mutableStateOf(settingsViewModel.isSoundEnabled()) }
    var vibrationEnabled by remember { mutableStateOf(settingsViewModel.isVibrationEnabled()) }
    BentoGlassSection(
        title = stringResource(R.string.settings_feedback_title),
        modifier = modifier,
    ) {
        SettingsToggleRow(
            icon = {
                Icon(
                    AppIcons.VolumeUp,
                    contentDescription = stringResource(R.string.settings_sound_title),
                    tint = tc.AccentPrimary,
                    modifier = Modifier.size(22.dp),
                )
            },
            label = stringResource(R.string.settings_sound_title),
            checked = soundEnabled,
            onCheckedChange = {
                soundEnabled = it
                settingsViewModel.setSoundEnabled(it)
            },
        )
        SettingsToggleRow(
            icon = {
                Icon(
                    AppIcons.Vibration,
                    contentDescription = stringResource(R.string.settings_vibration_title),
                    tint = tc.AccentPrimary,
                    modifier = Modifier.size(22.dp),
                )
            },
            label = stringResource(R.string.settings_vibration_title),
            checked = vibrationEnabled,
            onCheckedChange = {
                vibrationEnabled = it
                settingsViewModel.setVibrationEnabled(it)
            },
        )
    }
}

@Composable
internal fun SettingsToggleRow(
    icon: @Composable () -> Unit,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            icon()
            Text(label, color = tc.TextPrimary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = AppSwitchDefaults.colors(),
        )
    }
}

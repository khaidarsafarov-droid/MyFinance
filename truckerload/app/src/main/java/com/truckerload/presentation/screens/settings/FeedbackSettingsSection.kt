package com.truckerload.presentation.screens.settings

import com.truckerload.presentation.icons.AppIcons

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.truckerload.R

@Composable
fun FeedbackSettingsSection(
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    var soundEnabled by remember { mutableStateOf(settingsViewModel.isSoundEnabled()) }
    var vibrationEnabled by remember { mutableStateOf(settingsViewModel.isVibrationEnabled()) }
    Column(modifier = modifier) {
        SettingsSwitchRow(
            icon = AppIcons.Speaker,
            title = stringResource(R.string.settings_sound_title),
            subtitle = stringResource(R.string.settings_sound_subtitle),
            checked = soundEnabled,
            onCheckedChange = {
                soundEnabled = it
                settingsViewModel.setSoundEnabled(it)
            },
        )
        SettingsSwitchRow(
            icon = AppIcons.PhoneVibrate,
            title = stringResource(R.string.settings_vibration_title),
            subtitle = stringResource(R.string.settings_vibration_subtitle),
            checked = vibrationEnabled,
            onCheckedChange = {
                vibrationEnabled = it
                settingsViewModel.setVibrationEnabled(it)
            },
        )
    }
}

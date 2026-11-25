package com.truckerload.presentation.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.truckerload.R
import com.truckerload.presentation.icons.AppIcons

@Composable
fun AccessibilitySettingsSection(reduceMotion: Boolean, modifier: Modifier = Modifier) {
    val prefs: SettingsPreferencesViewModel = hiltViewModel()

    SettingsSwitchRow(
        icon = AppIcons.AccessibilityNew,
        title = stringResource(R.string.settings_reduce_motion_title),
        subtitle = stringResource(R.string.settings_motion_subtitle),
        checked = reduceMotion,
        onCheckedChange = prefs::setReduceMotion,
        modifier = modifier,
    )
}

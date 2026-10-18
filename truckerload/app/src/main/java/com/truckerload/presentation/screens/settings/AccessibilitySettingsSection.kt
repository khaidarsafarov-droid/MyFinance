package com.truckerload.presentation.screens.settings

import com.truckerload.presentation.icons.AppIcons

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.truckerload.R
import com.truckerload.presentation.di.LocalSettingsDataStore
import kotlinx.coroutines.launch

@Composable
fun AccessibilitySettingsSection(
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val settingsDataStore = LocalSettingsDataStore.current
    val scope = rememberCoroutineScope()

    SettingsSwitchRow(
        icon = AppIcons.AccessibilityNew,
        title = stringResource(R.string.settings_reduce_motion_title),
        subtitle = stringResource(R.string.settings_motion_subtitle),
        checked = reduceMotion,
        onCheckedChange = { enabled ->
            scope.launch { settingsDataStore.saveReduceMotion(enabled) }
        },
        modifier = modifier,
    )
}

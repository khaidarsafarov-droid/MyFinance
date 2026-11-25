package com.truckerload.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.truckerload.R
import com.truckerload.data.preferences.AppThemeMode
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.AppFilterChip
import com.truckerload.presentation.theme.LocalBrandPalette

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemeSettingsSection(selected: AppThemeMode, oledDark: Boolean, dynamicColor: Boolean, modifier: Modifier = Modifier) {
    val brand = LocalBrandPalette.current
    val prefs: SettingsPreferencesViewModel = hiltViewModel()
    val darkActive = selected == AppThemeMode.DARK || selected == AppThemeMode.SYSTEM
    val showDynamicColor = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsIconTile(icon = AppIcons.Palette, danger = false)
            Text(
                text = stringResource(R.string.settings_theme_title),
                color = brand.textPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        FlowRow(
            modifier = Modifier.padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppThemeMode.entries.forEach { mode ->
                AppFilterChip(
                    selected = selected == mode,
                    onClick = { prefs.setThemeMode(mode) },
                    leadingIcon = {
                        Icon(
                            imageVector = when (mode) {
                                AppThemeMode.SYSTEM -> AppIcons.BrightnessAuto
                                AppThemeMode.LIGHT -> AppIcons.LightMode
                                AppThemeMode.DARK -> AppIcons.DarkMode
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    label = {
                        Text(
                            when (mode) {
                                AppThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                                AppThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                                AppThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                            },
                            maxLines = 1,
                            softWrap = false,
                        )
                    },
                )
            }
        }
        if (showDynamicColor) {
            SettingsSwitchRow(
                icon = AppIcons.Palette,
                title = stringResource(R.string.settings_dynamic_color_title),
                subtitle = stringResource(R.string.settings_dynamic_color_desc),
                checked = dynamicColor,
                onCheckedChange = prefs::setDynamicColor,
            )
        }
        if (darkActive) {
            SettingsSwitchRow(
                icon = AppIcons.DarkMode,
                title = stringResource(R.string.settings_oled_dark_title),
                subtitle = stringResource(R.string.settings_oled_subtitle),
                checked = oledDark,
                onCheckedChange = prefs::setOledDark,
            )
        }
    }
}

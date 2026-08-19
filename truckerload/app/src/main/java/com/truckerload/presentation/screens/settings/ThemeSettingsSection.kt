package com.truckerload.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.data.preferences.AppThemeMode
import com.truckerload.presentation.di.LocalSettingsDataStore
import com.truckerload.presentation.theme.AppSwitchDefaults
import com.truckerload.presentation.theme.BentoGlassSection
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.ThemeManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemeSettingsSection(
    selected: AppThemeMode,
    oledDark: Boolean,
    dynamicColor: Boolean,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
    val settingsDataStore = LocalSettingsDataStore.current
    val scope = rememberCoroutineScope()
    val darkActive = selected == AppThemeMode.DARK || selected == AppThemeMode.SYSTEM
    val showDynamicColor = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S

    BentoGlassSection(
        title = stringResource(R.string.settings_theme_title),
        modifier = modifier,
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = selected == mode,
                    onClick = {
                        scope.launch {
                            settingsDataStore.saveThemeMode(mode)
                            ThemeManager.apply(mode)
                        }
                    },
                    label = {
                        Text(
                            when (mode) {
                                AppThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                                AppThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                                AppThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = tc.AccentPrimary.copy(alpha = 0.22f),
                        selectedLabelColor = tc.AccentPrimary,
                        containerColor = tc.SurfaceSecondary,
                        labelColor = tc.TextSecondary,
                    ),
                )
            }
        }
        if (showDynamicColor) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.settings_dynamic_color_title),
                    color = tc.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Switch(
                    checked = dynamicColor,
                    onCheckedChange = { enabled ->
                        scope.launch { settingsDataStore.saveDynamicColor(enabled) }
                    },
                    colors = AppSwitchDefaults.colors(),
                )
            }
            Text(
                text = stringResource(R.string.settings_dynamic_color_desc),
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
            )
        }
        if (darkActive) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        Icons.Default.DarkMode,
                        contentDescription = stringResource(R.string.settings_oled_dark_title),
                        tint = tc.AccentPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = stringResource(R.string.settings_oled_dark_title),
                        color = tc.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Switch(
                    checked = oledDark,
                    onCheckedChange = { enabled ->
                        scope.launch { settingsDataStore.saveOledDark(enabled) }
                    },
                    colors = AppSwitchDefaults.colors(),
                )
            }
        }
    }
}

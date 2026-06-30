package com.truckerload.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.data.preferences.AppThemeMode
import com.truckerload.presentation.di.LocalSettingsDataStore
import com.truckerload.presentation.theme.BentoGlassSection
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.ThemeManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemeSettingsSection(
    selected: AppThemeMode,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
    val settingsDataStore = LocalSettingsDataStore.current
    val scope = rememberCoroutineScope()

    BentoGlassSection(
        title = stringResource(R.string.settings_theme_title),
        subtitle = stringResource(R.string.settings_theme_desc),
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
    }
}

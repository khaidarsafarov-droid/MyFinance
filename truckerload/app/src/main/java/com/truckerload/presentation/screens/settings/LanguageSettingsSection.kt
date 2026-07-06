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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.data.preferences.AppLanguage
import com.truckerload.presentation.di.LocalSettingsDataStore
import com.truckerload.presentation.theme.BentoGlassSection
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.utils.AppLocale
import com.truckerload.widget.WidgetUpdateWorker
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LanguageSettingsSection(
    selected: AppLanguage,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
    val settingsDataStore = LocalSettingsDataStore.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    BentoGlassSection(
        title = stringResource(R.string.settings_language_title),
        subtitle = stringResource(R.string.settings_language_desc),
        modifier = modifier,
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppLanguage.entries.forEach { language ->
                FilterChip(
                    selected = selected == language,
                    onClick = {
                        if (language == selected) return@FilterChip
                        scope.launch {
                            settingsDataStore.saveLanguage(language)
                            AppLocale.applyAndRecreate(context, language)
                            WidgetUpdateWorker.refreshNow(context.applicationContext)
                        }
                    },
                    label = {
                        Text(
                            when (language) {
                                AppLanguage.RU -> stringResource(R.string.settings_language_ru)
                                AppLanguage.EN -> stringResource(R.string.settings_language_en)
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

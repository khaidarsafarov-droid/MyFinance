package com.truckerload.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.week.WeekStartDay
import com.truckerload.domain.week.WeekStartRebinder
import com.truckerload.domain.week.WeekStartRuntime
import com.truckerload.presentation.di.LocalSettingsDataStore
import com.truckerload.presentation.theme.BentoGlassSection
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.utils.BackupService
import com.truckerload.widget.WidgetUpdateWorker
import java.text.DateFormatSymbols
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeekStartSettingsSection(
    loadsStart: WeekStartDay,
    dieselStart: WeekStartDay,
    modifier: Modifier = Modifier,
) {
    val settingsDataStore = LocalSettingsDataStore.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val labels = remember {
        val names = DateFormatSymbols(Locale.getDefault()).shortWeekdays
        WeekStartDay.entries.associateWith { day ->
            names.getOrNull(day.calendarDay).orEmpty().replace(".", "").ifBlank { day.name }
        }
    }

    BentoGlassSection(
        title = stringResource(R.string.settings_week_start_title),
        subtitle = stringResource(R.string.settings_week_start_desc),
        modifier = modifier,
    ) {
            WeekStartChipRow(
                title = stringResource(R.string.settings_week_start_loads),
                selected = loadsStart,
                labels = labels,
                onSelect = { day ->
                    if (day == loadsStart) return@WeekStartChipRow
                    scope.launch {
                        WeekStartRuntime.installLoads(day)
                        settingsDataStore.saveLoadWeekStartDay(day)
                        withContext(Dispatchers.IO) {
                            WeekStartRebinder.rebindIfPossible(context)
                            BackupService.scheduleCreateAutoBackup(context)
                        }
                        WidgetUpdateWorker.refreshNow(context)
                    }
                },
            )
            WeekStartChipRow(
                title = stringResource(R.string.settings_week_start_diesel),
                selected = dieselStart,
                labels = labels,
                onSelect = { day ->
                    if (day == dieselStart) return@WeekStartChipRow
                    scope.launch {
                        WeekStartRuntime.installDiesel(day)
                        settingsDataStore.saveDieselWeekStartDay(day)
                        withContext(Dispatchers.IO) {
                            WeekStartRebinder.rebindIfPossible(context)
                            BackupService.scheduleCreateAutoBackup(context)
                        }
                        WidgetUpdateWorker.refreshNow(context)
                    }
                },
            )
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeekStartChipRow(
    title: String,
    selected: WeekStartDay,
    labels: Map<WeekStartDay, String>,
    onSelect: (WeekStartDay) -> Unit,
) {
    val tc = LocalTruckColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = tc.TextPrimary,
            modifier = Modifier.padding(top = 4.dp),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WeekStartDay.entries.forEach { day ->
                FilterChip(
                    selected = selected == day,
                    onClick = { onSelect(day) },
                    label = {
                        Text(
                            labels[day].orEmpty(),
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

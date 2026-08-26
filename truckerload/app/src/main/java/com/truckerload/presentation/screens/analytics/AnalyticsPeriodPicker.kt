package com.truckerload.presentation.screens.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.model.analytics.AnalyticsFilter
import com.truckerload.domain.model.analytics.AnalyticsPeriod
import com.truckerload.presentation.theme.AppFilterChipDefaults
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.utils.availableAnalyticsMonths
import com.truckerload.utils.availableAnalyticsWeeks
import com.truckerload.utils.availableAnalyticsYears
import com.truckerload.utils.getWeekLabelShort
import java.text.DateFormatSymbols
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AnalyticsPeriodPicker(
    filter: AnalyticsFilter,
    onSelectPreset: (AnalyticsPeriod) -> Unit,
    onSelectYear: (Int) -> Unit,
    onSelectMonth: (Int) -> Unit,
    onSelectWeek: (weekNumber: Int, weekYear: Int) -> Unit,
) {
    val today = remember { LocalDate.now() }
    val years = remember(today) { availableAnalyticsYears(today) }
    val months = remember(filter.year, today) {
        filter.year?.let { availableAnalyticsMonths(it, today) }.orEmpty()
    }
    val weeks = remember(filter.year, filter.month, today) {
        val year = filter.year
        val month = filter.month
        if (year != null && month != null) availableAnalyticsWeeks(year, month, today) else emptyList()
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChipRow {
            AnalyticsPeriod.entries.forEach { period ->
                FilterChip(
                    selected = !filter.isCalendar && filter.preset == period,
                    onClick = { onSelectPreset(period) },
                    label = {
                        Text(
                            when (period) {
                                AnalyticsPeriod.LAST_12_WEEKS ->
                                    stringResource(R.string.analytics_period_12_weeks)
                                AnalyticsPeriod.LAST_6_MONTHS ->
                                    stringResource(R.string.analytics_period_6_months)
                                AnalyticsPeriod.ALL_TIME ->
                                    stringResource(R.string.analytics_period_all)
                            },
                        )
                    },
                    colors = AppFilterChipDefaults.colors(),
                )
            }
        }

        FilterSection(title = stringResource(R.string.analytics_filter_years)) {
            ChipRow {
                years.forEach { year ->
                    FilterChip(
                        selected = filter.year == year,
                        onClick = { onSelectYear(year) },
                        label = { Text(year.toString()) },
                        colors = AppFilterChipDefaults.colors(),
                    )
                }
            }
        }

        if (filter.year != null && months.isNotEmpty()) {
            FilterSection(title = stringResource(R.string.analytics_filter_months)) {
                ChipRow {
                    months.forEach { month ->
                        FilterChip(
                            selected = filter.month == month,
                            onClick = { onSelectMonth(month) },
                            label = { Text(monthChipLabel(month)) },
                            colors = AppFilterChipDefaults.colors(),
                        )
                    }
                }
            }
        }

        if (filter.month != null && weeks.isNotEmpty()) {
            FilterSection(title = stringResource(R.string.analytics_filter_weeks)) {
                ChipRow {
                    weeks.forEach { (weekNumber, weekYear) ->
                        FilterChip(
                            selected = filter.weekNumber == weekNumber &&
                                filter.weekYear == weekYear,
                            onClick = { onSelectWeek(weekNumber, weekYear) },
                            label = { Text(getWeekLabelShort(weekNumber, weekYear)) },
                            colors = AppFilterChipDefaults.colors(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    content: @Composable () -> Unit,
) {
    val tc = LocalTruckColors.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = tc.TextSecondary,
            modifier = Modifier.padding(start = 2.dp),
        )
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = { content() },
    )
}

private fun monthChipLabel(month: Int): String {
    val short = DateFormatSymbols(Locale.getDefault())
        .shortMonths
        .getOrNull((month - 1).coerceIn(0, 11))
        .orEmpty()
        .replace(".", "")
    return short.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
}

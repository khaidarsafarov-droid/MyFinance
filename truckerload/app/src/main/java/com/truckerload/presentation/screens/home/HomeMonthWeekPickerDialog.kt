package com.truckerload.presentation.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.AppFilterChipDefaults
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.utils.getWeekLabelShort
import com.truckerload.utils.getWeeksInMonth
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

/**
 * Pick a calendar month for Home totals, then optionally drill into
 * trucking weeks that end in that month (typically 4–5).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HomeMonthWeekPickerDialog(
    initialYear: Int,
    initialMonth: Int,
    selectedWeekStart: String?,
    onSelectMonth: (year: Int, month: Int) -> Unit,
    onSelectWeek: (year: Int, month: Int, weekNumber: Int, weekYear: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val tc = LocalTruckColors.current
    val today = remember { Calendar.getInstance() }
    val maxYear = today.get(Calendar.YEAR)
    val maxMonth = today.get(Calendar.MONTH) + 1
    val minYear = maxYear - 2

    var year by remember(initialYear) {
        mutableIntStateOf(initialYear.coerceIn(minYear, maxYear))
    }
    var month by remember(initialMonth, initialYear) {
        mutableIntStateOf(initialMonth.coerceIn(1, 12))
    }

    val months = remember(year, maxYear, maxMonth) {
        val last = if (year == maxYear) maxMonth else 12
        (1..last).toList()
    }
    if (month !in months) {
        month = months.last()
    }

    val weeks = remember(year, month) { getWeeksInMonth(year, month) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tc.CardBackground,
        titleContentColor = tc.TextPrimary,
        textContentColor = tc.TextPrimary,
        title = { Text(stringResource(R.string.home_month_picker_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(
                        onClick = { if (year > minYear) year -= 1 },
                        enabled = year > minYear,
                    ) {
                        Icon(AppIcons.ChevronLeft, contentDescription = null, tint = tc.TextPrimary)
                    }
                    Text(
                        text = year.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = tc.TextPrimary,
                    )
                    IconButton(
                        onClick = { if (year < maxYear) year += 1 },
                        enabled = year < maxYear,
                    ) {
                        Icon(AppIcons.ChevronRight, contentDescription = null, tint = tc.TextPrimary)
                    }
                }

                Text(
                    text = stringResource(R.string.home_month_picker_months),
                    style = MaterialTheme.typography.labelMedium,
                    color = tc.TextSecondary,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    months.forEach { m ->
                        FilterChip(
                            selected = month == m,
                            onClick = { month = m },
                            label = { Text(monthShortLabel(m)) },
                            colors = AppFilterChipDefaults.colors(),
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.home_month_picker_weeks),
                    style = MaterialTheme.typography.labelMedium,
                    color = tc.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
                FilterChip(
                    selected = selectedWeekStart.isNullOrBlank(),
                    onClick = {
                        onSelectMonth(year, month)
                        onDismiss()
                    },
                    label = { Text(stringResource(R.string.home_month_whole)) },
                    colors = AppFilterChipDefaults.colors(),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    weeks.forEachIndexed { index, (weekNumber, weekYear) ->
                        val label = stringResource(
                            R.string.home_month_week_n,
                            index + 1,
                            getWeekLabelShort(weekNumber, weekYear),
                        )
                        FilterChip(
                            selected = false,
                            onClick = {
                                onSelectWeek(year, month, weekNumber, weekYear)
                                onDismiss()
                            },
                            label = { Text(label) },
                            colors = AppFilterChipDefaults.colors(),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_close), color = tc.AccentPrimary)
            }
        },
    )
}

private fun monthShortLabel(month: Int): String {
    val short = DateFormatSymbols(Locale.getDefault())
        .shortMonths
        .getOrNull((month - 1).coerceIn(0, 11))
        .orEmpty()
        .replace(".", "")
    return short.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
}

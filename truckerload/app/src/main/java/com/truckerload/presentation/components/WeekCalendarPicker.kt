package com.truckerload.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.model.WeekSummary
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.utils.getWeekLabelShort
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

@Composable
fun WeekCalendarPicker(
    selectedMonth: Int,
    selectedYear: Int,
    weeksInMonth: List<WeekSummary>,
    selectedWeekNumber: Int,
    selectedWeekYear: Int,
    onMonthYearChange: (month: Int, year: Int) -> Unit,
    onWeekSelect: (weekNumber: Int, year: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tc = LocalTruckColors.current
    var showMonthYearMenu by remember { mutableStateOf(false) }
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showMonthYearMenu = true }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = tc.AccentPrimary)
                Text(
                    text = stringResource(
                        R.string.finance_month_year_format,
                        monthLongLabel(selectedMonth),
                        selectedYear
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = tc.TextPrimary,
                    modifier = Modifier.padding(start = 8.dp)
                )
                Icon(
                    if (showMonthYearMenu) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = tc.TextSecondary
                )
            }
        }

        DropdownMenu(
            expanded = showMonthYearMenu,
            onDismissRequest = { showMonthYearMenu = false }
        ) {
            (1..12).forEach { monthVal ->
                val isSelected = selectedYear == currentYear && selectedMonth == monthVal
                DropdownMenuItem(
                    modifier = Modifier
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .background(
                            if (isSelected) tc.AccentPrimary.copy(alpha = 0.22f) else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        ),
                    text = {
                        Text(
                            text = stringResource(
                                R.string.finance_month_year_format,
                                monthLongLabel(monthVal),
                                currentYear
                            ),
                            color = if (isSelected) tc.AccentPrimary else tc.TextPrimary,
                            style = MaterialTheme.typography.titleSmall
                        )
                    },
                    onClick = {
                        onMonthYearChange(monthVal, currentYear)
                        showMonthYearMenu = false
                    }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            weeksInMonth.forEach { summary ->
                val isSelected = summary.weekNumber == selectedWeekNumber && summary.year == selectedWeekYear
                Card(
                    modifier = Modifier
                        .clickable { onWeekSelect(summary.weekNumber, summary.year) }
                        .padding(4.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) tc.AccentPrimary.copy(alpha = 0.2f) else tc.CardBackground
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = getWeekLabelShort(summary.weekNumber, summary.year),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) tc.AccentPrimary else tc.TextPrimary
                        )
                        Text(
                            text = stringResource(
                                R.string.finance_week_summary_short,
                                summary.loadsCount,
                                String.format(Locale.getDefault(), "%,.0f", summary.totalLoadRate)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = tc.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

private fun monthLongLabel(month: Int): String {
    val long = DateFormatSymbols(Locale.getDefault())
        .months
        .getOrNull((month - 1).coerceIn(0, 11))
        .orEmpty()
    return long.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

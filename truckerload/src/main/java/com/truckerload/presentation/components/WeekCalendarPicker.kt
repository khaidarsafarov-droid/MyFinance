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
import androidx.compose.ui.unit.dp
import com.truckerload.domain.model.WeekSummary
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.utils.getWeekLabelShort
import com.truckerload.utils.getWeeksInMonth
import java.util.Calendar

private val MONTH_NAMES = listOf(
    "", "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
)

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
                    text = "${MONTH_NAMES.getOrElse(selectedMonth) { "" }} $selectedYear",
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
            val cal = Calendar.getInstance()
            var y = cal.get(Calendar.YEAR)
            var m = cal.get(Calendar.MONTH) + 1
            repeat(24) {
                DropdownMenuItem(
                    text = { Text("${MONTH_NAMES[m]} $y") },
                    onClick = {
                        onMonthYearChange(m, y)
                        showMonthYearMenu = false
                    }
                )
                m--
                if (m < 1) { m = 12; y-- }
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
                            text = "${summary.loadsCount} грузов • $${String.format("%,.0f", summary.totalLoadRate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = tc.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

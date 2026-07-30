package com.truckerload.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.theme.LocalTruckColors
import java.util.Calendar
import java.text.DateFormatSymbols
import java.util.Locale
import java.text.SimpleDateFormat

@Composable
fun LoadCalendarWithDots(
    year: Int,
    month: Int,
    datesWithLoads: Set<String>,
    selectedDate: String?,
    onDateSelect: (String) -> Unit,
    onMonthChange: (year: Int, month: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tc = LocalTruckColors.current

    // Trucking week is always Sunday–Saturday (matches WeekUtils / "Select week").
    val firstDayOfWeek = Calendar.SUNDAY
    val cal = Calendar.getInstance(Locale.US).apply {
        this.firstDayOfWeek = firstDayOfWeek
        minimalDaysInFirstWeek = 1
        set(year, month - 1, 1)
    }
    var offset = (cal.get(Calendar.DAY_OF_WEEK) - firstDayOfWeek + 7) % 7
    cal.add(Calendar.DAY_OF_YEAR, -offset)

    val daysInMonth = Calendar.getInstance(Locale.US).apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, 1)
    }.getActualMaximum(Calendar.DAY_OF_MONTH)

    val totalCells = ((daysInMonth + offset + 6) / 7) * 7
    val dayCells = mutableListOf<Triple<Int, Int, String?>>()
    repeat(totalCells) { i ->
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val dateStr = "%04d-%02d-%02d".format(y, m, d)
        dayCells.add(Triple(d, if (m == month) 1 else 0, dateStr))
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                val (ny, nm) = if (month == 1) Pair(year - 1, 12) else Pair(year, month - 1)
                onMonthChange(ny, nm)
            }) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = stringResource(R.string.calendar_prev_month_cd),
                    tint = tc.TextPrimary
                )
            }
            Text(
                text = stringResource(
                    R.string.finance_month_year_format,
                    monthLongLabel(month),
                    year
                ),
                style = MaterialTheme.typography.titleMedium,
                color = tc.TextPrimary
            )
            IconButton(onClick = {
                val (ny, nm) = if (month == 12) Pair(year + 1, 1) else Pair(year, month + 1)
                onMonthChange(ny, nm)
            }) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = stringResource(R.string.calendar_next_month_cd),
                    tint = tc.TextPrimary
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            weekdayHeaders(firstDayOfWeek).forEach { h ->
                Text(
                    text = h,
                    style = MaterialTheme.typography.labelSmall,
                    color = tc.TextSecondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        dayCells.chunked(7).forEach { chunk ->
            val week = chunk + List(7 - chunk.size) { Triple(0, 0, null as String?) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                week.forEach { (day, inMonth, dateStr) ->
                    val isCurrentMonth = inMonth == 1
                    val hasLoad = isCurrentMonth && dateStr != null && dateStr in datesWithLoads
                    val isSelected = dateStr == selectedDate

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(2.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(
                                when {
                                    isSelected -> tc.AccentPrimary.copy(alpha = 0.3f)
                                    hasLoad && isCurrentMonth -> tc.AccentPrimary.copy(alpha = 0.15f)
                                    else -> Color.Transparent
                                }
                            )
                            .clickable(enabled = dateStr != null) {
                                dateStr?.let { onDateSelect(it) }
                            }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (day > 0) "$day" else "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = when {
                                    !isCurrentMonth -> tc.TextSecondary.copy(alpha = 0.5f)
                                    isSelected -> tc.AccentPrimary
                                    else -> tc.TextPrimary
                                }
                            )
                            if (hasLoad && isCurrentMonth) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(tc.AccentPrimary)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Stand-alone month name (nominative in Russian: «Август», not genitive «Августа»). */
private fun monthLongLabel(month: Int): String {
    val locale = Locale.getDefault()
    val cal = Calendar.getInstance(locale).apply {
        set(Calendar.MONTH, (month - 1).coerceIn(0, 11))
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val raw = SimpleDateFormat("LLLL", locale).format(cal.time)
    return raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}

private fun weekdayHeaders(firstDayOfWeek: Int): List<String> {
    val short = DateFormatSymbols(Locale.getDefault()).shortWeekdays
    val sequence = buildList {
        var day = firstDayOfWeek
        repeat(7) {
            add(short.getOrNull(day).orEmpty().replace(".", ""))
            day++
            if (day > Calendar.SATURDAY) day = Calendar.SUNDAY
        }
    }
    return sequence
}

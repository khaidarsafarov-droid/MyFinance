package com.truckerload.presentation.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.presentation.components.LoadCalendarWithDots
import com.truckerload.presentation.components.TlButton
import com.truckerload.presentation.components.TlTextButton as TextButton
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.utils.getWeekNumberAndYearFromDate
import com.truckerload.utils.getWeekRange
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.defaultMinSize

/** Calendar dialog collects [HomeViewModel.calendarDatesWithLoads] only while visible. */
@Composable
internal fun HomeCalendarDialog(
    viewModel: HomeViewModel,
    calendarYear: Int,
    calendarMonth: Int,
    selectedDate: String?,
    onYearMonthChange: (Int, Int) -> Unit,
    onDateSelect: (String) -> Unit,
    onWeekSelect: (start: String, end: String, label: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val tc = LocalTruckColors.current
    val datesWithLoads by viewModel.calendarDatesWithLoads.collectAsStateWithLifecycle()
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = tc.CardBackground,
            modifier = Modifier.fillMaxWidth(0.96f),
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    stringResource(R.string.home_calendar_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = tc.TextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                LoadCalendarWithDots(
                    year = calendarYear,
                    month = calendarMonth,
                    datesWithLoads = datesWithLoads,
                    selectedDate = selectedDate,
                    onDateSelect = onDateSelect,
                    onMonthChange = onYearMonthChange,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = {
                            val date = selectedDate
                                ?: java.time.LocalDate.of(calendarYear, calendarMonth, 1).toString()
                            val (week, year) = getWeekNumberAndYearFromDate(date)
                            val (start, end, label) = getWeekRange(week, year)
                            onWeekSelect(start, end, label)
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stringResource(R.string.home_calendar_select_week),
                            color = tc.TextPrimary,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                    TlButton(
                        onClick = onDismiss,
                        modifier = Modifier.defaultMinSize(minWidth = 96.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.common_close),
                            maxLines = 1,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

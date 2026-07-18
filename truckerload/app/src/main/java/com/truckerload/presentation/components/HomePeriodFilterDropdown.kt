package com.truckerload.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.filter.LoadFilter
import com.truckerload.presentation.theme.AppTextFieldDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePeriodFilterDropdown(
    currentFilter: LoadFilter,
    selectedYear: Int?,
    selectedDateLabel: String,
    selectedWeekLabel: String,
    onFilterSelected: (LoadFilter) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenArchive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    val displayLabel = when {
        currentFilter == LoadFilter.CALENDAR_DATE && selectedDateLabel.isNotBlank() -> selectedDateLabel
        currentFilter == LoadFilter.CALENDAR_WEEK && selectedWeekLabel.isNotBlank() -> selectedWeekLabel
        currentFilter == LoadFilter.ALL && selectedYear != null ->
            stringResource(R.string.home_year_format, selectedYear)
        else -> filterLabel(currentFilter)
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        OutlinedTextField(
            value = displayLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.home_period_filter_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            leadingIcon = {
                IconButton(
                    onClick = onOpenCalendar,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = stringResource(R.string.home_filter_calendar),
                    )
                }
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = AppTextFieldDefaults.outlined(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            periodMenuItem(R.string.home_filter_this_week, LoadFilter.THIS_WEEK) {
                onFilterSelected(LoadFilter.THIS_WEEK)
                expanded = false
            }
            periodMenuItem(R.string.home_filter_yesterday, LoadFilter.YESTERDAY) {
                onFilterSelected(LoadFilter.YESTERDAY)
                expanded = false
            }
            periodMenuItem(R.string.home_filter_last_week, LoadFilter.LAST_WEEK) {
                onFilterSelected(LoadFilter.LAST_WEEK)
                expanded = false
            }
            periodMenuItem(R.string.home_filter_this_month, LoadFilter.THIS_MONTH) {
                onFilterSelected(LoadFilter.THIS_MONTH)
                expanded = false
            }
            periodMenuItem(R.string.home_filter_dispute, LoadFilter.DISPUTE) {
                onFilterSelected(LoadFilter.DISPUTE)
                expanded = false
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.home_filter_calendar)) },
                onClick = {
                    expanded = false
                    onOpenCalendar()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.home_filter_archive)) },
                onClick = {
                    expanded = false
                    onOpenArchive()
                },
            )
        }
    }
}

@Composable
private fun periodMenuItem(
    labelRes: Int,
    filter: LoadFilter,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(stringResource(labelRes)) },
        onClick = onClick,
    )
}

@Composable
private fun filterLabel(filter: LoadFilter): String = when (filter) {
    LoadFilter.THIS_WEEK -> stringResource(R.string.home_filter_this_week)
    LoadFilter.YESTERDAY -> stringResource(R.string.home_filter_yesterday)
    LoadFilter.LAST_WEEK -> stringResource(R.string.home_filter_last_week)
    LoadFilter.THIS_MONTH -> stringResource(R.string.home_filter_this_month)
    LoadFilter.DISPUTE -> stringResource(R.string.home_filter_dispute)
    LoadFilter.CALENDAR_WEEK, LoadFilter.CALENDAR_DATE -> stringResource(R.string.home_filter_calendar)
    LoadFilter.ALL -> stringResource(R.string.home_filter_archive)
}

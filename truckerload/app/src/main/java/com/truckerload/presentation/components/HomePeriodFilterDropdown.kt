package com.truckerload.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
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
import com.truckerload.presentation.screens.home.LoadFilter
import com.truckerload.presentation.theme.LocalTruckColors

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
    val tc = LocalTruckColors.current
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
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        OutlinedTextField(
            value = displayLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.home_period_filter_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            leadingIcon = {
                Icon(Icons.Default.CalendarMonth, contentDescription = null)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                focusedBorderColor = tc.AccentPrimary,
                focusedLabelColor = tc.AccentPrimary,
                cursorColor = tc.AccentPrimary,
            ),
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
    LoadFilter.CALENDAR_WEEK, LoadFilter.CALENDAR_DATE -> stringResource(R.string.home_filter_calendar)
    LoadFilter.ALL -> stringResource(R.string.home_filter_archive)
}

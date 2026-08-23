package com.truckerload.presentation.components

import com.truckerload.presentation.icons.AppIcons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.filter.LoadFilter
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.SoftUiColors

enum class PeriodFilterStyle {
    /** Standalone outlined field (archive / no hero). */
    Field,
    /** Nested pill inside the forest hero card (Gemini canvas). */
    HeroPill,
}

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
    style: PeriodFilterStyle = PeriodFilterStyle.Field,
) {
    var expanded by remember { mutableStateOf(false) }

    val displayLabel = when {
        currentFilter == LoadFilter.CALENDAR_DATE && selectedDateLabel.isNotBlank() -> selectedDateLabel
        currentFilter == LoadFilter.CALENDAR_WEEK && selectedWeekLabel.isNotBlank() -> selectedWeekLabel
        currentFilter == LoadFilter.ALL && selectedYear != null ->
            stringResource(R.string.home_year_format, selectedYear)
        else -> filterLabel(currentFilter)
    }

    when (style) {
        PeriodFilterStyle.HeroPill -> {
            Column(modifier = modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                        .clickable { expanded = true }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.home_period_filter_label),
                            style = AppTypography.Caption.copy(color = SoftUiColors.ForestSoft),
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp),
                        ) {
                            Icon(
                                AppIcons.CalendarMonth,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = displayLabel,
                                style = AppTypography.Subtitle.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                    }
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    PeriodFilterMenuItems(
                        onFilterSelected = {
                            onFilterSelected(it)
                            expanded = false
                        },
                        onOpenCalendar = {
                            expanded = false
                            onOpenCalendar()
                        },
                        onOpenArchive = {
                            expanded = false
                            onOpenArchive()
                        },
                    )
                }
            }
        }
        PeriodFilterStyle.Field -> {
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
                                AppIcons.CalendarMonth,
                                contentDescription = stringResource(R.string.home_filter_calendar),
                            )
                        }
                    },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    colors = AppTextFieldDefaults.outlined(),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    PeriodFilterMenuItems(
                        onFilterSelected = {
                            onFilterSelected(it)
                            expanded = false
                        },
                        onOpenCalendar = {
                            expanded = false
                            onOpenCalendar()
                        },
                        onOpenArchive = {
                            expanded = false
                            onOpenArchive()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodFilterMenuItems(
    onFilterSelected: (LoadFilter) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenArchive: () -> Unit,
) {
    periodMenuItem(R.string.home_filter_this_week, LoadFilter.THIS_WEEK) {
        onFilterSelected(LoadFilter.THIS_WEEK)
    }
    periodMenuItem(R.string.home_filter_yesterday, LoadFilter.YESTERDAY) {
        onFilterSelected(LoadFilter.YESTERDAY)
    }
    periodMenuItem(R.string.home_filter_last_week, LoadFilter.LAST_WEEK) {
        onFilterSelected(LoadFilter.LAST_WEEK)
    }
    periodMenuItem(R.string.home_filter_this_month, LoadFilter.THIS_MONTH) {
        onFilterSelected(LoadFilter.THIS_MONTH)
    }
    periodMenuItem(R.string.home_filter_dispute, LoadFilter.DISPUTE) {
        onFilterSelected(LoadFilter.DISPUTE)
    }
    DropdownMenuItem(
        text = { Text(stringResource(R.string.home_filter_calendar)) },
        onClick = onOpenCalendar,
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.home_filter_archive)) },
        onClick = onOpenArchive,
    )
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

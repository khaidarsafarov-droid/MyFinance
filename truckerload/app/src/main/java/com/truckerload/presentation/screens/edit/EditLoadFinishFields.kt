package com.truckerload.presentation.screens.edit

import com.truckerload.presentation.icons.AppIcons

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.model.ActualFinishDate
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.utils.dateStringToUtcDatePickerMillis
import com.truckerload.utils.utcDatePickerMillisToDateString
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishDateField(
    value: String,
    lastDelDate: String?,
    onValueChange: (String) -> Unit,
    onDatePicked: (String) -> Unit,
    onTimePicked: (hour: Int, minute: Int) -> Unit,
    fieldColors: TextFieldColors,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    val tc = LocalTruckColors.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val help = if (lastDelDate.isNullOrBlank()) {
        stringResource(R.string.edit_load_finish_help)
    } else {
        stringResource(R.string.edit_load_finish_help_with_del, lastDelDate)
    }
    val datePart = ActualFinishDate.datePart(value).orEmpty()
    val timeParts = ActualFinishDate.timeParts(value)
    val initialHour = timeParts?.first
        ?: Calendar.getInstance(Locale.getDefault()).get(Calendar.HOUR_OF_DAY)
    val initialMinute = timeParts?.second
        ?: Calendar.getInstance(Locale.getDefault()).get(Calendar.MINUTE)

    if (showDatePicker) {
        val year = datePart.take(4).toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = dateStringToUtcDatePickerMillis(datePart)
                ?: System.currentTimeMillis(),
            yearRange = IntRange(year - 2, year + 1),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            colors = androidx.compose.material3.DatePickerDefaults.colors(
                containerColor = tc.CardBackground,
            ),
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { ms ->
                        utcDatePickerMillisToDateString(ms)?.let(onDatePicked)
                    }
                    showDatePicker = false
                    showTimePicker = true
                }) { Text(stringResource(R.string.common_ok)) }
            },
        ) { DatePicker(state = dateState) }
    }
    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor = tc.CardBackground,
            titleContentColor = tc.TextPrimary,
            textContentColor = tc.TextPrimary,
            confirmButton = {
                TextButton(onClick = {
                    onTimePicked(timeState.hour, timeState.minute)
                    showTimePicker = false
                }) { Text(stringResource(R.string.common_ok)) }
            },
            text = { TimePicker(state = timeState) },
        )
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val fieldModifier = if (focusRequester != null) {
            Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        } else {
            Modifier.fillMaxWidth()
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(stringResource(R.string.edit_load_finish_label)) },
            modifier = fieldModifier,
            supportingText = { Text(help) },
            shape = RoundedCornerShape(BentoGlassTheme.CellRadius),
            colors = fieldColors,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true }
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.edit_load_finish_pick_datetime),
                color = tc.TextSecondary,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { showDatePicker = true }) {
                Icon(
                    AppIcons.Edit,
                    contentDescription = stringResource(R.string.edit_load_finish_pick_datetime),
                    tint = tc.TextPrimary,
                )
            }
        }
    }
}

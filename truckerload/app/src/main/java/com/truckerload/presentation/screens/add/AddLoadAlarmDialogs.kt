package com.truckerload.presentation.screens.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.sync.LoadAlarmPlanner
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun AddLoadAlarmOfferDialog(
    tripId: String,
    pickupMillis: Long,
    availablePresets: List<LoadAlarmPlanner.Preset>,
    onPreset: (LoadAlarmPlanner.Preset) -> Unit,
    onCustom: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tc = LocalTruckColors.current
    val pickupLabel = remember(pickupMillis) {
        SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(pickupMillis))
    }
    val message = if (tripId.isNotBlank()) {
        stringResource(R.string.load_alarm_offer_message, tripId, pickupLabel)
    } else {
        stringResource(R.string.load_alarm_offer_message_no_trip, pickupLabel)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tc.CardBackground,
        titleContentColor = tc.TextPrimary,
        textContentColor = tc.TextPrimary,
        title = { Text(stringResource(R.string.load_alarm_offer_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(message)
                availablePresets.forEach { preset ->
                    val label = when (preset) {
                        LoadAlarmPlanner.Preset.TWO_HOURS ->
                            stringResource(R.string.load_alarm_option_2h)
                        LoadAlarmPlanner.Preset.ONE_HOUR ->
                            stringResource(R.string.load_alarm_option_1h)
                    }
                    TextButton(onClick = { onPreset(preset) }, modifier = Modifier.fillMaxWidth()) {
                        Text(label)
                    }
                }
                TextButton(onClick = onCustom, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.load_alarm_option_custom))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.load_alarm_skip))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoadCustomAlarmPickerDialog(
    pickupMillis: Long,
    error: String?,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val tc = LocalTruckColors.current
    val defaultTrigger = remember(pickupMillis) {
        (pickupMillis - LoadAlarmPlanner.HOUR_MS * 2)
            .coerceAtLeast(System.currentTimeMillis() + 60_000L)
            .coerceAtMost(pickupMillis - 60_000L)
    }
    val cal = remember(defaultTrigger) {
        Calendar.getInstance().apply { timeInMillis = defaultTrigger }
    }
    var step by remember { mutableStateOf(0) }
    var selectedDateUtcMillis by remember {
        mutableStateOf(
            dateOnlyUtcMillis(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH),
            ),
        )
    }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = selectedDateUtcMillis)
    val timeState = rememberTimePickerState(
        initialHour = cal.get(Calendar.HOUR_OF_DAY),
        initialMinute = cal.get(Calendar.MINUTE),
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tc.CardBackground,
        titleContentColor = tc.TextPrimary,
        textContentColor = tc.TextPrimary,
        title = {
            Text(
                if (step == 0) {
                    stringResource(R.string.load_alarm_custom_date_title)
                } else {
                    stringResource(R.string.load_alarm_custom_time_title)
                },
            )
        },
        text = {
            Column {
                if (step == 0) DatePicker(state = dateState) else TimePicker(state = timeState)
                error?.let {
                    Text(
                        it,
                        color = tc.AccentExpense,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (step == 0) {
                        selectedDateUtcMillis = dateState.selectedDateMillis ?: selectedDateUtcMillis
                        step = 1
                    } else {
                        onConfirm(
                            combineDateAndTime(selectedDateUtcMillis, timeState.hour, timeState.minute),
                        )
                    }
                },
            ) {
                Text(
                    if (step == 0) {
                        stringResource(R.string.common_ok)
                    } else {
                        stringResource(R.string.load_alarm_set)
                    },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { if (step == 1) step = 0 else onDismiss() }) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

private fun dateOnlyUtcMillis(year: Int, monthZeroBased: Int, day: Int): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(year, monthZeroBased, day, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return utc.timeInMillis
}

private fun combineDateAndTime(dateUtcMillis: Long, hour: Int, minute: Int): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = dateUtcMillis
    }
    val local = Calendar.getInstance().apply {
        set(
            utc.get(Calendar.YEAR),
            utc.get(Calendar.MONTH),
            utc.get(Calendar.DAY_OF_MONTH),
            hour,
            minute,
            0,
        )
        set(Calendar.MILLISECOND, 0)
    }
    return local.timeInMillis
}

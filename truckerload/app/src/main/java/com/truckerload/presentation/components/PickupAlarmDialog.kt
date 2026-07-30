package com.truckerload.presentation.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.components.TlTextButton as TextButton
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.sync.PickupAlarmPlanner
import com.truckerload.sync.PickupAlarmScheduler
import com.truckerload.utils.formatDateTimeForDisplay
import java.util.Calendar

data class PickupAlarmPrompt(
    val loadId: String,
    val tripId: String,
    val pickupMillis: Long,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickupAlarmDialog(
    prompt: PickupAlarmPrompt,
    onDismiss: () -> Unit,
) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val now = System.currentTimeMillis()
    val pickupLabel = formatDateTimeForDisplay(prompt.pickupMillis)
    val twoHoursMs = PickupAlarmPlanner.alarmAtOffset(
        prompt.pickupMillis,
        PickupAlarmPlanner.OFFSET_TWO_HOURS_MS,
        now,
    )
    val oneHourMs = PickupAlarmPlanner.alarmAtOffset(
        prompt.pickupMillis,
        PickupAlarmPlanner.OFFSET_ONE_HOUR_MS,
        now,
    )
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDateMillis by remember { mutableStateOf<Long?>(null) }

    fun scheduleAndDismiss(alarmMillis: Long) {
        val scheduled = PickupAlarmScheduler.schedule(
            context = context,
            loadId = prompt.loadId,
            tripId = prompt.tripId,
            pickupMillis = prompt.pickupMillis,
            alarmMillis = alarmMillis,
        )
        if (scheduled) {
            Toast.makeText(
                context,
                context.getString(R.string.pickup_alarm_scheduled, formatDateTimeForDisplay(alarmMillis)),
                Toast.LENGTH_LONG,
            ).show()
        } else {
            Toast.makeText(context, context.getString(R.string.pickup_alarm_past), Toast.LENGTH_SHORT).show()
        }
        onDismiss()
    }

    if (showDatePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = prompt.pickupMillis }
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = prompt.pickupMillis,
            yearRange = IntRange(cal.get(Calendar.YEAR), cal.get(Calendar.YEAR) + 1),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            colors = androidx.compose.material3.DatePickerDefaults.colors(
                containerColor = tc.CardBackground,
            ),
            confirmButton = {
                TextButton(onClick = {
                    pendingDateMillis = dateState.selectedDateMillis ?: prompt.pickupMillis
                    showDatePicker = false
                    showTimePicker = true
                }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        ) { DatePicker(state = dateState) }
    }

    if (showTimePicker) {
        val baseMillis = pendingDateMillis ?: prompt.pickupMillis
        val cal = Calendar.getInstance().apply { timeInMillis = baseMillis }
        val timeState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor = tc.CardBackground,
            titleContentColor = tc.TextPrimary,
            textContentColor = tc.TextPrimary,
            title = { Text(stringResource(R.string.pickup_alarm_custom_time_title)) },
            confirmButton = {
                TextButton(onClick = {
                    val merged = Calendar.getInstance().apply {
                        timeInMillis = baseMillis
                        set(Calendar.HOUR_OF_DAY, timeState.hour)
                        set(Calendar.MINUTE, timeState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val alarmMillis = PickupAlarmPlanner.alarmAtCustom(merged.timeInMillis, System.currentTimeMillis())
                    showTimePicker = false
                    if (alarmMillis != null) {
                        scheduleAndDismiss(alarmMillis)
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.pickup_alarm_past),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
            text = { TimePicker(state = timeState) },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tc.CardBackground,
        titleContentColor = tc.TextPrimary,
        textContentColor = tc.TextSecondary,
        title = { Text(stringResource(R.string.pickup_alarm_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.pickup_alarm_message, prompt.tripId, pickupLabel),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                twoHoursMs?.let { alarmMillis ->
                    TextButton(
                        onClick = { scheduleAndDismiss(alarmMillis) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.pickup_alarm_two_hours))
                    }
                }
                oneHourMs?.let { alarmMillis ->
                    TextButton(
                        onClick = { scheduleAndDismiss(alarmMillis) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.pickup_alarm_one_hour))
                    }
                }
                TextButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.pickup_alarm_custom))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.pickup_alarm_skip))
            }
        },
    )
}

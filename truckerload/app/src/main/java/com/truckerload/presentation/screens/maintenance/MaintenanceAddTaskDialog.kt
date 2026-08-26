package com.truckerload.presentation.screens.maintenance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.model.MaintenanceReminderType
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlTextButton as TextButton
import com.truckerload.presentation.components.dialogBodyScroll
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.LocalTruckColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddTaskDialog(
    draft: TaskDraft,
    isSaving: Boolean,
    errorKey: String?,
    onDismiss: () -> Unit,
    onChange: ((TaskDraft) -> TaskDraft) -> Unit,
    onSave: () -> Unit,
) {
    val tc = LocalTruckColors.current
    var showStartPicker by remember { mutableStateOf(false) }
    var showDuePicker by remember { mutableStateOf(false) }
    val fieldColors = AppTextFieldDefaults.outlined()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tc.CardBackground,
        title = { Text(stringResource(R.string.maintenance_add_task_title), color = tc.TextPrimary) },
        text = {
            Column(
                modifier = Modifier.dialogBodyScroll(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = draft.title,
                    onValueChange = { value -> onChange { it.copy(title = value) } },
                    label = { Text(stringResource(R.string.maintenance_field_title)) },
                    placeholder = { Text(stringResource(R.string.maintenance_field_title_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = fieldColors,
                )
                Text(
                    text = stringResource(R.string.maintenance_field_date),
                    style = MaterialTheme.typography.labelMedium,
                    color = tc.TextSecondary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = draft.startDate,
                        color = tc.TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { showStartPicker = true }) {
                        Text(stringResource(R.string.common_edit))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = draft.reminderType == MaintenanceReminderType.MILES,
                        onClick = { onChange { it.copy(reminderType = MaintenanceReminderType.MILES) } },
                        label = { Text(stringResource(R.string.maintenance_by_miles)) },
                    )
                    FilterChip(
                        selected = draft.reminderType == MaintenanceReminderType.DATE,
                        onClick = { onChange { it.copy(reminderType = MaintenanceReminderType.DATE) } },
                        label = { Text(stringResource(R.string.maintenance_by_date)) },
                    )
                }
                if (draft.reminderType == MaintenanceReminderType.MILES) {
                    OutlinedTextField(
                        value = draft.intervalMiles,
                        onValueChange = { value -> onChange { it.copy(intervalMiles = value.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }) } },
                        label = { Text(stringResource(R.string.maintenance_interval_miles)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = fieldColors,
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.maintenance_due_date),
                                style = MaterialTheme.typography.labelMedium,
                                color = tc.TextSecondary,
                            )
                            Text(draft.dueDate, color = tc.TextPrimary)
                        }
                        TextButton(onClick = { showDuePicker = true }) {
                            Text(stringResource(R.string.common_edit))
                        }
                    }
                }
                OutlinedTextField(
                    value = draft.odometerAtStart,
                    onValueChange = { value -> onChange { it.copy(odometerAtStart = value.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }) } },
                    label = { Text(stringResource(R.string.maintenance_current_odometer)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = fieldColors,
                )
                errorKey?.let { key ->
                    Text(errorText(key), color = tc.AccentExpense, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave, enabled = !isSaving) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )

    if (showStartPicker) {
        IsoDatePickerDialog(
            initial = draft.startDate,
            onDismiss = { showStartPicker = false },
            onConfirm = { date ->
                onChange { it.copy(startDate = date) }
                showStartPicker = false
            },
        )
    }
    if (showDuePicker) {
        IsoDatePickerDialog(
            initial = draft.dueDate,
            onDismiss = { showDuePicker = false },
            onConfirm = { date ->
                onChange { it.copy(dueDate = date) }
                showDuePicker = false
            },
        )
    }
}

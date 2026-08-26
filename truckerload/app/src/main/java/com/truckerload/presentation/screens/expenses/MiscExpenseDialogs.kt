package com.truckerload.presentation.screens.expenses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.expense.MiscExpenseFields
import com.truckerload.presentation.components.TlButton
import com.truckerload.presentation.components.TlTextButton
import com.truckerload.presentation.components.dialogBodyScroll
import com.truckerload.presentation.screens.add.JournalDatePickerDialog
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.utils.dateStringToUtcDatePickerMillis
import com.truckerload.utils.utcDatePickerMillisToDateString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiscExpenseEditorDialog(
    editor: MiscExpenseEditorState,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onChange: ((MiscExpenseEditorState) -> MiscExpenseEditorState) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    val tc = LocalTruckColors.current
    val fieldColors = AppTextFieldDefaults.outlined()
    var showDatePicker by remember { mutableStateOf(false) }
    val isEdit = editor.id > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tc.CardBackground,
        title = {
            Text(
                text = stringResource(
                    if (isEdit) R.string.misc_expense_edit_title else R.string.misc_expense_add,
                ),
                color = tc.TextPrimary,
            )
        },
        text = {
            Column(
                modifier = Modifier.dialogBodyScroll(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = editor.amountText,
                    onValueChange = { value ->
                        onChange {
                            it.copy(amountText = value.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' })
                        }
                    },
                    label = { Text(stringResource(R.string.misc_expense_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = fieldColors,
                )
                OutlinedTextField(
                    value = editor.description,
                    onValueChange = { value -> onChange { it.copy(description = value) } },
                    label = { Text(stringResource(R.string.misc_expense_description)) },
                    placeholder = { Text(stringResource(R.string.misc_expense_description_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5,
                    colors = fieldColors,
                )
                MiscExpenseReceiptAttachSection(
                    receiptPhotoPath = editor.receiptPhotoPath,
                    initialReceiptPhotoPath = editor.initialReceiptPhotoPath,
                    enabled = !isSaving,
                    onReceiptPathChange = { path ->
                        onChange { it.copy(receiptPhotoPath = path) }
                    },
                )
                OutlinedTextField(
                    value = isoToDisplayDate(editor.dateIso),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.misc_expense_date)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    trailingIcon = {
                        TlTextButton(onClick = { showDatePicker = true }) {
                            Text(stringResource(R.string.common_edit))
                        }
                    },
                    colors = fieldColors,
                )
                editor.error?.let { error ->
                    Text(
                        text = errorText(error),
                        color = tc.AccentExpense,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TlButton(onClick = onSave, enabled = !isSaving) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isEdit) {
                    TlTextButton(onClick = onDelete, enabled = !isSaving) {
                        Text(
                            text = stringResource(R.string.common_delete),
                            color = tc.AccentExpense,
                        )
                    }
                }
                TlTextButton(onClick = onDismiss, enabled = !isSaving) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        },
    )

    if (showDatePicker) {
        val initial = dateStringToUtcDatePickerMillis(editor.dateIso)
            ?: System.currentTimeMillis()
        JournalDatePickerDialog(
            recordedAtMillis = System.currentTimeMillis(),
            initialDateMillis = initial,
            onDismiss = { showDatePicker = false },
            onConfirm = { utcMillis ->
                onChange { it.copy(dateIso = utcDatePickerMillisToDateString(utcMillis)) }
            },
        )
    }
}

@Composable
fun MiscExpenseDeleteConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val tc = LocalTruckColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tc.CardBackground,
        title = {
            Text(stringResource(R.string.misc_expense_delete_title), color = tc.TextPrimary)
        },
        text = {
            Text(stringResource(R.string.misc_expense_delete_message), color = tc.TextSecondary)
        },
        confirmButton = {
            TlButton(onClick = onConfirm) {
                Text(stringResource(R.string.common_delete))
            }
        },
        dismissButton = {
            TlTextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
private fun errorText(error: MiscExpenseFields.Error): String = when (error) {
    MiscExpenseFields.Error.AMOUNT -> stringResource(R.string.misc_expense_error_amount)
    MiscExpenseFields.Error.DESCRIPTION -> stringResource(R.string.misc_expense_error_description)
    MiscExpenseFields.Error.DATE -> stringResource(R.string.misc_expense_error_date)
}

internal fun isoToDisplayDate(iso: String): String {
    if (iso.length < 10) return iso
    return "${iso.substring(8, 10)}.${iso.substring(5, 7)}.${iso.substring(0, 4)}"
}

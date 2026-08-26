package com.truckerload.presentation.screens.paycheck

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.paycheck.PaycheckSalaryFields
import com.truckerload.presentation.components.TlButton
import com.truckerload.presentation.components.TlOutlinedButton
import com.truckerload.presentation.components.TlTextButton
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.utils.formatDateForDisplay

@Composable
fun PaycheckEditorDialog(
    editor: PaycheckEditorState,
    isSaving: Boolean,
    fileAvailable: Boolean,
    onDismiss: () -> Unit,
    onChange: ((PaycheckEditorState) -> PaycheckEditorState) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onOpenFile: () -> Unit,
    onAttachFile: () -> Unit,
) {
    val tc = LocalTruckColors.current
    val fieldColors = AppTextFieldDefaults.outlined()
    val paycheck = editor.paycheck

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        containerColor = tc.CardBackground,
        title = {
            Text(
                text = stringResource(R.string.paycheck_edit_title),
                color = tc.TextPrimary,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = paycheck.weekLabel.ifBlank {
                        stringResource(
                            R.string.paycheck_week_fallback,
                            paycheck.weekNumber,
                            paycheck.year,
                        )
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = tc.TextPrimary,
                )
                Text(
                    text = formatDateForDisplay(paycheck.addedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary,
                )
                paycheck.driverName?.takeIf { it.isNotBlank() }?.let { name ->
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodySmall,
                        color = tc.TextSecondary,
                    )
                }
                Text(
                    text = stringResource(R.string.paycheck_edit_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary,
                )
                OutlinedTextField(
                    value = editor.netText,
                    onValueChange = { value ->
                        onChange {
                            it.copy(netText = value.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' })
                        }
                    },
                    label = { Text(stringResource(R.string.paycheck_net_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = fieldColors,
                )
                val fileLabel = editor.sourceFileName?.takeIf { it.isNotBlank() }
                if (fileLabel != null) {
                    Text(
                        text = fileLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (fileAvailable) tc.AccentPrimary else tc.TextSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (fileAvailable) {
                                    Modifier.clickable(onClick = onOpenFile)
                                } else {
                                    Modifier
                                },
                            ),
                    )
                }
                if (fileAvailable) {
                    TlOutlinedButton(
                        onClick = onOpenFile,
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.paycheck_open_file))
                    }
                } else {
                    Text(
                        text = stringResource(R.string.paycheck_file_missing),
                        style = MaterialTheme.typography.bodySmall,
                        color = tc.TextSecondary,
                    )
                }
                TlOutlinedButton(
                    onClick = onAttachFile,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.paycheck_attach_file))
                }
                editor.error?.let { error ->
                    Text(
                        text = when (error) {
                            PaycheckSalaryFields.Error.NET ->
                                stringResource(R.string.paycheck_error_net)
                        },
                        color = tc.AccentExpense,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (editor.attachFailed) {
                    Text(
                        text = stringResource(R.string.paycheck_attach_failed),
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
                TlTextButton(onClick = onDelete, enabled = !isSaving) {
                    Text(
                        text = stringResource(R.string.common_delete),
                        color = tc.AccentExpense,
                    )
                }
                TlTextButton(onClick = onDismiss, enabled = !isSaving) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        },
    )
}

package com.truckerload.presentation.screens.maintenance

import com.truckerload.presentation.icons.AppIcons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.truckerload.R
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.components.TlTextButton as TextButton
import com.truckerload.presentation.components.dialogBodyScroll
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.LocalTruckColors
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddArchiveDialog(
    draft: ArchiveDraft,
    isSaving: Boolean,
    errorKey: String?,
    onDismiss: () -> Unit,
    onChange: ((ArchiveDraft) -> ArchiveDraft) -> Unit,
    onSave: () -> Unit,
    onScanPhoto: () -> Unit,
) {
    val tc = LocalTruckColors.current
    var showDatePicker by remember { mutableStateOf(false) }
    val fieldColors = AppTextFieldDefaults.outlined()
    val total = String.format(Locale.US, "%,.2f", draft.lineTotal())

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tc.CardBackground,
        title = { Text(stringResource(R.string.maintenance_add_archive_title), color = tc.TextPrimary) },
        text = {
            Column(
                modifier = Modifier.dialogBodyScroll(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.maintenance_edit_receipt_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary,
                )
                if (!draft.photoPath.isNullOrBlank()) {
                    AsyncImage(
                        model = File(draft.photoPath),
                        contentDescription = stringResource(R.string.maintenance_receipt_photo),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop,
                    )
                }
                OutlinedButton(onClick = onScanPhoto, modifier = Modifier.fillMaxWidth()) {
                    Icon(AppIcons.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(
                            if (draft.photoPath.isNullOrBlank()) {
                                R.string.maintenance_scan_receipt
                            } else {
                                R.string.maintenance_retake_photo
                            },
                        ),
                    )
                }
                OutlinedTextField(
                    value = draft.serviceName,
                    onValueChange = { value -> onChange { it.copy(serviceName = value) } },
                    label = { Text(stringResource(R.string.maintenance_archive_service)) },
                    placeholder = { Text(stringResource(R.string.maintenance_archive_service_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = fieldColors,
                )
                OutlinedTextField(
                    value = draft.serviceDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.maintenance_archive_when)) },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        TextButton(onClick = { showDatePicker = true }) {
                            Text(stringResource(R.string.common_edit))
                        }
                    },
                    colors = fieldColors,
                )
                draft.lines.forEachIndexed { index, line ->
                    ServiceLineRow(
                        line = line,
                        showRemove = draft.lines.size > 1,
                        fieldColors = fieldColors,
                        onDescription = { value ->
                            onChange { draft ->
                                draft.copy(lines = draft.lines.mapIndexed { i, item ->
                                    if (i == index) item.copy(description = value) else item
                                })
                            }
                        },
                        onAmount = { value ->
                            onChange { draft ->
                                draft.copy(lines = draft.lines.mapIndexed { i, item ->
                                    if (i == index) {
                                        item.copy(amount = value.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' })
                                    } else {
                                        item
                                    }
                                })
                            }
                        },
                        onRemove = {
                            onChange { draft ->
                                val next = draft.lines.filterIndexed { i, _ -> i != index }
                                draft.copy(lines = next.ifEmpty { listOf(ArchiveLineDraft()) })
                            }
                        },
                    )
                }
                OutlinedButton(
                    onClick = { onChange { it.copy(lines = it.lines + ArchiveLineDraft()) } },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(AppIcons.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.maintenance_add_line))
                }
                Text(
                    text = stringResource(R.string.maintenance_lines_total, total),
                    style = MaterialTheme.typography.titleMedium,
                    color = tc.AccentExpense,
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

    if (showDatePicker) {
        IsoDatePickerDialog(
            initial = draft.serviceDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { date ->
                onChange { it.copy(serviceDate = date) }
                showDatePicker = false
            },
        )
    }
}

@Composable
private fun ServiceLineRow(
    line: ArchiveLineDraft,
    showRemove: Boolean,
    fieldColors: androidx.compose.material3.TextFieldColors,
    onDescription: (String) -> Unit,
    onAmount: (String) -> Unit,
    onRemove: () -> Unit,
) {
    val tc = LocalTruckColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = line.description,
            onValueChange = onDescription,
            label = { Text(stringResource(R.string.maintenance_line_name)) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            colors = fieldColors,
        )
        OutlinedTextField(
            value = line.amount,
            onValueChange = onAmount,
            label = { Text(stringResource(R.string.maintenance_line_price)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(110.dp),
            singleLine = true,
            colors = fieldColors,
        )
        if (showRemove) {
            IconButton(onClick = onRemove) {
                Icon(
                    AppIcons.Delete,
                    contentDescription = stringResource(R.string.common_delete),
                    tint = tc.TextSecondary,
                )
            }
        }
    }
}

package com.truckerload.presentation.screens.maintenance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.LocalTruckColors
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddArchiveDialog(
    draft: ArchiveDraft,
    isSaving: Boolean,
    errorKey: String?,
    onDismiss: () -> Unit,
    onChange: ((ArchiveDraft) -> ArchiveDraft) -> Unit,
    onSave: () -> Unit,
    onRetakePhoto: () -> Unit,
) {
    val tc = LocalTruckColors.current
    var showDatePicker by remember { mutableStateOf(false) }
    val fieldColors = AppTextFieldDefaults.outlined()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tc.CardBackground,
        title = { Text(stringResource(R.string.maintenance_edit_receipt_title), color = tc.TextPrimary) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
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
                OutlinedButton(onClick = onRetakePhoto, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.maintenance_retake_photo))
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
                    value = draft.description,
                    onValueChange = { value -> onChange { it.copy(description = value) } },
                    label = { Text(stringResource(R.string.maintenance_archive_what)) },
                    modifier = Modifier.fillMaxWidth(),
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
                OutlinedTextField(
                    value = draft.amount,
                    onValueChange = { value -> onChange { it.copy(amount = value.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }) } },
                    label = { Text(stringResource(R.string.maintenance_archive_amount)) },
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

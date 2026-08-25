package com.truckerload.presentation.components

import com.truckerload.presentation.icons.AppIcons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import com.truckerload.presentation.components.TlTextButton as TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.model.Load
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.utils.dateStringToUtcDatePickerMillis
import com.truckerload.utils.utcDatePickerMillisToDateString
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisputeSection(
    load: Load,
    onDisputeChanged: (Load) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
    var showDatePicker by remember { mutableStateOf(false) }
    var amountText by remember(load.id) {
        mutableStateOf(formatDisputeAmountInput(load.disputeAmount))
    }

    if (showDatePicker) {
        val today = Calendar.getInstance(Locale.US)
        val todayIso = "%04d-%02d-%02d".format(
            Locale.US,
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH) + 1,
            today.get(Calendar.DAY_OF_MONTH),
        )
        val initialMillis = dateStringToUtcDatePickerMillis(load.disputeResponseDate ?: todayIso)
            ?: System.currentTimeMillis()
        val yearForRange = load.disputeResponseDate?.take(4)?.toIntOrNull()
            ?: today.get(Calendar.YEAR)
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            yearRange = IntRange(yearForRange - 1, yearForRange + 2),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            colors = androidx.compose.material3.DatePickerDefaults.colors(
                containerColor = tc.CardBackground,
            ),
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = dateState.selectedDateMillis
                        if (selectedMillis != null) {
                            val date = utcDatePickerMillisToDateString(selectedMillis)
                            onDisputeChanged(
                                load.copy(
                                    isDispute = true,
                                    disputeResponseDate = date,
                                    disputeCompleted = false,
                                    disputeAmount = parseDisputeAmount(amountText)
                                        ?: load.disputeAmount,
                                ),
                            )
                        }
                        showDatePicker = false
                    },
                ) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        ) { DatePicker(state = dateState) }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.dispute_section_title),
            style = MaterialTheme.typography.titleMedium,
            color = tc.TextPrimary,
        )

        if (load.hadDispute) {
            DisputeBadge(
                label = stringResource(R.string.dispute_was_dispute),
                color = tc.Success,
            )
            load.disputeResponseDate?.let { date ->
                Text(
                    text = stringResource(R.string.dispute_response_date_label, date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tc.TextSecondary,
                )
            }
            load.disputeAmount?.takeIf { it > 0 }?.let { amount ->
                val formatted = formatDisputeUsd(amount)
                Text(
                    text = stringResource(
                        if (load.disputeAmountApplied) {
                            R.string.dispute_amount_added
                        } else {
                            R.string.dispute_amount_not_added
                        },
                        formatted,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tc.TextSecondary,
                )
            }
            return@Column
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = load.isDispute,
                onCheckedChange = { checked ->
                    if (checked) {
                        showDatePicker = true
                    } else {
                        amountText = ""
                        onDisputeChanged(
                            load.copy(
                                isDispute = false,
                                disputeResponseDate = null,
                                disputeCompleted = false,
                                disputeAmount = null,
                                disputeApplyToLoad = false,
                                disputeAmountApplied = false,
                            ),
                        )
                    }
                },
                enabled = !load.disputeCompleted,
            )
            Text(
                text = stringResource(R.string.dispute_checkbox_label),
                style = MaterialTheme.typography.bodyLarge,
                color = tc.TextPrimary,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        if (load.isActiveDispute) {
            DisputeBadge(
                label = stringResource(R.string.dispute_active),
                color = tc.Danger,
            )
            OutlinedTextField(
                value = load.disputeResponseDate.orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.dispute_response_date_title)) },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(AppIcons.Edit, contentDescription = stringResource(R.string.common_edit))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                colors = AppTextFieldDefaults.outlined(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { raw ->
                        val cleaned = raw.filter { it.isDigit() || it == '.' || it == ',' }
                        amountText = cleaned
                        onDisputeChanged(
                            load.copy(disputeAmount = parseDisputeAmount(cleaned)),
                        )
                    },
                    label = { Text(stringResource(R.string.dispute_amount_label)) },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = AppTextFieldDefaults.outlined(),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Checkbox(
                        checked = load.disputeApplyToLoad,
                        onCheckedChange = { checked ->
                            onDisputeChanged(
                                load.copy(
                                    disputeAmount = parseDisputeAmount(amountText)
                                        ?: load.disputeAmount,
                                    disputeApplyToLoad = checked,
                                ),
                            )
                        },
                    )
                    Text(
                        text = stringResource(R.string.dispute_apply_to_load),
                        style = MaterialTheme.typography.labelSmall,
                        color = tc.TextSecondary,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = load.disputeCompleted,
                    onCheckedChange = { completed ->
                        if (completed) {
                            onDisputeChanged(
                                load.copy(
                                    disputeCompleted = true,
                                    disputeAmount = parseDisputeAmount(amountText)
                                        ?: load.disputeAmount,
                                ),
                            )
                        }
                    },
                )
                Text(
                    text = stringResource(R.string.dispute_completed_checkbox),
                    style = MaterialTheme.typography.bodyLarge,
                    color = tc.TextPrimary,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}

@Composable
fun DisputeBadge(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = color,
        )
    }
}

internal fun parseDisputeAmount(raw: String): Double? {
    val cleaned = raw.replace(',', '.').trim()
    if (cleaned.isEmpty() || cleaned == ".") return null
    return cleaned.toDoubleOrNull()?.takeIf { it >= 0.0 }
}

internal fun formatDisputeAmountInput(amount: Double?): String {
    if (amount == null) return ""
    return if (abs(amount - amount.toLong()) < 0.0001) {
        amount.toLong().toString()
    } else {
        amount.toString()
    }
}

internal fun formatDisputeUsd(amount: Double): String =
    if (abs(amount - amount.toLong()) < 0.005) {
        "$${amount.toLong()}"
    } else {
        String.format(Locale.US, "$%.2f", amount)
    }

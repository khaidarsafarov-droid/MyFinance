package com.truckerload.presentation.screens.add

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.MoneyFormat
import com.truckerload.utils.dateStringToUtcDatePickerMillis
import com.truckerload.utils.formatDateForDisplay
import com.truckerload.utils.formatDateTimeForDisplay
import com.truckerload.utils.formatIsoDate
import com.truckerload.utils.getWeekRange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDieselScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
) {
    val tc = LocalTruckColors.current
    val viewModel: AddDieselViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var datePickerThenTime by remember { mutableStateOf(false) }
    val (_, _, weekLabel) = getWeekRange(uiState.weekNumber, uiState.year)

    DieselLocationPermissionEffect(onGranted = viewModel::ensureLocation)

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            viewModel.clearSaved()
            onSaved()
        }
    }

    if (showDatePicker) {
        JournalDatePickerDialog(
            recordedAtMillis = uiState.recordedAtMillis,
            initialDateMillis = dateStringToUtcDatePickerMillis(formatIsoDate(uiState.recordedAtMillis))
                ?: uiState.recordedAtMillis,
            onDismiss = { showDatePicker = false },
            onConfirm = viewModel::setRecordedDate,
            openTimePickerAfterConfirm = datePickerThenTime,
            onOpenTimePicker = { showTimePicker = true },
        )
    }
    if (showTimePicker) {
        JournalTimePickerDialog(
            recordedAtMillis = uiState.recordedAtMillis,
            onDismiss = { showTimePicker = false },
            onConfirm = viewModel::setRecordedTime,
        )
    }
    if (uiState.showSaveDialog) {
        JournalSaveConfirmDialog(
            onDismiss = viewModel::dismissSaveDialog,
            onSave = viewModel::save,
            saveEnabled = !uiState.isSaving,
            onEditDateTime = {
                datePickerThenTime = true
                showDatePicker = true
            },
            dateTimeLabel = formatDateTimeForDisplay(uiState.recordedAtMillis),
        ) {
            uiState.paidTotal?.let { total ->
                Text(
                    stringResource(R.string.add_diesel_summary_paid, MoneyFormat.formatCurrency(total)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tc.TextSecondary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            uiState.savings?.takeIf { it > 0.0 }?.let { saved ->
                Text(
                    stringResource(R.string.add_diesel_summary_saved, MoneyFormat.formatCurrency(saved)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tc.AccentProfit,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            uiState.error?.let {
                Text(
                    it,
                    color = tc.AccentExpense,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }

    JournalEntryScaffold(
        title = stringResource(R.string.add_diesel_title),
        onBack = onBack,
        onSave = viewModel::openSaveDialog,
        saveEnabled = !uiState.isSaving,
        errorMessage = uiState.error?.takeIf { !uiState.showSaveDialog },
    ) {
        BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    stringResource(R.string.add_diesel_date),
                    style = MaterialTheme.typography.titleSmall,
                    color = tc.TextPrimary,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            datePickerThenTime = false
                            showDatePicker = true
                        },
                ) {
                    Text(
                        formatDateForDisplay(uiState.recordedAtMillis),
                        style = MaterialTheme.typography.titleMedium,
                        color = tc.TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = {
                        datePickerThenTime = false
                        showDatePicker = true
                    }) {
                        Icon(
                            AppIcons.Edit,
                            contentDescription = stringResource(R.string.add_diesel_change_date),
                            tint = tc.TextPrimary,
                        )
                    }
                }
                JournalWeekSelectorRow(
                    weekNumber = uiState.weekNumber,
                    weekLabel = weekLabel,
                    onPreviousWeek = viewModel::selectPreviousWeek,
                    onNextWeek = viewModel::selectNextWeek,
                )
                DieselAmountFields(
                    uiState = uiState,
                    onGallonsChange = viewModel::setGallonsText,
                    onPriceChange = viewModel::setPricePerGallonText,
                    onDiscountChange = viewModel::setDiscountPriceText,
                    onLocationChange = viewModel::setLocationText,
                    onImagePicked = viewModel::scanReceipt,
                )
            }
        }
    }
}

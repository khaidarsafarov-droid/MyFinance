package com.truckerload.presentation.screens.add

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.truckerload.presentation.ScreenPhase
import com.truckerload.presentation.components.EditLoadSkeleton
import com.truckerload.presentation.components.ScreenLoadError
import com.truckerload.presentation.components.rememberFormBack
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.MoneyFormat
import com.truckerload.presentation.utils.buttonTap
import com.truckerload.utils.dateStringToUtcDatePickerMillis
import com.truckerload.utils.formatDateForDisplay
import com.truckerload.utils.formatDateTimeForDisplay
import com.truckerload.utils.formatIsoDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDieselScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
) {
    val tc = LocalTruckColors.current
    val viewModel: AddDieselViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val requestBack = rememberFormBack(isDirty = uiState.isDirty, onBack = onBack)
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var datePickerThenTime by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    DieselLocationPermissionEffect(onGranted = viewModel::ensureLocation)

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            viewModel.clearSaved()
            onSaved()
        }
    }

    LaunchedEffect(uiState.deleted) {
        if (uiState.deleted) {
            viewModel.clearDeleted()
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
            uiState.savings?.takeIf { it.isPositive() }?.let { saved ->
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


    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.diesel_delete_title)) },
            text = { Text(stringResource(R.string.diesel_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.delete()
                    },
                    enabled = !uiState.isSaving,
                ) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    JournalEntryScaffold(
        title = stringResource(
            if (uiState.editingId != null) R.string.edit_diesel_title else R.string.add_diesel_title,
        ),
        onBack = requestBack,
        onSave = viewModel::openSaveDialog,
        saveEnabled = !uiState.isSaving && uiState.recordPhase == ScreenPhase.Content,
        errorMessage = uiState.error?.takeIf { !uiState.showSaveDialog },
        onDelete = if (uiState.editingId != null) {
            { showDeleteConfirm = true }
        } else {
            null
        },
    ) {
        when (uiState.recordPhase) {
            ScreenPhase.Loading -> EditLoadSkeleton(
                contentDescription = stringResource(R.string.screen_loading),
            )
            ScreenPhase.Error -> ScreenLoadError(onRetry = viewModel::retryEditLoad)
            ScreenPhase.Empty, ScreenPhase.Content -> DieselEntryForm(
                uiState = uiState,
                onOpenDate = {
                    datePickerThenTime = false
                    showDatePicker = true
                },
                onGallonsChange = viewModel::setGallonsText,
                onPriceChange = viewModel::setPricePerGallonText,
                onDiscountChange = viewModel::setDiscountPriceText,
                onLocationChange = viewModel::setLocationText,
                onImagePicked = viewModel::scanReceipt,
                onSubmit = viewModel::openSaveDialog,
            )
        }
    }
}

@Composable
private fun DieselEntryForm(
    uiState: AddDieselUiState,
    onOpenDate: () -> Unit,
    onGallonsChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onDiscountChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onImagePicked: (Uri) -> Unit,
    onSubmit: () -> Unit,
) {
    val tc = LocalTruckColors.current
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
                    .buttonTap(onClick = onOpenDate),
            ) {
                Text(
                    formatDateForDisplay(uiState.recordedAtMillis),
                    style = MaterialTheme.typography.titleMedium,
                    color = tc.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onOpenDate) {
                    Icon(
                        AppIcons.Edit,
                        contentDescription = stringResource(R.string.add_diesel_change_date),
                        tint = tc.TextPrimary,
                    )
                }
            }
            DieselAmountFields(
                uiState = uiState,
                onGallonsChange = onGallonsChange,
                onPriceChange = onPriceChange,
                onDiscountChange = onDiscountChange,
                onLocationChange = onLocationChange,
                onImagePicked = onImagePicked,
                onSubmit = onSubmit,
            )
        }
    }
}

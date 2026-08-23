package com.truckerload.presentation.screens.add

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.presentation.components.TlTextButton as TextButton
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens
import com.truckerload.presentation.utils.MoneyFormat
import com.truckerload.utils.formatDateTimeForDisplay
import com.truckerload.utils.getWeekRange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPaycheckScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
) {
    val tc = LocalTruckColors.current
    val viewModel: AddPaycheckViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val (_, _, weekLabel) = getWeekRange(uiState.weekNumber, uiState.year)

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            viewModel.clearSaved()
            onSaved()
        }
    }

    if (showDatePicker) {
        JournalDatePickerDialog(
            recordedAtMillis = uiState.recordedAtMillis,
            initialDateMillis = uiState.recordedAtMillis,
            onDismiss = { showDatePicker = false },
            onConfirm = viewModel::setRecordedDate,
            openTimePickerAfterConfirm = true,
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
            onEditDateTime = { showDatePicker = true },
            dateTimeLabel = formatDateTimeForDisplay(uiState.recordedAtMillis),
        ) {
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
        title = stringResource(R.string.add_paycheck_title),
        onBack = onBack,
        onSave = viewModel::openSaveDialog,
        saveEnabled = !uiState.isSaving,
        errorMessage = uiState.error?.takeIf { !uiState.showSaveDialog },
    ) {
        BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                JournalWeekSelectorRow(
                    weekNumber = uiState.weekNumber,
                    weekLabel = weekLabel,
                    onPreviousWeek = viewModel::selectPreviousWeek,
                    onNextWeek = viewModel::selectNextWeek,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
                OutlinedTextField(
                    value = uiState.amountText,
                    onValueChange = viewModel::setAmountText,
                    label = { Text(stringResource(R.string.common_enter_amount)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = UiDimens.InputMinHeight),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = AppTextFieldDefaults.outlined(),
                )
                val parsedAmount = uiState.amountText.replace(",", "").toDoubleOrNull()
                if (parsedAmount != null) {
                    Text(
                        text = MoneyFormat.formatCurrency(parsedAmount),
                        style = AppTypography.HeroNumber,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                val last = uiState.lastAmount
                if (last != null && uiState.amountText.isBlank()) {
                    TextButton(
                        onClick = viewModel::applyLastAmount,
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text(
                            stringResource(
                                R.string.ux_smart_default_last_amount,
                                MoneyFormat.formatCurrency(last),
                            ),
                        )
                    }
                }
            }
        }
    }
}

package com.truckerload.presentation.screens.add

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckerload.R
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlTextButton as TextButton
import com.truckerload.presentation.di.LocalPaycheckRepository
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.utils.formatDateTimeForDisplay
import com.truckerload.utils.getWeekRange
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPaycheckScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val paycheckRepository = LocalPaycheckRepository.current
    val viewModel: AddPaycheckViewModel = viewModel(
        factory = AddPaycheckViewModel.Factory(application, paycheckRepository),
    )
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
        val cal = Calendar.getInstance().apply { timeInMillis = uiState.recordedAtMillis }
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.recordedAtMillis,
            yearRange = IntRange(cal.get(Calendar.YEAR) - 2, cal.get(Calendar.YEAR) + 1),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            colors = androidx.compose.material3.DatePickerDefaults.colors(
                containerColor = tc.CardBackground,
            ),
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let(viewModel::setRecordedDate)
                    showDatePicker = false
                    showTimePicker = true
                }) { Text(stringResource(R.string.common_ok)) }
            },
        ) { DatePicker(state = dateState) }
    }
    if (showTimePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = uiState.recordedAtMillis }
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
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setRecordedTime(timeState.hour, timeState.minute)
                    showTimePicker = false
                }) { Text(stringResource(R.string.common_ok)) }
            },
            text = { TimePicker(state = timeState) },
        )
    }
    if (uiState.showSaveDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissSaveDialog,
            containerColor = tc.CardBackground,
            titleContentColor = tc.TextPrimary,
            textContentColor = tc.TextPrimary,
            title = { Text(stringResource(R.string.common_save)) },
            text = {
                Column {
                    Text(stringResource(R.string.common_date_time), style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clickable { showDatePicker = true },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(formatDateTimeForDisplay(uiState.recordedAtMillis), style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.common_edit))
                        }
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
            },
            confirmButton = {
                Button(
                    onClick = viewModel::save,
                    modifier = Modifier.height(48.dp),
                    enabled = !uiState.isSaving,
                ) {
                    Text(stringResource(R.string.common_save))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissSaveDialog) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    Scaffold(
        containerColor = tc.Background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_paycheck_title), color = tc.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = tc.TextPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = tc.Background,
                    titleContentColor = tc.TextPrimary,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(stringResource(R.string.add_select_week), style = MaterialTheme.typography.titleSmall, color = tc.TextPrimary)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    ) {
                        IconButton(onClick = viewModel::selectPreviousWeek) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = tc.TextPrimary)
                        }
                        Text(
                            stringResource(R.string.add_or_edit_week_format, uiState.weekNumber, weekLabel),
                            style = MaterialTheme.typography.bodyMedium,
                            color = tc.TextSecondary,
                        )
                        IconButton(onClick = viewModel::selectNextWeek) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = tc.TextPrimary)
                        }
                    }
                    OutlinedTextField(
                        value = uiState.amountText,
                        onValueChange = viewModel::setAmountText,
                        label = { Text(stringResource(R.string.common_enter_amount)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = tc.AccentPrimary,
                            unfocusedBorderColor = tc.Divider,
                            focusedLabelColor = tc.AccentPrimary,
                            unfocusedLabelColor = tc.TextSecondary,
                            focusedTextColor = tc.TextPrimary,
                            unfocusedTextColor = tc.TextPrimary,
                            focusedContainerColor = BentoGlassTheme.CardFill,
                            unfocusedContainerColor = BentoGlassTheme.CardFill,
                            cursorColor = tc.AccentPrimary,
                        ),
                    )
                }
            }
            Button(
                onClick = viewModel::openSaveDialog,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !uiState.isSaving,
            ) {
                Text(stringResource(R.string.common_save))
            }
            uiState.error?.let {
                Text(it, color = tc.AccentExpense, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

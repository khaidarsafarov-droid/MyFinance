package com.truckerload.presentation.screens.add

import android.widget.Toast
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.truckerload.presentation.components.TlButton as Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.di.LocalAiRepository
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.MoneyFormat
import com.truckerload.sync.LoadAlarmPlanner
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoadScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    onOptimisticInsert: ((com.truckerload.domain.model.Load) -> Unit)? = null,
    onRevertOptimistic: ((String) -> Unit)? = null,
) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val aiRepository = LocalAiRepository.current
    val viewModel: AddLoadViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val parseFailed = stringResource(R.string.add_load_parse_failed)
    val alarmSetToast = stringResource(R.string.load_alarm_set_toast)
    val invalidCustomTime = stringResource(R.string.load_alarm_invalid_custom_time)

    LaunchedEffect(uiState.savedLoad, uiState.alarmPrompt) {
        if (uiState.savedLoad != null && uiState.alarmPrompt == null) {
            viewModel.clearSaved()
            onSaved()
        }
    }

    val prompt = uiState.alarmPrompt
    if (prompt != null && !prompt.showCustomPicker) {
        AlarmOfferDialog(
            tripId = prompt.tripId,
            pickupMillis = prompt.pickupMillis,
            availablePresets = prompt.availablePresets,
            onPreset = { preset ->
                if (viewModel.schedulePresetAlarm(preset)) {
                    Toast.makeText(context, alarmSetToast, Toast.LENGTH_SHORT).show()
                }
            },
            onCustom = viewModel::showCustomAlarmPicker,
            onDismiss = viewModel::dismissAlarmPrompt,
        )
    }
    if (prompt != null && prompt.showCustomPicker) {
        CustomAlarmPickerDialog(
            pickupMillis = prompt.pickupMillis,
            error = prompt.customError,
            onConfirm = { triggerAt ->
                if (viewModel.scheduleCustomAlarm(triggerAt, invalidCustomTime)) {
                    Toast.makeText(context, alarmSetToast, Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = viewModel::hideCustomAlarmPicker,
        )
    }

    Scaffold(
        containerColor = BentoGlassTheme.ScreenBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_load_title), color = tc.TextPrimary) },
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
                    containerColor = BentoGlassTheme.ScreenBackground,
                    titleContentColor = tc.TextPrimary,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = uiState.rawText,
                    onValueChange = viewModel::setRawText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(12.dp),
                    label = { Text(stringResource(R.string.add_load_input_label)) },
                    placeholder = { Text(stringResource(R.string.add_load_input_placeholder)) },
                    shape = RoundedCornerShape(BentoGlassTheme.CellRadius),
                    colors = AppTextFieldDefaults.outlined(),
                )
            }
            LoadParsePreviewCard(
                preview = uiState.previewLoad,
                isParsing = uiState.isParsingPreview,
                parseHint = uiState.previewHint,
            )
            val preview = uiState.previewLoad
            val saveLabel = when {
                uiState.isSaving -> stringResource(R.string.add_load_saving)
                preview != null && preview.totalRate > 0 -> stringResource(
                    R.string.add_load_save_with_preview,
                    MoneyFormat.formatCurrency(preview.totalRate),
                    MoneyFormat.formatNumber(preview.totalMiles),
                )
                else -> stringResource(R.string.add_load_save_offline)
            }
            Button(
                onClick = {
                    viewModel.save(
                        parseFailedFallback = parseFailed,
                        saveErrorFormatter = { msg ->
                            context.getString(R.string.common_save_error, msg)
                        },
                        onOptimisticInsert = onOptimisticInsert,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = uiState.rawText.isNotBlank() && !uiState.isSaving && aiRepository != null,
            ) {
                Text(saveLabel)
            }
            Text(
                stringResource(R.string.add_load_hint_online),
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
                modifier = Modifier.padding(top = 8.dp),
            )
            uiState.error?.let {
                Text(it, color = tc.AccentExpense, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun AlarmOfferDialog(
    tripId: String,
    pickupMillis: Long,
    availablePresets: List<LoadAlarmPlanner.Preset>,
    onPreset: (LoadAlarmPlanner.Preset) -> Unit,
    onCustom: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tc = LocalTruckColors.current
    val pickupLabel = remember(pickupMillis) {
        SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(pickupMillis))
    }
    val message = if (tripId.isNotBlank()) {
        stringResource(R.string.load_alarm_offer_message, tripId, pickupLabel)
    } else {
        stringResource(R.string.load_alarm_offer_message_no_trip, pickupLabel)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tc.CardBackground,
        titleContentColor = tc.TextPrimary,
        textContentColor = tc.TextPrimary,
        title = { Text(stringResource(R.string.load_alarm_offer_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(message)
                availablePresets.forEach { preset ->
                    val label = when (preset) {
                        LoadAlarmPlanner.Preset.TWO_HOURS ->
                            stringResource(R.string.load_alarm_option_2h)
                        LoadAlarmPlanner.Preset.ONE_HOUR ->
                            stringResource(R.string.load_alarm_option_1h)
                    }
                    TextButton(
                        onClick = { onPreset(preset) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(label)
                    }
                }
                TextButton(
                    onClick = onCustom,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.load_alarm_option_custom))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.load_alarm_skip))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomAlarmPickerDialog(
    pickupMillis: Long,
    error: String?,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val tc = LocalTruckColors.current
    val defaultTrigger = remember(pickupMillis) {
        (pickupMillis - LoadAlarmPlanner.HOUR_MS * 2)
            .coerceAtLeast(System.currentTimeMillis() + 60_000L)
            .coerceAtMost(pickupMillis - 60_000L)
    }
    val cal = remember(defaultTrigger) {
        Calendar.getInstance().apply { timeInMillis = defaultTrigger }
    }
    var step by remember { mutableStateOf(0) } // 0 = date, 1 = time
    var selectedDateUtcMillis by remember {
        mutableStateOf(dateOnlyUtcMillis(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)))
    }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = selectedDateUtcMillis)
    val timeState = rememberTimePickerState(
        initialHour = cal.get(Calendar.HOUR_OF_DAY),
        initialMinute = cal.get(Calendar.MINUTE),
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tc.CardBackground,
        titleContentColor = tc.TextPrimary,
        textContentColor = tc.TextPrimary,
        title = {
            Text(
                if (step == 0) {
                    stringResource(R.string.load_alarm_custom_date_title)
                } else {
                    stringResource(R.string.load_alarm_custom_time_title)
                },
            )
        },
        text = {
            Column {
                if (step == 0) {
                    DatePicker(state = dateState)
                } else {
                    TimePicker(state = timeState)
                }
                error?.let {
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
            TextButton(
                onClick = {
                    if (step == 0) {
                        selectedDateUtcMillis = dateState.selectedDateMillis ?: selectedDateUtcMillis
                        step = 1
                    } else {
                        onConfirm(
                            combineDateAndTime(selectedDateUtcMillis, timeState.hour, timeState.minute),
                        )
                    }
                },
            ) {
                Text(
                    if (step == 0) {
                        stringResource(R.string.common_ok)
                    } else {
                        stringResource(R.string.load_alarm_set)
                    },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (step == 1) step = 0 else onDismiss()
            }) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

/** Midnight UTC millis for the given local civil date (Material DatePicker convention). */
private fun dateOnlyUtcMillis(year: Int, monthZeroBased: Int, day: Int): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(year, monthZeroBased, day, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return utc.timeInMillis
}

private fun combineDateAndTime(dateUtcMillis: Long, hour: Int, minute: Int): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = dateUtcMillis
    }
    val local = Calendar.getInstance().apply {
        set(
            utc.get(Calendar.YEAR),
            utc.get(Calendar.MONTH),
            utc.get(Calendar.DAY_OF_MONTH),
            hour,
            minute,
            0,
        )
        set(Calendar.MILLISECOND, 0)
    }
    return local.timeInMillis
}

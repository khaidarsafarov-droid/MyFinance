package com.truckerload.presentation.screens.add

import android.app.Application
import android.widget.Toast
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
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckerload.R
import com.truckerload.domain.alarm.LoadAlarmPlanner
import com.truckerload.domain.model.Load
import com.truckerload.presentation.di.LocalAiRepository
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.sync.LoadAlarmScheduler
import com.truckerload.utils.utcDatePickerMillisToDateString
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private enum class AlarmPromptStep {
    NONE,
    ASK,
    CHOOSE,
    CUSTOM_DATE,
    CUSTOM_TIME,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoadScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    onOptimisticInsert: ((Load) -> Unit)? = null,
    onRevertOptimistic: ((String) -> Unit)? = null,
) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val loadRepository = LocalLoadRepository.current
    val aiRepository = LocalAiRepository.current
    val viewModel: AddLoadViewModel = viewModel(
        factory = AddLoadViewModel.Factory(application, loadRepository, aiRepository),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val parseFailed = stringResource(R.string.add_load_parse_failed)

    var alarmStep by remember { mutableStateOf(AlarmPromptStep.NONE) }
    var pendingLoad by remember { mutableStateOf<Load?>(null) }
    var alarmOffer by remember { mutableStateOf<LoadAlarmPlanner.Offer?>(null) }
    var customDraftMillis by remember { mutableLongStateOf(0L) }

    fun finishWithoutAlarm() {
        alarmStep = AlarmPromptStep.NONE
        pendingLoad = null
        alarmOffer = null
        viewModel.clearSaved()
        onSaved()
    }

    fun scheduleAndFinish(preset: LoadAlarmPlanner.Preset, customMillis: Long? = null) {
        val load = pendingLoad ?: return finishWithoutAlarm()
        val offer = alarmOffer ?: return finishWithoutAlarm()
        val trigger = LoadAlarmPlanner.triggerMillis(preset, offer.pickupMillis, customMillis)
        if (trigger == null ||
            !LoadAlarmPlanner.isValidTrigger(trigger, offer.pickupMillis, System.currentTimeMillis())
        ) {
            Toast.makeText(context, context.getString(R.string.load_alarm_invalid_time), Toast.LENGTH_SHORT).show()
            return
        }
        val ok = LoadAlarmScheduler.schedule(context, load, trigger, offer.pickupMillis)
        Toast.makeText(
            context,
            context.getString(
                if (ok) R.string.load_alarm_scheduled else R.string.load_alarm_schedule_failed,
            ),
            Toast.LENGTH_SHORT,
        ).show()
        finishWithoutAlarm()
    }

    LaunchedEffect(uiState.savedLoad) {
        val saved = uiState.savedLoad ?: return@LaunchedEffect
        if (alarmStep != AlarmPromptStep.NONE) return@LaunchedEffect
        val offer = LoadAlarmPlanner.offerForLoad(saved)
        if (offer == null) {
            viewModel.clearSaved()
            onSaved()
        } else {
            pendingLoad = saved
            alarmOffer = offer
            alarmStep = AlarmPromptStep.ASK
        }
    }

    val offer = alarmOffer
    if (alarmStep == AlarmPromptStep.ASK && offer != null) {
        val pickupLabel = remember(offer.pickupMillis) {
            SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(offer.pickupMillis))
        }
        AlertDialog(
            onDismissRequest = { finishWithoutAlarm() },
            containerColor = tc.CardBackground,
            titleContentColor = tc.TextPrimary,
            textContentColor = tc.TextPrimary,
            title = { Text(stringResource(R.string.load_alarm_ask_title)) },
            text = {
                Text(stringResource(R.string.load_alarm_ask_message, pickupLabel))
            },
            confirmButton = {
                TextButton(onClick = { alarmStep = AlarmPromptStep.CHOOSE }) {
                    Text(stringResource(R.string.load_alarm_ask_yes), color = tc.AccentPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { finishWithoutAlarm() }) {
                    Text(stringResource(R.string.load_alarm_ask_no), color = tc.TextSecondary)
                }
            },
        )
    }

    if (alarmStep == AlarmPromptStep.CHOOSE && offer != null) {
        AlertDialog(
            onDismissRequest = { finishWithoutAlarm() },
            containerColor = tc.CardBackground,
            titleContentColor = tc.TextPrimary,
            textContentColor = tc.TextPrimary,
            title = { Text(stringResource(R.string.load_alarm_choose_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (LoadAlarmPlanner.Preset.TWO_HOURS_BEFORE in offer.availablePresets) {
                        TextButton(
                            onClick = {
                                scheduleAndFinish(LoadAlarmPlanner.Preset.TWO_HOURS_BEFORE)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.load_alarm_option_2h), color = tc.AccentPrimary)
                        }
                    }
                    if (LoadAlarmPlanner.Preset.ONE_HOUR_BEFORE in offer.availablePresets) {
                        TextButton(
                            onClick = {
                                scheduleAndFinish(LoadAlarmPlanner.Preset.ONE_HOUR_BEFORE)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.load_alarm_option_1h), color = tc.AccentPrimary)
                        }
                    }
                    TextButton(
                        onClick = {
                            customDraftMillis = LoadAlarmPlanner.defaultCustomMillis(
                                offer.pickupMillis,
                                System.currentTimeMillis(),
                            )
                            alarmStep = AlarmPromptStep.CUSTOM_DATE
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.load_alarm_option_custom), color = tc.AccentPrimary)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { finishWithoutAlarm() }) {
                    Text(stringResource(R.string.common_cancel), color = tc.TextSecondary)
                }
            },
        )
    }

    if (alarmStep == AlarmPromptStep.CUSTOM_DATE && offer != null) {
        val cal = Calendar.getInstance().apply { timeInMillis = customDraftMillis }
        val utcNoon = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(Calendar.YEAR, cal.get(Calendar.YEAR))
            set(Calendar.MONTH, cal.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 12)
        }.timeInMillis
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = utcNoon,
            yearRange = IntRange(cal.get(Calendar.YEAR) - 1, cal.get(Calendar.YEAR) + 1),
        )
        DatePickerDialog(
            onDismissRequest = { alarmStep = AlarmPromptStep.CHOOSE },
            colors = androidx.compose.material3.DatePickerDefaults.colors(
                containerColor = tc.CardBackground,
            ),
            confirmButton = {
                TextButton(onClick = {
                    val selectedUtc = dateState.selectedDateMillis ?: return@TextButton
                    val dateStr = utcDatePickerMillisToDateString(selectedUtc)
                    val parts = dateStr.split("-")
                    if (parts.size == 3) {
                        val next = Calendar.getInstance().apply {
                            timeInMillis = customDraftMillis
                            set(Calendar.YEAR, parts[0].toInt())
                            set(Calendar.MONTH, parts[1].toInt() - 1)
                            set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                        }
                        customDraftMillis = next.timeInMillis
                    }
                    alarmStep = AlarmPromptStep.CUSTOM_TIME
                }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { alarmStep = AlarmPromptStep.CHOOSE }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        ) { DatePicker(state = dateState) }
    }

    if (alarmStep == AlarmPromptStep.CUSTOM_TIME && offer != null) {
        val cal = Calendar.getInstance().apply { timeInMillis = customDraftMillis }
        val timeState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { alarmStep = AlarmPromptStep.CUSTOM_DATE },
            containerColor = tc.CardBackground,
            titleContentColor = tc.TextPrimary,
            textContentColor = tc.TextPrimary,
            title = { Text(stringResource(R.string.load_alarm_custom_time_title)) },
            confirmButton = {
                TextButton(onClick = {
                    val next = Calendar.getInstance().apply {
                        timeInMillis = customDraftMillis
                        set(Calendar.HOUR_OF_DAY, timeState.hour)
                        set(Calendar.MINUTE, timeState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    scheduleAndFinish(LoadAlarmPlanner.Preset.CUSTOM, next.timeInMillis)
                }) { Text(stringResource(R.string.common_ok), color = tc.AccentPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { alarmStep = AlarmPromptStep.CUSTOM_DATE }) {
                    Text(stringResource(R.string.common_cancel), color = tc.TextSecondary)
                }
            },
            text = { TimePicker(state = timeState) },
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
                Text(
                    if (uiState.isSaving) {
                        stringResource(R.string.add_load_saving)
                    } else {
                        stringResource(R.string.add_load_save_offline)
                    },
                )
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

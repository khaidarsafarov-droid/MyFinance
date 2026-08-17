package com.truckerload.presentation.screens.add

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.MoneyFormat

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
        AddLoadAlarmOfferDialog(
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
        AddLoadCustomAlarmPickerDialog(
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
            AddLoadModeSelector(
                selected = uiState.mode,
                onSelect = viewModel::setMode,
            )
            when (uiState.mode) {
                AddLoadInputMode.PASTE -> PasteLoadCard(
                    rawText = uiState.rawText,
                    onRawText = viewModel::setRawText,
                )
                AddLoadInputMode.MANUAL -> AddLoadManualForm(
                    fields = uiState.manual,
                    onTripId = viewModel::setManualTripId,
                    onDate = viewModel::setManualDate,
                    onRate = viewModel::setManualRate,
                    onMiles = viewModel::setManualMiles,
                    onPointA = viewModel::setManualPointA,
                    onPointB = viewModel::setManualPointB,
                )
                AddLoadInputMode.DOCUMENT -> AddLoadDocumentSection(
                    extractedText = uiState.rawText,
                    documentName = uiState.documentName,
                    isExtracting = uiState.isExtractingDocument,
                    onDocumentPicked = viewModel::importDocument,
                    onExtractedTextChange = viewModel::setRawText,
                )
            }
            if (uiState.mode != AddLoadInputMode.MANUAL) {
                LoadParsePreviewCard(
                    preview = uiState.previewLoad,
                    isParsing = uiState.isParsingPreview,
                    parseHint = uiState.previewHint,
                )
            }
            val preview = uiState.previewLoad
            val saveEnabled = !uiState.isSaving && !uiState.isExtractingDocument && when (uiState.mode) {
                AddLoadInputMode.MANUAL -> uiState.manual.canSave()
                AddLoadInputMode.PASTE, AddLoadInputMode.DOCUMENT -> uiState.rawText.isNotBlank()
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
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = saveEnabled,
            ) {
                Text(saveButtonLabel(uiState.isSaving, preview))
            }
            Text(
                stringResource(
                    when (uiState.mode) {
                        AddLoadInputMode.PASTE -> R.string.add_load_hint_paste
                        AddLoadInputMode.MANUAL -> R.string.add_load_hint_manual
                        AddLoadInputMode.DOCUMENT -> R.string.add_load_hint_document
                    },
                ),
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
private fun PasteLoadCard(rawText: String, onRawText: (String) -> Unit) {
    BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = rawText,
            onValueChange = onRawText,
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
}

@Composable
private fun saveButtonLabel(isSaving: Boolean, preview: com.truckerload.domain.model.Load?): String =
    when {
        isSaving -> stringResource(R.string.add_load_saving)
        preview != null && preview.totalRate > 0 -> stringResource(
            R.string.add_load_save_with_preview,
            MoneyFormat.formatCurrency(preview.totalRate),
            MoneyFormat.formatNumber(preview.totalMiles),
        )
        else -> stringResource(R.string.add_load_save_offline)
    }

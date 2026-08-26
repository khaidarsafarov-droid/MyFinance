package com.truckerload.presentation.screens.add

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.truckerload.presentation.components.OneUiBackButton
import com.truckerload.presentation.components.OneUiBottomActionBar
import com.truckerload.presentation.components.OneUiLargeTitleHeader
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.verticalContentScroll
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.MoneyFormat

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

    uiState.confirmIncomplete?.let { pending ->
        AddLoadConfirmIncompleteDialog(
            completeness = pending,
            onConfirm = {
                viewModel.confirmIncompleteSave(
                    saveErrorFormatter = { msg -> context.getString(R.string.common_save_error, msg) },
                    onOptimisticInsert = onOptimisticInsert,
                )
            },
            onDismiss = viewModel::dismissIncompleteConfirm,
        )
    }

    Scaffold(
        containerColor = BentoGlassTheme.ScreenBackground,
        topBar = {
            OneUiLargeTitleHeader(
                title = stringResource(R.string.add_load_title),
                navigationIcon = { OneUiBackButton(onBack = onBack) },
            )
        },
        bottomBar = {
            OneUiBottomActionBar {
                val preview = uiState.previewLoad
                val saveEnabled = !uiState.isSaving && !uiState.isExtractingDocument && when (uiState.mode) {
                    AddLoadInputMode.MANUAL, AddLoadInputMode.DOCUMENT -> uiState.manual.canSave()
                    AddLoadInputMode.PASTE -> uiState.rawText.isNotBlank()
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
                    Text(
                        saveButtonLabel(
                            isSaving = uiState.isSaving,
                            preview = preview,
                            manual = uiState.manual.takeIf { uiState.mode != AddLoadInputMode.PASTE },
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalContentScroll()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AddLoadModeSelector(
                selected = uiState.mode,
                onSelect = viewModel::setMode,
            )
            EquipmentTypeChipRow(
                selected = uiState.equipmentType,
                onSelect = viewModel::setEquipmentType,
            )
            when (uiState.mode) {
                AddLoadInputMode.PASTE -> PasteLoadCard(
                    rawText = uiState.rawText,
                    onRawText = viewModel::setRawText,
                )
                AddLoadInputMode.MANUAL -> {
                    uiState.completeness?.let { AddLoadReviewCard(completeness = it) }
                    AddLoadManualForm(
                        fields = uiState.manual,
                        onTripId = viewModel::setManualTripId,
                        onDate = viewModel::setManualDate,
                        onRate = viewModel::setManualRate,
                        onMiles = viewModel::setManualMiles,
                        onPointChange = viewModel::setManualPoint,
                        onAddPoint = viewModel::addManualPoint,
                        onRemovePoint = viewModel::removeManualPoint,
                    )
                }
                AddLoadInputMode.DOCUMENT -> {
                    AddLoadDocumentSection(
                        extractedText = uiState.rawText,
                        documentName = uiState.documentName,
                        isExtracting = uiState.isExtractingDocument,
                        onDocumentPicked = viewModel::importDocument,
                    )
                    if (uiState.rawText.isNotBlank() && !uiState.isExtractingDocument) {
                        uiState.completeness?.let { AddLoadReviewCard(completeness = it) }
                        AddLoadManualForm(
                            fields = uiState.manual,
                            onTripId = viewModel::setManualTripId,
                            onDate = viewModel::setManualDate,
                            onRate = viewModel::setManualRate,
                            onMiles = viewModel::setManualMiles,
                            onPointChange = viewModel::setManualPoint,
                            onAddPoint = viewModel::addManualPoint,
                            onRemovePoint = viewModel::removeManualPoint,
                            hintRes = R.string.add_load_document_fields_hint,
                        )
                    }
                }
            }
            if (uiState.mode == AddLoadInputMode.PASTE) {
                LoadParsePreviewCard(
                    preview = uiState.previewLoad,
                    isParsing = uiState.isParsingPreview,
                    parseHint = uiState.previewHint,
                )
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
private fun saveButtonLabel(
    isSaving: Boolean,
    preview: com.truckerload.domain.model.Load?,
    manual: ManualLoadFields?,
): String {
    if (isSaving) return stringResource(R.string.add_load_saving)
    val rate = preview?.totalRate?.takeIf { it > 0 } ?: manual?.parsedRate()?.takeIf { it > 0 }
    val miles = preview?.totalMiles ?: manual?.parsedMiles() ?: 0.0
    return if (rate != null) {
        stringResource(
            R.string.add_load_save_with_preview,
            MoneyFormat.formatCurrency(rate),
            MoneyFormat.formatNumber(miles),
        )
    } else {
        stringResource(R.string.add_load_save_offline)
    }
}

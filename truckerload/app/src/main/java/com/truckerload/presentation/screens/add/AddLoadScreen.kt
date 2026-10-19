package com.truckerload.presentation.screens.add

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.presentation.components.formKeyboard
import com.truckerload.presentation.components.formKeyboardActions
import com.truckerload.presentation.components.rememberFormBack
import com.truckerload.presentation.components.DashboardHero
import com.truckerload.presentation.components.OneUiBottomActionBar
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.verticalContentScroll
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.motion.SAVE_CONFIRM_HOLD_MS
import com.truckerload.presentation.motion.rememberMotionEnabled
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.addLoadSharedBounds
import com.truckerload.utils.getCurrentWeekNumberAndYear
import com.truckerload.utils.getWeekRange
import kotlinx.coroutines.delay

@Composable
fun AddLoadScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    onOptimisticInsert: ((com.truckerload.domain.model.Load) -> Unit)? = null,
    onRevertOptimistic: ((String) -> Unit)? = null,
) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val resources = LocalResources.current
    val viewModel: AddLoadViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val requestBack = rememberFormBack(isDirty = uiState.isDirty, onBack = onBack)
    val parseFailed = stringResource(R.string.add_load_parse_failed)
    val alarmSetToast = stringResource(R.string.load_alarm_set_toast)
    val invalidCustomTime = stringResource(R.string.load_alarm_invalid_custom_time)
    val saveCurrent = {
        viewModel.save(
            parseFailedFallback = parseFailed,
            saveErrorFormatter = { msg ->
                resources.getString(R.string.common_save_error, msg)
            },
            onOptimisticInsert = onOptimisticInsert,
        )
    }

    val motion = rememberMotionEnabled()
    var sawAlarm by remember { mutableStateOf(false) }
    var pasteToken by remember { mutableIntStateOf(0) }
    val pasteEmpty = stringResource(R.string.add_load_paste_empty)
    val weekChip = run {
        val (week, year) = getCurrentWeekNumberAndYear()
        val (_, _, label) = getWeekRange(week, year)
        resources.getString(R.string.home_week_chip, week, label.substringBefore(",").trim())
    }
    fun pasteFromClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val text = clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
        if (text.isBlank()) {
            Toast.makeText(context, pasteEmpty, Toast.LENGTH_SHORT).show()
        } else {
            viewModel.setMode(AddLoadInputMode.MANUAL)
            viewModel.setRawText(text)
            pasteToken += 1
        }
    }
    LaunchedEffect(uiState.previewLoad?.id, pasteToken) {
        val preview = uiState.previewLoad ?: return@LaunchedEffect
        if (pasteToken == 0) return@LaunchedEffect
        applyPastedPreview(viewModel, preview)
    }
    LaunchedEffect(uiState.savedLoad?.id, uiState.alarmPrompt != null) {
        if (uiState.savedLoad == null) return@LaunchedEffect
        if (uiState.alarmPrompt != null) {
            sawAlarm = true
            return@LaunchedEffect
        }
        if (motion && !sawAlarm) delay(SAVE_CONFIRM_HOLD_MS)
        viewModel.clearSaved()
        onSaved()
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
                    saveErrorFormatter = { msg -> resources.getString(R.string.common_save_error, msg) },
                    onOptimisticInsert = onOptimisticInsert,
                )
            },
            onDismiss = viewModel::dismissIncompleteConfirm,
        )
    }

    Scaffold(
        modifier = Modifier.addLoadSharedBounds(),
        containerColor = BentoGlassTheme.ScreenBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            DashboardHero(
                title = stringResource(R.string.add_load_title),
                showBack = true,
                onBack = requestBack,
                actions = {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = androidx.compose.ui.graphics.Color.White,
                    ) {
                        Text(
                            text = weekChip,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                            color = com.truckerload.presentation.theme.LocalBrandPalette.current.labelOnWhite(),
                        )
                    }
                },
            )
        },
        bottomBar = {
            OneUiBottomActionBar {
                val saveEnabled = !uiState.isSaving && !uiState.isExtractingDocument && when (uiState.mode) {
                    AddLoadInputMode.MANUAL, AddLoadInputMode.DOCUMENT -> true
                    AddLoadInputMode.PASTE -> uiState.rawText.isNotBlank()
                }
                val confirmed = uiState.savedLoad != null
                Button(
                    onClick = { if (!confirmed) saveCurrent() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = saveEnabled || confirmed,
                ) {
                    SaveConfirmLabel(
                        confirmed = confirmed,
                        motion = motion,
                        label = if (uiState.isSaving) {
                            stringResource(R.string.add_load_saving)
                        } else {
                            stringResource(R.string.add_load_save_load)
                        },
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BentoGlassTheme.ScreenBackground)
                .padding(padding)
                .verticalContentScroll()
                .padding(horizontal = 24.dp, vertical = 8.dp),
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
                    onSubmit = saveCurrent,
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
                        showFieldErrors = uiState.showFieldErrors,
                        fieldErrorNonce = uiState.fieldErrorNonce,
                        onSubmit = saveCurrent,
                        equipment = uiState.equipmentType,
                        onEquipment = viewModel::setEquipmentType,
                        showPasteBanner = true,
                        onPaste = { pasteFromClipboard() },
                        onOpenDocuments = { viewModel.setMode(AddLoadInputMode.DOCUMENT) },
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
                            showFieldErrors = uiState.showFieldErrors,
                            fieldErrorNonce = uiState.fieldErrorNonce,
                            onSubmit = saveCurrent,
                            equipment = uiState.equipmentType,
                            onEquipment = viewModel::setEquipmentType,
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
            val showHistoryTip = shouldShowAddLoadHistoryTip(
                uiState.mode,
                uiState.rawText,
                uiState.isExtractingDocument,
            )
            if (showHistoryTip) {
                AddLoadHistoryTip()
            } else if (uiState.mode != AddLoadInputMode.PASTE) {
                Text(
                    stringResource(
                        when (uiState.mode) {
                            AddLoadInputMode.MANUAL -> R.string.add_load_hint_manual
                            AddLoadInputMode.DOCUMENT -> R.string.add_load_hint_document
                            AddLoadInputMode.PASTE -> R.string.add_load_hint_paste
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            uiState.error?.let {
                Text(it, color = tc.AccentExpense, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun PasteLoadCard(
    rawText: String,
    onRawText: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    OutlinedTextField(
        value = rawText,
        onValueChange = onRawText,
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        label = { Text(stringResource(R.string.add_load_input_label)) },
        placeholder = { Text(stringResource(R.string.add_load_input_placeholder)) },
        keyboardOptions = formKeyboard(KeyboardType.Text, done = true),
        keyboardActions = formKeyboardActions(onDone = { if (rawText.isNotBlank()) onSubmit() }),
        shape = RoundedCornerShape(BentoGlassTheme.CellRadius),
        colors = AppTextFieldDefaults.outlined(),
    )
}

@Composable
private fun SaveConfirmLabel(confirmed: Boolean, motion: Boolean, label: String) {
    Crossfade(
        targetState = confirmed,
        animationSpec = if (motion) tween(durationMillis = 180) else snap(),
        label = "save-confirm",
    ) { done ->
        Box(contentAlignment = Alignment.Center) {
            if (done) {
                Icon(
                    imageVector = AppIcons.CheckCircle,
                    contentDescription = stringResource(R.string.add_load_saved_ack),
                    modifier = Modifier.size(22.dp),
                )
            } else {
                Text(label)
            }
        }
    }
}

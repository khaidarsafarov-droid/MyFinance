package com.truckerload.presentation.screens.add

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.data.repository.AiRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.model.EquipmentType
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.normalizeTripId
import com.truckerload.domain.parser.LoadCompleteness
import com.truckerload.domain.parser.ManualLoadFactory
import com.truckerload.domain.parser.PasteParseGap
import com.truckerload.domain.parser.PasteParseHint
import com.truckerload.sync.LoadAlarmPlanner
import com.truckerload.sync.LoadAlarmScheduler
import com.truckerload.utils.LoadDocumentTextExtractor
import com.truckerload.utils.getFirstPickUpMillis
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LoadAlarmPromptState(
    val loadId: String,
    val tripId: String,
    val pickupMillis: Long,
    val availablePresets: List<LoadAlarmPlanner.Preset>,
    val showCustomPicker: Boolean = false,
    val customError: String? = null,
)

data class AddLoadUiState(
    val mode: AddLoadInputMode = AddLoadInputMode.PASTE,
    val rawText: String = "",
    val manual: ManualLoadFields = ManualLoadFields(),
    val documentName: String? = null,
    val isExtractingDocument: Boolean = false,
    val error: String? = null,
    val isSaving: Boolean = false,
    val savedLoad: Load? = null,
    val alarmPrompt: LoadAlarmPromptState? = null,
    val previewLoad: Load? = null,
    val isParsingPreview: Boolean = false,
    val previewHint: String? = null,
    val equipmentType: EquipmentType? = null,
    /** Gaps in the imported draft; drives the review card. */
    val completeness: LoadCompleteness? = null,
    /** Set when save is held back until the driver confirms an incomplete load. */
    val confirmIncomplete: LoadCompleteness? = null,
)

@HiltViewModel
class AddLoadViewModel @Inject constructor(
    application: Application,
    private val loadRepository: LoadRepository,
    private val aiRepository: AiRepository,
    private val settingsDataStore: SettingsDataStore,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        AddLoadUiState(
            rawText = savedStateHandle[KEY_RAW] ?: "",
            manual = ManualLoadFields(date = todayIso()),
        ),
    )
    val uiState: StateFlow<AddLoadUiState> = _uiState.asStateFlow()

    private var previewJob: Job? = null

    init {
        val initial = _uiState.value.rawText
        if (initial.isNotBlank()) schedulePreview(initial)
        viewModelScope.launch {
            val last = settingsDataStore.getLastEquipmentTypeOnce()
            if (last != null && _uiState.value.equipmentType == null) {
                _uiState.update { it.copy(equipmentType = last) }
            }
        }
    }

    fun setEquipmentType(type: EquipmentType?) {
        _uiState.update { it.copy(equipmentType = type) }
    }

    fun setMode(mode: AddLoadInputMode) {
        _uiState.update { it.copy(mode = mode, error = null) }
    }

    fun setRawText(value: String) {
        savedStateHandle[KEY_RAW] = value
        _uiState.update {
            it.copy(
                rawText = value,
                error = null,
                previewLoad = null,
                previewHint = null,
                isParsingPreview = value.isNotBlank(),
            )
        }
        schedulePreview(value)
    }

    fun setManualTripId(value: String) = updateManual { it.copy(tripId = value) }
    fun setManualDate(value: String) = updateManual { it.copy(date = value) }
    fun setManualRate(value: String) = updateManual { it.copy(rate = value) }
    fun setManualMiles(value: String) = updateManual { it.copy(miles = value) }
    fun setManualPoint(index: Int, value: String) = updateManual { it.withPoint(index, value) }
    fun addManualPoint() = updateManual { it.addPoint() }
    fun removeManualPoint(index: Int) = updateManual { it.removePoint(index) }

    fun importDocument(uri: Uri, mimeType: String?) {
        if (_uiState.value.isExtractingDocument) return
        val name = uri.lastPathSegment?.substringAfterLast('/') ?: mimeType
        _uiState.update {
            it.copy(
                isExtractingDocument = true,
                documentName = name,
                error = null,
                completeness = null,
                confirmIncomplete = null,
            )
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                LoadDocumentTextExtractor(getApplication()).extract(uri, mimeType)
            }
            result
                .onSuccess { text -> applyDocumentText(text, name) }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            error = getApplication<Application>().getString(
                                com.truckerload.R.string.add_load_document_failed,
                            ),
                        )
                    }
                }
            _uiState.update { it.copy(isExtractingDocument = false) }
        }
    }

    fun save(
        parseFailedFallback: String,
        saveErrorFormatter: (String) -> String,
        onOptimisticInsert: ((Load) -> Unit)?,
    ) {
        // FIX: atomic isSaving gate — two taps were able to insert two loads
        var accepted = false
        _uiState.update { state ->
            if (state.isSaving) return@update state
            accepted = true
            state.copy(isSaving = true, error = null)
        }
        if (!accepted) return
        if (_uiState.value.mode == AddLoadInputMode.MANUAL ||
            _uiState.value.mode == AddLoadInputMode.DOCUMENT
        ) {
            saveManual(saveErrorFormatter, onOptimisticInsert)
            return
        }
        saveParsed(parseFailedFallback, saveErrorFormatter, onOptimisticInsert)
    }

    fun clearSaved() {
        _uiState.update { it.copy(savedLoad = null, alarmPrompt = null) }
    }

    fun dismissAlarmPrompt() {
        _uiState.update { it.copy(alarmPrompt = null) }
    }

    fun showCustomAlarmPicker() {
        _uiState.update { state ->
            val prompt = state.alarmPrompt ?: return
            state.copy(alarmPrompt = prompt.copy(showCustomPicker = true, customError = null))
        }
    }

    fun hideCustomAlarmPicker() {
        _uiState.update { state ->
            val prompt = state.alarmPrompt ?: return
            state.copy(alarmPrompt = prompt.copy(showCustomPicker = false, customError = null))
        }
    }

    fun schedulePresetAlarm(preset: LoadAlarmPlanner.Preset): Boolean {
        val prompt = _uiState.value.alarmPrompt ?: return false
        return scheduleAlarm(LoadAlarmPlanner.triggerAt(prompt.pickupMillis, preset.hoursBefore))
    }

    fun scheduleCustomAlarm(triggerAtMillis: Long, invalidTimeMessage: String): Boolean {
        val prompt = _uiState.value.alarmPrompt ?: return false
        val now = System.currentTimeMillis()
        if (!LoadAlarmPlanner.isValidAlarmTime(triggerAtMillis, prompt.pickupMillis, now)) {
            _uiState.update { it.copy(alarmPrompt = prompt.copy(customError = invalidTimeMessage)) }
            return false
        }
        return scheduleAlarm(triggerAtMillis)
    }

    private fun applyDocumentText(text: String, fileName: String? = null) {
        savedStateHandle[KEY_RAW] = text
        previewJob?.cancel()
        val draft = aiRepository.extractLoadFields(text, fileName)
        val fallbackDate = _uiState.value.manual.date.ifBlank { todayIso() }
        val fields = ManualLoadFields.fromDraft(draft, fallbackDate)
        _uiState.update {
            it.copy(
                rawText = text,
                error = null,
                previewLoad = null,
                previewHint = null,
                isParsingPreview = false,
                manual = fields,
                completeness = fields.completeness(),
                confirmIncomplete = null,
            )
        }
    }

    private fun updateManual(transform: (ManualLoadFields) -> ManualLoadFields) {
        _uiState.update { state ->
            val manual = transform(state.manual)
            state.copy(
                manual = manual,
                error = null,
                completeness = if (state.completeness == null) null else manual.completeness(),
                confirmIncomplete = null,
            )
        }
    }

    /** Saves a draft the driver acknowledged as incomplete. */
    fun confirmIncompleteSave(
        saveErrorFormatter: (String) -> String,
        onOptimisticInsert: ((Load) -> Unit)?,
    ) {
        if (_uiState.value.confirmIncomplete == null) return
        _uiState.update { it.copy(confirmIncomplete = null, isSaving = true, error = null) }
        saveManual(saveErrorFormatter, onOptimisticInsert, skipConfirmation = true)
    }

    fun dismissIncompleteConfirm() {
        _uiState.update { it.copy(confirmIncomplete = null, isSaving = false) }
    }

    private fun schedulePreview(value: String) {
        previewJob?.cancel()
        if (value.isBlank()) {
            _uiState.update { it.copy(previewLoad = null, isParsingPreview = false, previewHint = null) }
            return
        }
        previewJob = viewModelScope.launch {
            delay(PREVIEW_DEBOUNCE_MS)
            _uiState.update { it.copy(isParsingPreview = true, previewHint = null) }
            aiRepository.parseLoadFromUserInput(value)
                .onSuccess { load ->
                    _uiState.update {
                        it.copy(previewLoad = load, isParsingPreview = false, previewHint = null)
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            previewLoad = null,
                            isParsingPreview = false,
                            previewHint = if (value.length < MIN_PREVIEW_CHARS) {
                                null
                            } else {
                                getApplication<Application>().getString(previewHintRes(value))
                            },
                        )
                    }
                }
        }
    }

    private fun saveManual(
        saveErrorFormatter: (String) -> String,
        onOptimisticInsert: ((Load) -> Unit)?,
        skipConfirmation: Boolean = false,
    ) {
        val fields = _uiState.value.manual
        val rate = fields.parsedRate() ?: 0.0
        if (rate <= 0.0) {
            _uiState.update {
                it.copy(
                    isSaving = false,
                    error = getApplication<Application>().getString(com.truckerload.R.string.add_load_manual_rate_required),
                )
            }
            return
        }
        if (fields.filledPoints().isEmpty()) {
            _uiState.update {
                it.copy(
                    isSaving = false,
                    error = getApplication<Application>().getString(com.truckerload.R.string.add_load_manual_address_required),
                )
            }
            return
        }
        val completeness = fields.completeness()
        if (!skipConfirmation && completeness.needsConfirmation) {
            _uiState.update {
                it.copy(isSaving = false, confirmIncomplete = completeness, completeness = completeness)
            }
            return
        }
        persistLoad(
            ManualLoadFactory.build(
                tripId = fields.tripId,
                date = fields.date,
                rate = rate,
                miles = fields.parsedMiles(),
                pointA = fields.pointA,
                pointB = fields.pointB,
                extraPoints = fields.extraPoints,
                rawMessage = if (_uiState.value.mode == AddLoadInputMode.DOCUMENT) {
                    _uiState.value.rawText
                } else {
                    ""
                },
                equipmentType = _uiState.value.equipmentType,
            ),
            saveErrorFormatter,
            onOptimisticInsert,
        )
    }

    private fun saveParsed(
        parseFailedFallback: String,
        saveErrorFormatter: (String) -> String,
        onOptimisticInsert: ((Load) -> Unit)?,
    ) {
        val text = _uiState.value.rawText
        if (text.isBlank()) {
            _uiState.update { it.copy(isSaving = false) }
            return
        }
        viewModelScope.launch {
            val cached = _uiState.value.previewLoad
            val parseResult = if (cached != null && cached.rawMessage == text) {
                Result.success(cached)
            } else {
                aiRepository.parseLoadFromUserInput(text)
            }
            parseResult
                .onSuccess { persistLoad(it, saveErrorFormatter, onOptimisticInsert) }
                .onFailure { fallBackToReview(text, parseFailedFallback) }
        }
    }

    /**
     * Pasted text that does not parse into a full load is not an error: keep whatever
     * fields were recognized and let the driver complete and confirm them.
     */
    private fun fallBackToReview(text: String, parseFailedFallback: String) {
        val draft = aiRepository.extractLoadFields(text)
        if (draft.isEmpty()) {
            _uiState.update { it.copy(isSaving = false, error = parseFailedFallback) }
            return
        }
        val fields = ManualLoadFields.fromDraft(draft, _uiState.value.manual.date.ifBlank { todayIso() })
        _uiState.update {
            it.copy(
                isSaving = false,
                mode = AddLoadInputMode.MANUAL,
                manual = fields,
                completeness = fields.completeness(),
                confirmIncomplete = null,
                error = getApplication<Application>().getString(
                    com.truckerload.R.string.add_load_review_needed,
                ),
            )
        }
    }

    private fun persistLoad(
        load: Load,
        saveErrorFormatter: (String) -> String,
        onOptimisticInsert: ((Load) -> Unit)?,
    ) {
        viewModelScope.launch {
            try {
                val typed = load.copy(equipmentType = load.equipmentType ?: _uiState.value.equipmentType)
                // FIX: same Trip ID must update (unique tripId) instead of crashing / orphaning stops
                val tripId = typed.tripId.trim()
                val existing = if (tripId.isNotBlank()) {
                    loadRepository.getByTripId(tripId)
                        ?: loadRepository.getByTripId(normalizeTripId(tripId))
                } else {
                    null
                }
                val persisted = if (existing != null) {
                    val updated = typed.copy(id = existing.id, parsedAt = existing.parsedAt)
                    loadRepository.updateLoad(updated)
                    updated
                } else {
                    loadRepository.insertLoad(typed)
                    typed
                }
                // FIX: alarm / journal must use the Room row id after an update
                onOptimisticInsert?.invoke(persisted)
                persisted.equipmentType?.let { settingsDataStore.saveLastEquipmentType(it) }
                savedStateHandle[KEY_RAW] = ""
                val pickup = getFirstPickUpMillis(persisted)
                val offer = pickup?.let { LoadAlarmPlanner.buildOffer(it, System.currentTimeMillis()) }
                val alarmPrompt = if (offer != null && offer.canOffer) {
                    LoadAlarmPromptState(
                        loadId = persisted.id,
                        tripId = persisted.tripId,
                        pickupMillis = offer.pickupMillis,
                        availablePresets = offer.availablePresets,
                    )
                } else {
                    null
                }
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        savedLoad = persisted,
                        rawText = "",
                        manual = ManualLoadFields(date = todayIso()),
                        documentName = null,
                        alarmPrompt = alarmPrompt,
                        previewLoad = null,
                        previewHint = null,
                        isParsingPreview = false,
                        completeness = null,
                        confirmIncomplete = null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, error = saveErrorFormatter(e.message.orEmpty()))
                }
            }
        }
    }

    private fun scheduleAlarm(triggerAtMillis: Long): Boolean {
        val prompt = _uiState.value.alarmPrompt ?: return false
        val ok = LoadAlarmScheduler.schedule(
            context = getApplication(),
            loadId = prompt.loadId,
            tripId = prompt.tripId,
            triggerAtMillis = triggerAtMillis,
            pickupMillis = prompt.pickupMillis,
        )
        if (ok) _uiState.update { it.copy(alarmPrompt = null) }
        return ok
    }

    private fun todayIso(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun previewHintRes(text: String): Int = when (PasteParseHint.of(text)) {
        PasteParseGap.MISSING_RATE -> com.truckerload.R.string.add_load_preview_missing_rate
        PasteParseGap.MISSING_ADDRESS -> com.truckerload.R.string.add_load_preview_missing_address
        PasteParseGap.MISSING_BOTH -> com.truckerload.R.string.add_load_preview_missing_both
        PasteParseGap.INCOMPLETE -> com.truckerload.R.string.add_load_preview_incomplete
    }

    companion object {
        private const val KEY_RAW = "add_load_raw_text"
        private const val PREVIEW_DEBOUNCE_MS = 450L
        private const val MIN_PREVIEW_CHARS = 40
    }
}

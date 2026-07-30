package com.truckerload.presentation.screens.add

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.truckerload.data.repository.AiRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.model.Load
import com.truckerload.sync.LoadAlarmPlanner
import com.truckerload.sync.LoadAlarmScheduler
import com.truckerload.utils.getFirstPickUpMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoadAlarmPromptState(
    val loadId: String,
    val tripId: String,
    val pickupMillis: Long,
    val availablePresets: List<LoadAlarmPlanner.Preset>,
    val showCustomPicker: Boolean = false,
    val customError: String? = null,
)

data class AddLoadUiState(
    val rawText: String = "",
    val error: String? = null,
    val isSaving: Boolean = false,
    val savedLoad: Load? = null,
    val alarmPrompt: LoadAlarmPromptState? = null,
)

@HiltViewModel
class AddLoadViewModel @Inject constructor(
    application: Application,
    private val loadRepository: LoadRepository,
    private val aiRepository: AiRepository,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        AddLoadUiState(rawText = savedStateHandle[KEY_RAW] ?: ""),
    )
    val uiState: StateFlow<AddLoadUiState> = _uiState.asStateFlow()

    fun setRawText(value: String) {
        savedStateHandle[KEY_RAW] = value
        _uiState.update { it.copy(rawText = value, error = null) }
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
        val triggerAt = LoadAlarmPlanner.triggerAt(prompt.pickupMillis, preset.hoursBefore)
        return scheduleAlarm(triggerAt)
    }

    fun scheduleCustomAlarm(triggerAtMillis: Long, invalidTimeMessage: String): Boolean {
        val prompt = _uiState.value.alarmPrompt ?: return false
        val now = System.currentTimeMillis()
        if (!LoadAlarmPlanner.isValidAlarmTime(triggerAtMillis, prompt.pickupMillis, now)) {
            _uiState.update {
                it.copy(alarmPrompt = prompt.copy(customError = invalidTimeMessage))
            }
            return false
        }
        return scheduleAlarm(triggerAtMillis)
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
        if (ok) {
            _uiState.update { it.copy(alarmPrompt = null) }
        }
        return ok
    }

    fun save(
        parseFailedFallback: String,
        saveErrorFormatter: (String) -> String,
        onOptimisticInsert: ((Load) -> Unit)?,
    ) {
        val text = _uiState.value.rawText
        if (text.isBlank() || _uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            aiRepository.parseLoadFromMessage(text)
                .onSuccess { load ->
                    try {
                        loadRepository.insertLoad(load)
                        onOptimisticInsert?.invoke(load)
                        savedStateHandle[KEY_RAW] = ""
                        val now = System.currentTimeMillis()
                        val pickup = getFirstPickUpMillis(load)
                        val offer = pickup?.let { LoadAlarmPlanner.buildOffer(it, now) }
                        val alarmPrompt = if (offer != null && offer.canOffer) {
                            LoadAlarmPromptState(
                                loadId = load.id,
                                tripId = load.tripId,
                                pickupMillis = offer.pickupMillis,
                                availablePresets = offer.availablePresets,
                            )
                        } else {
                            null
                        }
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                savedLoad = load,
                                rawText = "",
                                alarmPrompt = alarmPrompt,
                            )
                        }
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                error = saveErrorFormatter(e.message.orEmpty()),
                            )
                        }
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = err.message ?: parseFailedFallback,
                        )
                    }
                }
        }
    }

    companion object {
        private const val KEY_RAW = "add_load_raw_text"
    }
}

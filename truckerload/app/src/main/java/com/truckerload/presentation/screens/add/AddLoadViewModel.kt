package com.truckerload.presentation.screens.add

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.truckerload.data.repository.AiRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.model.Load
import com.truckerload.presentation.components.PickupAlarmPrompt
import com.truckerload.sync.PickupAlarmPlanner
import com.truckerload.utils.getFirstPickUpMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddLoadUiState(
    val rawText: String = "",
    val error: String? = null,
    val isSaving: Boolean = false,
    val savedLoad: Load? = null,
    val alarmPrompt: PickupAlarmPrompt? = null,
)

class AddLoadViewModel(
    application: Application,
    private val loadRepository: LoadRepository,
    private val aiRepository: AiRepository?,
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
        _uiState.update { it.copy(savedLoad = null) }
    }

    fun clearAlarmPrompt() {
        _uiState.update { it.copy(alarmPrompt = null) }
    }

    fun save(
        parseFailedFallback: String,
        saveErrorFormatter: (String) -> String,
        onOptimisticInsert: ((Load) -> Unit)?,
    ) {
        val text = _uiState.value.rawText
        val ai = aiRepository ?: return
        if (text.isBlank() || _uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            ai.parseLoadFromMessage(text)
                .onSuccess { load ->
                    try {
                        loadRepository.insertLoad(load)
                        onOptimisticInsert?.invoke(load)
                        savedStateHandle[KEY_RAW] = ""
                        val pickupMillis = getFirstPickUpMillis(load)
                        val now = System.currentTimeMillis()
                        val alarmPrompt = if (PickupAlarmPlanner.shouldPromptForAlarm(pickupMillis, now)) {
                            PickupAlarmPrompt(
                                loadId = load.id,
                                tripId = load.tripId,
                                pickupMillis = pickupMillis!!,
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

    class Factory(
        private val application: Application,
        private val loadRepository: LoadRepository,
        private val aiRepository: AiRepository?,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            AddLoadViewModel(
                application,
                loadRepository,
                aiRepository,
                extras.createSavedStateHandle(),
            ) as T
    }

    companion object {
        private const val KEY_RAW = "add_load_raw_text"
    }
}

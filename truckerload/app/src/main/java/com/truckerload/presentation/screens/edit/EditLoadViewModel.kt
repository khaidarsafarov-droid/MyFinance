package com.truckerload.presentation.screens.edit

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.truckerload.R
import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.lastDelDateFromStops
import com.truckerload.domain.model.withRouteMetrics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditLoadUiState(
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val original: Load? = null,
    val tripId: String = "",
    val loadDate: String = "",
    /** Дата завершения груза (YYYY-MM-DD); по умолчанию — последний DEL. */
    val finishDate: String = "",
    val lastDelDate: String? = null,
    val totalRate: String = "",
    val totalMiles: String = "",
    val pointA: String = "",
    val pointB: String = "",
    val disputeLoad: Load? = null,
    val focusFinish: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class EditLoadViewModel @Inject constructor(
    application: Application,
    private val loadRepository: LoadRepository,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val loadId = Uri.decode(savedStateHandle.get<String>("loadId").orEmpty())
    private val focusFinish = savedStateHandle.get<Boolean>("focusFinish") ?: false

    private val _uiState = MutableStateFlow(EditLoadUiState(focusFinish = focusFinish))
    val uiState: StateFlow<EditLoadUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun setTripId(value: String) {
        savedStateHandle[KEY_TRIP] = value
        _uiState.update { it.copy(tripId = value, saveError = null) }
    }

    fun setLoadDate(value: String) {
        savedStateHandle[KEY_DATE] = value
        _uiState.update { it.copy(loadDate = value, saveError = null) }
    }

    fun setFinishDate(value: String) {
        savedStateHandle[KEY_FINISH] = value
        _uiState.update { it.copy(finishDate = value, saveError = null) }
    }

    fun setTotalRate(value: String) {
        savedStateHandle[KEY_RATE] = value
        _uiState.update { it.copy(totalRate = value, saveError = null) }
    }

    fun setTotalMiles(value: String) {
        savedStateHandle[KEY_MILES] = value
        _uiState.update { it.copy(totalMiles = value, saveError = null) }
    }

    fun setPointA(value: String) {
        savedStateHandle[KEY_A] = value
        _uiState.update { it.copy(pointA = value, saveError = null) }
    }

    fun setPointB(value: String) {
        savedStateHandle[KEY_B] = value
        _uiState.update { it.copy(pointB = value, saveError = null) }
    }

    fun setDisputeLoad(updated: Load) {
        _uiState.update { it.copy(disputeLoad = updated, saveError = null) }
    }

    fun clearSaved() {
        _uiState.update { it.copy(saved = false) }
    }

    fun save(
        saveErrorFormatter: (String) -> String,
        onOptimisticUpdate: ((Load) -> Unit)?,
    ) {
        val state = _uiState.value
        val original = state.original ?: return
        if (state.isSaving) return
        val parsedRate = state.totalRate.toDoubleOrNull()
        val parsedMiles = state.totalMiles.toDoubleOrNull()
        if (parsedRate == null || parsedMiles == null) {
            _uiState.update {
                it.copy(saveError = getApplication<Application>().getString(R.string.edit_load_invalid_number))
            }
            return
        }
        val finishIso = state.finishDate.trim().takeIf { it.length >= 10 }?.take(10)
        val lastDel = state.lastDelDate
        // Persist override only when it differs from last DEL (or DEL unknown).
        // Empty field clears the override and falls back to stops.
        val actualFinish = when {
            finishIso.isNullOrBlank() -> null
            lastDel != null && finishIso == lastDel -> null
            else -> finishIso
        }
        val updated = (state.disputeLoad ?: original).copy(
            tripId = state.tripId.ifBlank { original.tripId },
            date = state.loadDate.ifBlank { original.date },
            totalRate = parsedRate,
            totalMiles = parsedMiles,
            pointA = state.pointA,
            pointB = state.pointB,
            actualFinishDate = actualFinish,
            updatedAt = System.currentTimeMillis(),
        ).withRouteMetrics()
        _uiState.update { it.copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            try {
                loadRepository.updateLoad(updated)
                val reloaded = loadRepository.getLoadById(loadId)?.withRouteMetrics() ?: updated
                onOptimisticUpdate?.invoke(reloaded)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saved = true,
                        original = reloaded,
                        finishDate = reloaded.actualFinishDate
                            ?: reloaded.lastDelDateFromStops().orEmpty(),
                        lastDelDate = reloaded.lastDelDateFromStops(),
                        disputeLoad = reloaded,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveError = saveErrorFormatter(e.message.orEmpty()),
                    )
                }
            }
        }
    }

    private fun load() {
        val app = getApplication<Application>()
        if (loadId.isBlank()) {
            _uiState.update {
                it.copy(isLoading = false, loadError = app.getString(R.string.load_invalid))
            }
            return
        }
        viewModelScope.launch {
            try {
                val loaded = loadRepository.getLoadById(loadId)?.withRouteMetrics()
                if (loaded == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadError = app.getString(R.string.load_detail_not_found),
                        )
                    }
                    return@launch
                }
                val lastDel = loaded.lastDelDateFromStops()
                val tripId = savedStateHandle[KEY_TRIP] ?: loaded.tripId
                val loadDate = savedStateHandle[KEY_DATE] ?: loaded.date
                // Prefill finish with saved override, else last DEL (auto from stops).
                val finishDate = savedStateHandle[KEY_FINISH]
                    ?: loaded.actualFinishDate?.takeIf { it.length >= 10 }?.take(10)
                    ?: lastDel.orEmpty()
                val totalRate = savedStateHandle[KEY_RATE] ?: loaded.totalRate.toString()
                val totalMiles = savedStateHandle[KEY_MILES] ?: loaded.totalMiles.toString()
                val pointA = savedStateHandle[KEY_A] ?: loaded.pointA
                val pointB = savedStateHandle[KEY_B] ?: loaded.pointB
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        original = loaded,
                        tripId = tripId,
                        loadDate = loadDate,
                        finishDate = finishDate,
                        lastDelDate = lastDel,
                        totalRate = totalRate,
                        totalMiles = totalMiles,
                        pointA = pointA,
                        pointB = pointB,
                        disputeLoad = loaded,
                        focusFinish = focusFinish,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, loadError = e.message)
                }
            }
        }
    }

    companion object {
        private const val KEY_TRIP = "edit_trip_id"
        private const val KEY_DATE = "edit_load_date"
        private const val KEY_FINISH = "edit_finish_date"
        private const val KEY_RATE = "edit_total_rate"
        private const val KEY_MILES = "edit_total_miles"
        private const val KEY_A = "edit_point_a"
        private const val KEY_B = "edit_point_b"
    }
}

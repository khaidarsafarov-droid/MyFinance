package com.truckerload.presentation.screens.edit
import android.database.sqlite.SQLiteException
import java.io.IOException


import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.truckerload.R
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.model.ActualFinishDate
import com.truckerload.domain.model.DisputePayout
import com.truckerload.domain.model.EquipmentType
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.lastDelDateFromStops
import com.truckerload.domain.model.lastDelDateTimeFromStops
import com.truckerload.domain.model.withRouteMetrics
import com.truckerload.domain.parser.DecimalInput
import com.truckerload.presentation.screens.add.FormDraft
import com.truckerload.utils.FieldErrors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class EditLoadUiState(
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val original: Load? = null,
    val tripId: String = "",
    val loadDate: String = "",
    /** Finish override: `YYYY-MM-DD` or `YYYY-MM-DD HH:mm`. */
    val finishDate: String = "",
    val lastDelDate: String? = null,
    val totalRate: String = "",
    val totalMiles: String = "",
    val pointA: String = "",
    val pointB: String = "",
    val equipmentType: EquipmentType? = null,
    val disputeLoad: Load? = null,
    val focusFinish: Boolean = false,
    val focusMiles: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val saved: Boolean = false,
    val showFieldErrors: Boolean = false,
    val fieldErrorNonce: Int = 0,
    val draftBaseline: String? = null,
) {
    fun draftSignature(): String = FormDraft.editLoad(
        tripId = tripId,
        loadDate = loadDate,
        finishDate = finishDate,
        totalRate = totalRate,
        totalMiles = totalMiles,
        pointA = pointA,
        pointB = pointB,
        equipment = equipmentType?.name.orEmpty(),
        dispute = disputeLoad,
    )

    val isDirty: Boolean
        get() = FormDraft.isDirty(draftBaseline, draftSignature())
}

@HiltViewModel
class EditLoadViewModel @Inject constructor(
    application: Application,
    private val loadRepository: LoadRepository,
    private val settingsDataStore: SettingsDataStore,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val loadId = Uri.decode(savedStateHandle.get<String>("loadId").orEmpty())
    private val focusFinish = savedStateHandle.get<Boolean>("focusFinish") ?: false
    private val focusMiles = savedStateHandle.get<Boolean>("focusMiles") ?: false

    private val _uiState = MutableStateFlow(
        EditLoadUiState(focusFinish = focusFinish, focusMiles = focusMiles),
    )
    val uiState: StateFlow<EditLoadUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null
    private var loadGeneration = 0

    init {
        load()
    }

    fun refresh() = load()

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

    fun setFinishDatePart(isoDate: String) {
        val date = isoDate.trim().take(10)
        if (date.length < 10) return
        val current = _uiState.value.finishDate
        val time = ActualFinishDate.timeParts(current)
        val next = if (time != null) {
            ActualFinishDate.combine(date, time.first, time.second)
        } else {
            // Picker path: keep a clock so early finish today drops the route immediately.
            ActualFinishDate.combineNow(date)
        }
        setFinishDate(next)
    }

    fun setFinishTime(hour: Int, minute: Int) {
        val date = ActualFinishDate.datePart(_uiState.value.finishDate)
            ?: LocalDate.now().toString()
        setFinishDate(ActualFinishDate.combine(date, hour, minute))
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

    fun setEquipmentType(type: EquipmentType?) {
        _uiState.update { it.copy(equipmentType = type, saveError = null) }
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
        val parsedRate = DecimalInput.parse(state.totalRate)
        val parsedMiles = DecimalInput.parse(state.totalMiles)
        val rateInvalid = FieldErrors.amountRes(state.totalRate) != null
        if (parsedRate == null || parsedMiles == null || parsedMiles < 0.0 || rateInvalid) {
            _uiState.update {
                it.copy(
                    saveError = getApplication<Application>().getString(R.string.edit_load_invalid_number),
                    showFieldErrors = true,
                    fieldErrorNonce = it.fieldErrorNonce + 1,
                )
            }
            return
        }
        val actualFinish = ActualFinishDate.normalize(state.finishDate)
        val merged = (state.disputeLoad ?: original).copy(
            tripId = state.tripId.ifBlank { original.tripId },
            date = state.loadDate.ifBlank { original.date },
            totalRate = com.truckerload.domain.money.Money.fromDollars(parsedRate),
            totalMiles = com.truckerload.domain.units.MeasurementUnits.storedDistance(parsedMiles),
            pointA = state.pointA,
            pointB = state.pointB,
            equipmentType = state.equipmentType,
            actualFinishDate = actualFinish,
            updatedAt = System.currentTimeMillis(),
        )
        val updated = DisputePayout.settleFrom(original, merged).withRouteMetrics()
        _uiState.update { it.copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            try {
                loadRepository.updateLoad(updated)
                updated.equipmentType?.let { settingsDataStore.saveLastEquipmentType(it) }
                val reloaded = loadRepository.getLoadById(loadId)?.withRouteMetrics() ?: updated
                onOptimisticUpdate?.invoke(reloaded)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saved = true,
                        original = reloaded,
                        finishDate = reloaded.actualFinishDate
                            ?: reloaded.lastDelDateTimeFromStops().orEmpty(),
                        lastDelDate = reloaded.lastDelDateFromStops(),
                        disputeLoad = reloaded,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: SQLiteException) {
                _uiState.update {
                    it.copy(saveError = saveErrorFormatter(e.message.orEmpty()))
                }
            
            } catch (e: IOException) {
                _uiState.update {
                    it.copy(saveError = saveErrorFormatter(e.message.orEmpty()))
                }
            
            } catch (e: IllegalArgumentException) {
                _uiState.update {
                    it.copy(saveError = saveErrorFormatter(e.message.orEmpty()))
                }
            
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalStateException) {
                _uiState.update {
                    it.copy(saveError = saveErrorFormatter(e.message.orEmpty()))
                }
            
            } finally {
                _uiState.update { it.copy(isSaving = false) }
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
        loadJob?.cancel()
        val generation = ++loadGeneration
        loadJob = viewModelScope.launch {
            try {
                val loaded = loadRepository.getLoadById(loadId)?.withRouteMetrics()
                if (generation != loadGeneration) return@launch
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
                val finishDate = savedStateHandle[KEY_FINISH]
                    ?: ActualFinishDate.normalize(loaded.actualFinishDate)
                    ?: run {
                        val fromStops = loaded.lastDelDateTimeFromStops().orEmpty()
                        if (focusFinish && fromStops.length == 10) {
                            // «When did you finish?» — default clock to now on that day.
                            ActualFinishDate.combineNow(fromStops)
                        } else if (focusFinish && fromStops.isBlank()) {
                            ActualFinishDate.combineNow(LocalDate.now().toString())
                        } else {
                            fromStops
                        }
                    }
                val totalRate = savedStateHandle[KEY_RATE] ?: loaded.totalRate.toString()
                val totalMiles = savedStateHandle[KEY_MILES] ?: if (com.truckerload.domain.units.DisplayUnits.isMetric) {
                    com.truckerload.domain.units.MeasurementUnits.formatInput(
                        com.truckerload.domain.units.MeasurementUnits.displayDistance(loaded.totalMiles),
                    )
                } else {
                    loaded.totalMiles.toString()
                }
                val pointA = savedStateHandle[KEY_A] ?: loaded.pointA
                val pointB = savedStateHandle[KEY_B] ?: loaded.pointB
                _uiState.update { state ->
                    val opened = state.copy(
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
                        equipmentType = loaded.equipmentType,
                        disputeLoad = loaded,
                        focusFinish = focusFinish,
                    )
                    val baseline = savedStateHandle.get<String>(KEY_DRAFT) ?: opened.draftSignature().also {
                        savedStateHandle[KEY_DRAFT] = it
                    }
                    opened.copy(draftBaseline = baseline)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: SQLiteException) {
                if (generation != loadGeneration) return@launch
                _uiState.update {
                    it.copy(isLoading = false, loadError = e.message)
                }
            
            } catch (e: IOException) {
                if (generation != loadGeneration) return@launch
                _uiState.update {
                    it.copy(isLoading = false, loadError = e.message)
                }
            
            } catch (e: IllegalArgumentException) {
                if (generation != loadGeneration) return@launch
                _uiState.update {
                    it.copy(isLoading = false, loadError = e.message)
                }
            
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalStateException) {
                if (generation != loadGeneration) return@launch
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
        private const val KEY_DRAFT = "edit_load_draft_baseline"
    }
}

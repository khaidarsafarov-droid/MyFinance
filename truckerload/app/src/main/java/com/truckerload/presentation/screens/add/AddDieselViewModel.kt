package com.truckerload.presentation.screens.add

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.truckerload.R
import com.truckerload.data.repository.DieselRepository
import com.truckerload.domain.model.Diesel
import com.truckerload.utils.getCurrentWeekNumberAndYear
import com.truckerload.utils.getWeekNumberAndYearFromTimestamp
import com.truckerload.utils.getWeekRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

data class AddDieselUiState(
    val amountText: String = "",
    val recordedAtMillis: Long = System.currentTimeMillis(),
    val weekNumber: Int = 1,
    val year: Int = 1970,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
    val showSaveDialog: Boolean = false,
)

class AddDieselViewModel(
    application: Application,
    private val dieselRepository: DieselRepository,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val initialWeekAndYear = getCurrentWeekNumberAndYear()

    private val _uiState = MutableStateFlow(
        AddDieselUiState(
            amountText = savedStateHandle[KEY_AMOUNT_TEXT] ?: "",
            recordedAtMillis = savedStateHandle[KEY_RECORDED_AT_MILLIS] ?: System.currentTimeMillis(),
            weekNumber = savedStateHandle[KEY_WEEK_NUMBER] ?: initialWeekAndYear.first,
            year = savedStateHandle[KEY_YEAR] ?: initialWeekAndYear.second,
            showSaveDialog = savedStateHandle[KEY_SHOW_SAVE_DIALOG] ?: false,
        ),
    )
    val uiState: StateFlow<AddDieselUiState> = _uiState.asStateFlow()

    fun setAmountText(value: String) {
        savedStateHandle[KEY_AMOUNT_TEXT] = value
        _uiState.update { it.copy(amountText = value, error = null) }
    }

    fun selectPreviousWeek() {
        val state = _uiState.value
        if (state.weekNumber > 1) {
            setWeekAndYear(state.weekNumber - 1, state.year)
        } else {
            setWeekAndYear(52, state.year - 1)
        }
    }

    fun selectNextWeek() {
        val state = _uiState.value
        if (state.weekNumber < 52) {
            setWeekAndYear(state.weekNumber + 1, state.year)
        } else {
            setWeekAndYear(1, state.year + 1)
        }
    }

    fun openSaveDialog() {
        val amount = _uiState.value.amountText.toDoubleOrNull() ?: 0.0
        if (amount <= 0.0 || _uiState.value.isSaving) return

        val now = System.currentTimeMillis()
        savedStateHandle[KEY_RECORDED_AT_MILLIS] = now
        savedStateHandle[KEY_SHOW_SAVE_DIALOG] = true
        _uiState.update {
            it.copy(
                recordedAtMillis = now,
                error = null,
                showSaveDialog = true,
            )
        }
    }

    fun dismissSaveDialog() {
        savedStateHandle[KEY_SHOW_SAVE_DIALOG] = false
        _uiState.update { it.copy(showSaveDialog = false) }
    }

    fun setRecordedDate(selectedDateMillis: Long) {
        val current = Calendar.getInstance().apply {
            timeInMillis = _uiState.value.recordedAtMillis
        }
        val selected = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        current.set(Calendar.YEAR, selected.get(Calendar.YEAR))
        current.set(Calendar.MONTH, selected.get(Calendar.MONTH))
        current.set(Calendar.DAY_OF_MONTH, selected.get(Calendar.DAY_OF_MONTH))
        setRecordedAtMillis(current.timeInMillis)
    }

    fun setRecordedTime(hour: Int, minute: Int) {
        val current = Calendar.getInstance().apply {
            timeInMillis = _uiState.value.recordedAtMillis
        }
        current.set(Calendar.HOUR_OF_DAY, hour)
        current.set(Calendar.MINUTE, minute)
        setRecordedAtMillis(current.timeInMillis)
    }

    fun save() {
        val state = _uiState.value
        val amount = state.amountText.toDoubleOrNull() ?: 0.0
        if (amount <= 0.0 || state.isSaving) return

        val (weekNumber, year) = getWeekNumberAndYearFromTimestamp(state.recordedAtMillis)
        val (weekStart, weekEnd, weekLabel) = getWeekRange(weekNumber, year)
        val diesel = Diesel(
            id = 0,
            weekNumber = weekNumber,
            year = year,
            weekLabel = weekLabel,
            weekStartDate = weekStart,
            weekEndDate = weekEnd,
            totalAmount = amount,
            gallons = null,
            pricePerGallon = null,
            location = null,
            rawExtractedText = "",
            sourceFileName = null,
            addedAt = state.recordedAtMillis,
        )

        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    dieselRepository.insertDiesel(diesel)
                }
                savedStateHandle[KEY_AMOUNT_TEXT] = ""
                savedStateHandle[KEY_WEEK_NUMBER] = weekNumber
                savedStateHandle[KEY_YEAR] = year
                savedStateHandle[KEY_SHOW_SAVE_DIALOG] = false
                _uiState.update {
                    it.copy(
                        amountText = "",
                        weekNumber = weekNumber,
                        year = year,
                        isSaving = false,
                        saved = true,
                        showSaveDialog = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = e.message ?: getApplication<Application>().getString(R.string.common_save_failed),
                    )
                }
            }
        }
    }

    fun clearSaved() {
        _uiState.update { it.copy(saved = false) }
    }

    private fun setWeekAndYear(weekNumber: Int, year: Int) {
        savedStateHandle[KEY_WEEK_NUMBER] = weekNumber
        savedStateHandle[KEY_YEAR] = year
        _uiState.update {
            it.copy(
                weekNumber = weekNumber,
                year = year,
                error = null,
            )
        }
    }

    private fun setRecordedAtMillis(value: Long) {
        savedStateHandle[KEY_RECORDED_AT_MILLIS] = value
        _uiState.update { it.copy(recordedAtMillis = value, error = null) }
    }

    class Factory(
        private val application: Application,
        private val dieselRepository: DieselRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            AddDieselViewModel(
                application,
                dieselRepository,
                extras.createSavedStateHandle(),
            ) as T
    }

    companion object {
        private const val KEY_AMOUNT_TEXT = "add_diesel_amount_text"
        private const val KEY_RECORDED_AT_MILLIS = "add_diesel_recorded_at_millis"
        private const val KEY_WEEK_NUMBER = "add_diesel_week_number"
        private const val KEY_YEAR = "add_diesel_year"
        private const val KEY_SHOW_SAVE_DIALOG = "add_diesel_show_save_dialog"
    }
}

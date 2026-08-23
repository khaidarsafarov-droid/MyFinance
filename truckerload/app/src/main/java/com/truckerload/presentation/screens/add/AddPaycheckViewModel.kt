package com.truckerload.presentation.screens.add

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.truckerload.R
import com.truckerload.data.preferences.LastUsedDefaultsStore
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.domain.model.Paycheck
import com.truckerload.utils.AmountInputValidator
import com.truckerload.utils.getCurrentWeekNumberAndYear
import com.truckerload.utils.getMillisForWeek
import com.truckerload.utils.getWeekNumberAndYearFromTimestamp
import com.truckerload.utils.shiftWeekNumberAndYear
import com.truckerload.utils.getWeekRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

data class AddPaycheckUiState(
    val amountText: String = "",
    val recordedAtMillis: Long = System.currentTimeMillis(),
    val weekNumber: Int = 1,
    val year: Int = 1970,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
    val showSaveDialog: Boolean = false,
    val lastAmount: Double? = null,
)

@HiltViewModel
class AddPaycheckViewModel @Inject constructor(
    application: Application,
    private val paycheckRepository: PaycheckRepository,
    private val lastUsedDefaultsStore: LastUsedDefaultsStore,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val initialWeekAndYear = getCurrentWeekNumberAndYear()

    private val _uiState = MutableStateFlow(
        AddPaycheckUiState(
            amountText = savedStateHandle[KEY_AMOUNT_TEXT] ?: "",
            recordedAtMillis = savedStateHandle[KEY_RECORDED_AT_MILLIS] ?: System.currentTimeMillis(),
            weekNumber = savedStateHandle[KEY_WEEK_NUMBER] ?: initialWeekAndYear.first,
            year = savedStateHandle[KEY_YEAR] ?: initialWeekAndYear.second,
            showSaveDialog = savedStateHandle[KEY_SHOW_SAVE_DIALOG] ?: false,
            lastAmount = lastUsedDefaultsStore.lastPaycheckAmount.value,
        ),
    )
    val uiState: StateFlow<AddPaycheckUiState> = _uiState.asStateFlow()

    fun setAmountText(value: String) {
        savedStateHandle[KEY_AMOUNT_TEXT] = value
        _uiState.update { it.copy(amountText = value, error = null) }
    }

    fun applyLastAmount() {
        val last = _uiState.value.lastAmount ?: return
        val text = if (last % 1.0 == 0.0) last.toLong().toString()
        else String.format(Locale.US, "%.2f", last)
        setAmountText(text)
    }

    fun selectPreviousWeek() {
        val state = _uiState.value
        val (week, year) = shiftWeekNumberAndYear(state.weekNumber, state.year, -1)
        setWeekAndYear(week, year)
    }

    fun selectNextWeek() {
        val state = _uiState.value
        val (week, year) = shiftWeekNumberAndYear(state.weekNumber, state.year, 1)
        setWeekAndYear(week, year)
    }

    fun openSaveDialog() {
        if (_uiState.value.isSaving) return
        if (AmountInputValidator.parsePositiveAmount(_uiState.value.amountText) == null) {
            _uiState.update {
                it.copy(error = getApplication<Application>().getString(R.string.common_amount_must_be_positive))
            }
            return
        }
        savedStateHandle[KEY_SHOW_SAVE_DIALOG] = true
        _uiState.update {
            it.copy(
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
        val amount = AmountInputValidator.parsePositiveAmount(state.amountText)
        if (amount == null || state.isSaving) {
            if (amount == null) {
                _uiState.update {
                    it.copy(error = getApplication<Application>().getString(R.string.common_amount_must_be_positive))
                }
            }
            return
        }

        val weekNumber = state.weekNumber
        val year = state.year
        val (weekStart, weekEnd, weekLabel) = getWeekRange(weekNumber, year)
        val paycheck = Paycheck(
            id = 0,
            weekNumber = weekNumber,
            year = year,
            weekLabel = weekLabel,
            weekStartDate = weekStart,
            weekEndDate = weekEnd,
            driverName = null,
            grossAmount = null,
            netAmount = amount,
            rawExtractedText = "",
            sourceFileName = null,
            addedAt = state.recordedAtMillis,
        )

        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    paycheckRepository.insertPaycheck(paycheck)
                }
                lastUsedDefaultsStore.savePaycheckAmount(amount)
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
                        lastAmount = amount,
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
        val millis = getMillisForWeek(weekNumber, year)
        savedStateHandle[KEY_WEEK_NUMBER] = weekNumber
        savedStateHandle[KEY_YEAR] = year
        savedStateHandle[KEY_RECORDED_AT_MILLIS] = millis
        _uiState.update {
            it.copy(
                weekNumber = weekNumber,
                year = year,
                recordedAtMillis = millis,
                error = null,
            )
        }
    }

    private fun setRecordedAtMillis(value: Long) {
        val (weekNumber, year) = getWeekNumberAndYearFromTimestamp(value)
        savedStateHandle[KEY_RECORDED_AT_MILLIS] = value
        savedStateHandle[KEY_WEEK_NUMBER] = weekNumber
        savedStateHandle[KEY_YEAR] = year
        _uiState.update {
            it.copy(
                recordedAtMillis = value,
                weekNumber = weekNumber,
                year = year,
                error = null,
            )
        }
    }


    companion object {
        private const val KEY_AMOUNT_TEXT = "add_paycheck_amount_text"
        private const val KEY_RECORDED_AT_MILLIS = "add_paycheck_recorded_at_millis"
        private const val KEY_WEEK_NUMBER = "add_paycheck_week_number"
        private const val KEY_YEAR = "add_paycheck_year"
        private const val KEY_SHOW_SAVE_DIALOG = "add_paycheck_show_save_dialog"
    }
}

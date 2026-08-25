package com.truckerload.presentation.screens.add

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.truckerload.R
import com.truckerload.data.preferences.LastUsedDefaultsStore
import com.truckerload.data.repository.DieselRepository
import com.truckerload.domain.model.Diesel
import com.truckerload.domain.model.DieselPurchaseMath
import com.truckerload.utils.AmountInputValidator
import com.truckerload.utils.getCurrentWeekNumberAndYear
import com.truckerload.utils.getMillisForWeek
import com.truckerload.utils.getWeekNumberAndYearFromTimestamp
import com.truckerload.utils.shiftWeekNumberAndYear
import com.truckerload.utils.getWeekRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

data class AddDieselUiState(
    val gallonsText: String = "",
    val pricePerGallonText: String = "",
    val discountPriceText: String = "",
    val locationText: String = "",
    val recordedAtMillis: Long = System.currentTimeMillis(),
    val weekNumber: Int = 1,
    val year: Int = 1970,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
    val showSaveDialog: Boolean = false,
) {
    val gallons: Double? get() = AmountInputValidator.parsePositiveAmount(gallonsText)
    val pricePerGallon: Double? get() = AmountInputValidator.parsePositiveAmount(pricePerGallonText)
    val discountPricePerGallon: Double?
        get() = AmountInputValidator.parsePositiveAmount(
            discountPriceText
        )

    val paidTotal: Double?
        get() = DieselPurchaseMath.paidTotal(gallons, pricePerGallon, discountPricePerGallon)

    val savings: Double?
        get() = DieselPurchaseMath.savings(gallons, pricePerGallon, discountPricePerGallon)
}

@HiltViewModel
class AddDieselViewModel @Inject constructor(
    application: Application,
    private val dieselRepository: DieselRepository,
    private val lastUsedDefaultsStore: LastUsedDefaultsStore,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val initialWeekAndYear = getCurrentWeekNumberAndYear()

    private val _uiState = MutableStateFlow(
        AddDieselUiState(
            gallonsText = savedStateHandle[KEY_GALLONS_TEXT] ?: "",
            pricePerGallonText = savedStateHandle[KEY_PRICE_TEXT] ?: "",
            discountPriceText = savedStateHandle[KEY_DISCOUNT_TEXT] ?: "",
            locationText = savedStateHandle[KEY_LOCATION_TEXT] ?: "",
            recordedAtMillis = savedStateHandle[KEY_RECORDED_AT_MILLIS] ?: System.currentTimeMillis(),
            weekNumber = savedStateHandle[KEY_WEEK_NUMBER] ?: initialWeekAndYear.first,
            year = savedStateHandle[KEY_YEAR] ?: initialWeekAndYear.second,
            showSaveDialog = savedStateHandle[KEY_SHOW_SAVE_DIALOG] ?: false,
        ),
    )
    val uiState: StateFlow<AddDieselUiState> = _uiState.asStateFlow()

    fun setGallonsText(value: String) {
        savedStateHandle[KEY_GALLONS_TEXT] = value
        _uiState.update { it.copy(gallonsText = value, error = null) }
    }

    fun setPricePerGallonText(value: String) {
        savedStateHandle[KEY_PRICE_TEXT] = value
        _uiState.update { it.copy(pricePerGallonText = value, error = null) }
    }

    fun setDiscountPriceText(value: String) {
        savedStateHandle[KEY_DISCOUNT_TEXT] = value
        _uiState.update { it.copy(discountPriceText = value, error = null) }
    }

    fun setLocationText(value: String) {
        savedStateHandle[KEY_LOCATION_TEXT] = value
        _uiState.update { it.copy(locationText = value, error = null) }
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
        val validationError = validateInputs(_uiState.value)
        if (validationError != null) {
            _uiState.update { it.copy(error = validationError) }
            return
        }
        // Keep recordedAtMillis / selected week — do not force "now".
        savedStateHandle[KEY_SHOW_SAVE_DIALOG] = true
        _uiState.update { it.copy(error = null, showSaveDialog = true) }
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
        if (state.isSaving) return
        val validationError = validateInputs(state)
        if (validationError != null) {
            _uiState.update { it.copy(error = validationError) }
            return
        }

        val gallons = state.gallons!!
        val price = state.pricePerGallon!!
        val discount = state.discountPricePerGallon
        val paidTotal = state.paidTotal!!

        val weekNumber = state.weekNumber
        val year = state.year
        val (weekStart, weekEnd, weekLabel) = getWeekRange(weekNumber, year)
        val diesel = Diesel(
            id = 0,
            weekNumber = weekNumber,
            year = year,
            weekLabel = weekLabel,
            weekStartDate = weekStart,
            weekEndDate = weekEnd,
            totalAmount = paidTotal,
            gallons = gallons,
            pricePerGallon = price,
            discountPricePerGallon = discount,
            location = state.locationText.trim().ifBlank { null },
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
                lastUsedDefaultsStore.saveDieselAmount(paidTotal)
                savedStateHandle[KEY_GALLONS_TEXT] = ""
                savedStateHandle[KEY_PRICE_TEXT] = ""
                savedStateHandle[KEY_DISCOUNT_TEXT] = ""
                savedStateHandle[KEY_LOCATION_TEXT] = ""
                savedStateHandle[KEY_WEEK_NUMBER] = weekNumber
                savedStateHandle[KEY_YEAR] = year
                savedStateHandle[KEY_SHOW_SAVE_DIALOG] = false
                _uiState.update {
                    it.copy(
                        gallonsText = "",
                        pricePerGallonText = "",
                        discountPriceText = "",
                        locationText = "",
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

    private fun validateInputs(state: AddDieselUiState): String? {
        val app = getApplication<Application>()
        if (state.gallons == null) {
            return app.getString(R.string.add_diesel_gallons_required)
        }
        if (state.pricePerGallon == null) {
            return app.getString(R.string.add_diesel_price_required)
        }
        val discountRaw = state.discountPriceText.trim()
        if (discountRaw.isNotEmpty() && state.discountPricePerGallon == null) {
            return app.getString(R.string.add_diesel_discount_invalid)
        }
        val discount = state.discountPricePerGallon
        if (discount != null && discount > state.pricePerGallon!!) {
            return app.getString(R.string.add_diesel_discount_above_price)
        }
        if (state.paidTotal == null) {
            return app.getString(R.string.common_amount_must_be_positive)
        }
        return null
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
        private const val KEY_GALLONS_TEXT = "add_diesel_gallons_text"
        private const val KEY_PRICE_TEXT = "add_diesel_price_text"
        private const val KEY_DISCOUNT_TEXT = "add_diesel_discount_text"
        private const val KEY_LOCATION_TEXT = "add_diesel_location_text"
        private const val KEY_RECORDED_AT_MILLIS = "add_diesel_recorded_at_millis"
        private const val KEY_WEEK_NUMBER = "add_diesel_week_number"
        private const val KEY_YEAR = "add_diesel_year"
        private const val KEY_SHOW_SAVE_DIALOG = "add_diesel_show_save_dialog"
    }
}

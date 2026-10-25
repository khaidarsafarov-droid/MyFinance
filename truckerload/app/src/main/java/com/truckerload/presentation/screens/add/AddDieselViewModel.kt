package com.truckerload.presentation.screens.add
import kotlinx.coroutines.CancellationException

import android.database.sqlite.SQLiteException
import java.io.IOException


import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.truckerload.R
import com.truckerload.data.preferences.LastUsedDefaultsStore
import com.truckerload.presentation.ScreenPhase
import com.truckerload.data.repository.DieselRepository
import com.truckerload.domain.model.Diesel
import com.truckerload.domain.model.DieselPurchaseMath
import com.truckerload.domain.money.Money
import com.truckerload.domain.money.UnitPrice
import com.truckerload.domain.parser.DieselReceiptExtractor
import com.truckerload.utils.AmountInputValidator
import com.truckerload.utils.LocationHelper
import com.truckerload.utils.OCRService
import com.truckerload.utils.applyUtcDatePickerDay
import com.truckerload.utils.getCurrentWeekNumberAndYear
import com.truckerload.utils.getWeekNumberAndYearFromTimestamp
import com.truckerload.utils.getWeekRange
import com.truckerload.domain.week.WeekStartRuntime
import com.truckerload.widget.WidgetDataUpdater
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
    val editingId: Int? = null,
    val gallonsText: String = "",
    val pricePerGallonText: String = "",
    val discountPriceText: String = "",
    val locationText: String = "",
    val rawExtractedText: String = "",
    val recordedAtMillis: Long = System.currentTimeMillis(),
    val weekNumber: Int = 1,
    val year: Int = 1970,
    val isSaving: Boolean = false,
    val isScanning: Boolean = false,
    val isResolvingLocation: Boolean = false,
    val scanMessage: String? = null,
    val error: String? = null,
    val saved: Boolean = false,
    val deleted: Boolean = false,
    val showSaveDialog: Boolean = false,
    val showFieldErrors: Boolean = false,
    val fieldErrorNonce: Int = 0,
    val draftBaseline: String? = null,
    val isLoadingRecord: Boolean = false,
    val loadFailed: Boolean = false,
) {
    fun draftSignature(): String = FormDraft.diesel(
        gallons = gallonsText,
        price = pricePerGallonText,
        discount = discountPriceText,
        location = locationText,
        recordedAtMillis = recordedAtMillis,
        weekNumber = weekNumber,
        year = year,
    )

    val isDirty: Boolean get() = FormDraft.isDirty(draftBaseline, draftSignature())

    val recordPhase: ScreenPhase
        get() = when {
            loadFailed -> ScreenPhase.Error
            isLoadingRecord -> ScreenPhase.Loading
            else -> ScreenPhase.Content
        }

    val gallons: Double? get() = AmountInputValidator.parsePositiveAmount(gallonsText)
    val pricePerGallon: UnitPrice?
        get() = AmountInputValidator.parsePositiveAmount(pricePerGallonText)?.let(UnitPrice::fromDollars)
    val discountPricePerGallon: UnitPrice?
        get() = AmountInputValidator.parsePositiveAmount(discountPriceText)?.let(UnitPrice::fromDollars)

    val paidTotal: Money?
        get() = DieselPurchaseMath.paidTotal(gallons, pricePerGallon, discountPricePerGallon)

    val savings: Money?
        get() = DieselPurchaseMath.savings(gallons, pricePerGallon, discountPricePerGallon)
}

@HiltViewModel
class AddDieselViewModel @Inject constructor(
    application: Application,
    private val dieselRepository: DieselRepository,
    private val lastUsedDefaultsStore: LastUsedDefaultsStore,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val initialWeekAndYear = getCurrentWeekNumberAndYear(WeekStartRuntime.diesel)
    private val editDieselId: Int = savedStateHandle.get<Int>("dieselId")?.takeIf { it > 0 } ?: -1

    private val _uiState = MutableStateFlow(
        AddDieselUiState(
            gallonsText = savedStateHandle[KEY_GALLONS_TEXT] ?: "",
            pricePerGallonText = savedStateHandle[KEY_PRICE_TEXT] ?: "",
            discountPriceText = savedStateHandle[KEY_DISCOUNT_TEXT] ?: "",
            locationText = savedStateHandle[KEY_LOCATION_TEXT] ?: "",
            rawExtractedText = savedStateHandle[KEY_RAW_TEXT] ?: "",
            recordedAtMillis = savedStateHandle[KEY_RECORDED_AT_MILLIS] ?: System.currentTimeMillis(),
            weekNumber = savedStateHandle[KEY_WEEK_NUMBER] ?: initialWeekAndYear.first,
            year = savedStateHandle[KEY_YEAR] ?: initialWeekAndYear.second,
            showSaveDialog = savedStateHandle[KEY_SHOW_SAVE_DIALOG] ?: false,
            editingId = editDieselId.takeIf { it > 0 },
            isLoadingRecord = editDieselId > 0 && savedStateHandle.get<String>(KEY_DRAFT) == null,
        ),
    )
    val uiState: StateFlow<AddDieselUiState> = _uiState.asStateFlow()

    init {
        val savedBaseline = savedStateHandle.get<String>(KEY_DRAFT)
        if (editDieselId > 0 && savedBaseline == null) {
            loadExistingRecord()
        } else {
            val baseline = savedBaseline ?: _uiState.value.draftSignature().also {
                savedStateHandle[KEY_DRAFT] = it
            }
            _uiState.update {
                it.copy(
                    draftBaseline = baseline,
                    editingId = editDieselId.takeIf { id -> id > 0 },
                )
            }
        }
    }

    fun retryEditLoad() {
        if (editDieselId <= 0 || _uiState.value.isLoadingRecord) return
        _uiState.update { it.copy(isLoadingRecord = true, loadFailed = false) }
        loadExistingRecord()
    }

    private fun loadExistingRecord() {
        viewModelScope.launch {
            runCatching { dieselRepository.getDieselById(editDieselId) }
                .onSuccess { existing ->
                    if (existing != null) {
                        loadForEdit(existing)
                    } else {
                        _uiState.update { it.copy(isLoadingRecord = false, loadFailed = true) }
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingRecord = false, loadFailed = true) }
                }
        }
    }

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

    fun ensureLocation() {
        if (_uiState.value.locationText.isNotBlank() || _uiState.value.isResolvingLocation) return
        viewModelScope.launch {
            _uiState.update { it.copy(isResolvingLocation = true) }
            val label = withContext(Dispatchers.IO) {
                LocationHelper(getApplication()).getCurrentStopLabel()
            }
            _uiState.update { state ->
                val nextLocation = state.locationText.ifBlank { label.orEmpty() }
                savedStateHandle[KEY_LOCATION_TEXT] = nextLocation
                val before = state.draftSignature()
                val filled = state.copy(isResolvingLocation = false, locationText = nextLocation)
                val baseline = FormDraft.baselineAfterAutoFill(
                    state.draftBaseline,
                    before,
                    filled.draftSignature(),
                )
                if (baseline != null && baseline != state.draftBaseline) {
                    savedStateHandle[KEY_DRAFT] = baseline
                }
                filled.copy(draftBaseline = baseline)
            }
        }
    }

    fun scanReceipt(uri: Uri) {
        if (_uiState.value.isScanning) return
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, scanMessage = null, error = null) }
            val app = getApplication<Application>()
            val ocr = OCRService(app)
            val result = runCatching {
                withContext(Dispatchers.IO) { ocr.recognizeFromUri(app, uri) }
            }
            ocr.close()
            val text = result.getOrNull()?.text.orEmpty()
            if (result.isFailure || text.isBlank()) {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        scanMessage = app.getString(R.string.add_diesel_scan_failed),
                    )
                }
                return@launch
            }
            applyExtractedText(text)
        }
    }

    internal fun applyExtractedText(text: String) {
        val app = getApplication<Application>()
        val fields = DieselReceiptExtractor.extract(text)
        val labels = DieselScanLabels(
            gallons = app.getString(DieselAmountUnits.gallonsLabel()),
            price = app.getString(DieselAmountUnits.priceLabel()),
            discount = app.getString(R.string.add_diesel_discount_price),
            location = app.getString(R.string.add_diesel_location),
            noneMessage = app.getString(R.string.add_diesel_scan_none),
            foundTemplate = app.getString(R.string.add_diesel_scan_found),
        )
        _uiState.update { state ->
            val filled = DieselFormFill.applyScan(state, fields, text, labels)
            val next = if (!com.truckerload.domain.units.DisplayUnits.isMetric) filled else filled.copy(
                gallonsText = fields.gallons?.let { formatAmount(DieselAmountUnits.displayVolume(it)) } ?: filled.gallonsText,
                pricePerGallonText = fields.pricePerGallon?.let {
                    formatAmount(com.truckerload.domain.units.MeasurementUnits.displayPricePerVolume(it))
                } ?: filled.pricePerGallonText,
                discountPriceText = fields.discountPricePerGallon?.let {
                    formatAmount(com.truckerload.domain.units.MeasurementUnits.displayPricePerVolume(it))
                } ?: filled.discountPriceText,
            )
            savedStateHandle[KEY_GALLONS_TEXT] = next.gallonsText
            savedStateHandle[KEY_PRICE_TEXT] = next.pricePerGallonText
            savedStateHandle[KEY_DISCOUNT_TEXT] = next.discountPriceText
            savedStateHandle[KEY_LOCATION_TEXT] = next.locationText
            savedStateHandle[KEY_RAW_TEXT] = next.rawExtractedText
            next
        }
    }

    fun openSaveDialog() {
        if (_uiState.value.isSaving) return
        val validationError = validateInputs(_uiState.value)
        if (validationError != null) {
            _uiState.update {
                it.copy(
                    error = validationError,
                    showFieldErrors = true,
                    fieldErrorNonce = it.fieldErrorNonce + 1,
                )
            }
            return
        }
        // Keep recordedAtMillis (week is derived from it) — do not force "now".
        savedStateHandle[KEY_SHOW_SAVE_DIALOG] = true
        _uiState.update { it.copy(error = null, showSaveDialog = true) }
    }

    fun dismissSaveDialog() {
        savedStateHandle[KEY_SHOW_SAVE_DIALOG] = false
        _uiState.update { it.copy(showSaveDialog = false) }
    }

    fun setRecordedDate(selectedDateMillis: Long) {
        // FIX: DatePicker millis are UTC midnight — local Calendar shifted US timezones back a day
        setRecordedAtMillis(applyUtcDatePickerDay(selectedDateMillis, _uiState.value.recordedAtMillis))
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
            _uiState.update {
                it.copy(
                    error = validationError,
                    showFieldErrors = true,
                    fieldErrorNonce = it.fieldErrorNonce + 1,
                )
            }
            return
        }

        val gallons = DieselAmountUnits.storedGallons(state.gallons!!)
        val price = DieselAmountUnits.storedPrice(state.pricePerGallon!!)
        val discount = state.discountPricePerGallon?.let(DieselAmountUnits::storedPrice)
        val paidTotal = state.paidTotal!!

        val weekNumber = state.weekNumber
        val year = state.year
        val (weekStart, weekEnd, weekLabel) = getWeekRange(weekNumber, year, WeekStartRuntime.diesel)
        val diesel = Diesel(
            id = state.editingId ?: 0,
            weekNumber = weekNumber,
            year = year,
            weekLabel = weekLabel,
            weekStartDate = weekStart,
            weekEndDate = weekEnd,
            totalAmount = paidTotal,
            gallons = gallons,
            pricePerGallon = price,
            discountPricePerGallon = discount,
            location = state.locationText.trim().takeIf { it.isNotBlank() },
            rawExtractedText = state.rawExtractedText,
            sourceFileName = state.rawExtractedText.takeIf { it.isNotBlank() }?.let { "diesel_scan.jpg" },
            addedAt = state.recordedAtMillis,
        )

        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    dieselRepository.insertDiesel(diesel)
                }
                lastUsedDefaultsStore.saveDieselAmount(paidTotal.cents / 100.0)
                WidgetDataUpdater.updateWidgetData(getApplication())
                savedStateHandle[KEY_GALLONS_TEXT] = ""
                savedStateHandle[KEY_PRICE_TEXT] = ""
                savedStateHandle[KEY_DISCOUNT_TEXT] = ""
                savedStateHandle[KEY_LOCATION_TEXT] = ""
                savedStateHandle[KEY_RAW_TEXT] = ""
                savedStateHandle[KEY_WEEK_NUMBER] = weekNumber
                savedStateHandle[KEY_YEAR] = year
                savedStateHandle[KEY_SHOW_SAVE_DIALOG] = false
                _uiState.update {
                    it.copy(
                        gallonsText = "",
                        pricePerGallonText = "",
                        discountPriceText = "",
                        locationText = "",
                        rawExtractedText = "",
                        scanMessage = null,
                        weekNumber = weekNumber,
                        year = year,
                        isSaving = false,
                        saved = true,
                        showSaveDialog = false,
                    )
                }
            } catch (e: SQLiteException) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = e.message ?: getApplication<Application>().getString(R.string.common_save_failed),
                    )
                }
            
            } catch (e: IOException) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = e.message ?: getApplication<Application>().getString(R.string.common_save_failed),
                    )
                }
            
            } catch (e: IllegalArgumentException) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = e.message ?: getApplication<Application>().getString(R.string.common_save_failed),
                    )
                }
            
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalStateException) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = e.message ?: getApplication<Application>().getString(R.string.common_save_failed),
                    )
                }
            
            }
        }
    }


    fun delete() {
        val id = _uiState.value.editingId ?: return
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, error = null, showSaveDialog = false) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    dieselRepository.deleteDiesel(id)
                }
                WidgetDataUpdater.updateWidgetData(getApplication())
                _uiState.update {
                    it.copy(isSaving = false, deleted = true, showSaveDialog = false)
                }
            } catch (e: SQLiteException) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = e.message
                            ?: getApplication<Application>().getString(R.string.common_save_failed),
                    )
                }
            
            } catch (e: IOException) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = e.message
                            ?: getApplication<Application>().getString(R.string.common_save_failed),
                    )
                }
            
            } catch (e: IllegalArgumentException) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = e.message
                            ?: getApplication<Application>().getString(R.string.common_save_failed),
                    )
                }
            
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalStateException) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = e.message
                            ?: getApplication<Application>().getString(R.string.common_save_failed),
                    )
                }
            
            }
        }
    }

    fun clearDeleted() {
        _uiState.update { it.copy(deleted = false) }
    }

    fun clearSaved() {
        _uiState.update { it.copy(saved = false) }
    }

    private fun loadForEdit(existing: Diesel) {
        val gallons = existing.gallons?.let { formatAmount(DieselAmountUnits.displayVolume(it)) }.orEmpty()
        val price = existing.pricePerGallon?.let { raw ->
            if (com.truckerload.domain.units.DisplayUnits.isMetric) {
                formatAmount(DieselAmountUnits.displayPrice(raw))
            } else {
                formatAmount(raw)
            }
        }.orEmpty()
        val discount = existing.discountPricePerGallon?.let { raw ->
            if (com.truckerload.domain.units.DisplayUnits.isMetric) {
                formatAmount(DieselAmountUnits.displayPrice(raw))
            } else {
                formatAmount(raw)
            }
        }.orEmpty()
        savedStateHandle[KEY_GALLONS_TEXT] = gallons
        savedStateHandle[KEY_PRICE_TEXT] = price
        savedStateHandle[KEY_DISCOUNT_TEXT] = discount
        savedStateHandle[KEY_LOCATION_TEXT] = existing.location.orEmpty()
        savedStateHandle[KEY_RAW_TEXT] = existing.rawExtractedText
        savedStateHandle[KEY_RECORDED_AT_MILLIS] = existing.addedAt
        savedStateHandle[KEY_WEEK_NUMBER] = existing.weekNumber
        savedStateHandle[KEY_YEAR] = existing.year
        _uiState.update { state ->
            val loaded = state.copy(
                editingId = existing.id,
                gallonsText = gallons,
                pricePerGallonText = price,
                discountPriceText = discount,
                locationText = existing.location.orEmpty(),
                rawExtractedText = existing.rawExtractedText,
                recordedAtMillis = existing.addedAt,
                weekNumber = existing.weekNumber,
                year = existing.year,
            )
            val baseline = savedStateHandle.get<String>(KEY_DRAFT) ?: loaded.draftSignature().also {
                savedStateHandle[KEY_DRAFT] = it
            }
            loaded.copy(draftBaseline = baseline, isLoadingRecord = false, loadFailed = false)
        }
    }

    private fun formatAmount(value: UnitPrice): String {
        val text = value.formatPlain().trimEnd('0').trimEnd('.')
        return text.ifEmpty { "0" }
    }

    private fun formatAmount(value: Double): String =
        if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            String.format(java.util.Locale.US, "%.2f", value)
        }

    private fun validateInputs(state: AddDieselUiState): String? {
        val app = getApplication<Application>()
        if (state.gallons == null) return app.getString(DieselAmountUnits.gallonsRequired())
        if (state.pricePerGallon == null) return app.getString(DieselAmountUnits.priceRequired())
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

    private fun setRecordedAtMillis(value: Long) {
        val (weekNumber, year) = getWeekNumberAndYearFromTimestamp(value, WeekStartRuntime.diesel)
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
        private const val KEY_RAW_TEXT = "add_diesel_raw_text"
        private const val KEY_RECORDED_AT_MILLIS = "add_diesel_recorded_at_millis"
        private const val KEY_WEEK_NUMBER = "add_diesel_week_number"
        private const val KEY_YEAR = "add_diesel_year"
        private const val KEY_SHOW_SAVE_DIALOG = "add_diesel_show_save_dialog"
        private const val KEY_DRAFT = "add_diesel_draft_baseline"
    }
}

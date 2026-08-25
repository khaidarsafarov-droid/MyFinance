package com.truckerload.presentation.screens.tax

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.MaintenanceRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.domain.tax.AccountantExportSection
import com.truckerload.domain.tax.PerDiemCalculator
import com.truckerload.utils.AccountantWorkbookBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class TaxTrackerUiState(
    val year: Int = Calendar.getInstance().get(Calendar.YEAR),
    val calendarMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val totalGrossIncome: Double = 0.0,
    val dieselDeductions: Double = 0.0,
    val totalDeductions: Double = 0.0,
    val perDiemDays: Int = 0,
    val perDiemAmount: Double = 0.0,
    /** Unique on-duty YYYY-MM-DD dates for [year] (calendar dots). */
    val perDiemDates: Set<String> = emptySet(),
    val taxableIncome: Double = 0.0,
    val selfEmploymentTax: Double = 0.0,
    val federalTax: Double = 0.0,
    val totalTaxOwed: Double = 0.0,
    val daysUntilNextQuarterly: Int = 0,
    val nextQuarterlyDate: String = "",
    val reservedAmount: Double = 0.0,
    val shortfall: Double = 0.0,
    val isLoading: Boolean = false,
    val isExporting: Boolean = false,
    val errorMessage: String? = null,
    val exportMessage: String? = null,
)

private val QUARTERLY_DATES = listOf(
    Triple(4, 15, "Q1"),
    Triple(6, 15, "Q2"),
    Triple(9, 15, "Q3"),
    Triple(1, 15, "Q4"),
)

@HiltViewModel
class TaxTrackerViewModel @Inject constructor(
    private val paycheckRepository: PaycheckRepository,
    private val dieselRepository: DieselRepository,
    private val loadRepository: LoadRepository,
    private val maintenanceRepository: MaintenanceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaxTrackerUiState())
    val uiState: StateFlow<TaxTrackerUiState> = _uiState.asStateFlow()

    init {
        loadTaxData(Calendar.getInstance().get(Calendar.YEAR))
    }

    fun refresh() {
        loadTaxData(_uiState.value.year)
    }

    fun setYear(year: Int) {
        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        _uiState.update {
            it.copy(
                year = year,
                calendarMonth = if (year == currentYear) {
                    now.get(Calendar.MONTH) + 1
                } else {
                    1
                },
            )
        }
        loadTaxData(year)
    }

    fun setCalendarMonth(year: Int, month: Int) {
        val previousYear = _uiState.value.year
        _uiState.update {
            it.copy(
                year = year,
                calendarMonth = month.coerceIn(1, 12),
            )
        }
        if (year != previousYear) {
            loadTaxData(year)
        }
    }

    fun clearExportMessage() {
        _uiState.update { it.copy(exportMessage = null) }
    }

    fun setExporting(exporting: Boolean, message: String? = null) {
        _uiState.update {
            it.copy(isExporting = exporting, exportMessage = message, errorMessage = null)
        }
    }

    /**
     * Loads year-scoped rows for the accountant workbook.
     * Call from a coroutine; empty selected sections still produce a Summary sheet.
     */
    suspend fun prepareWorkbookInput(
        sections: Set<AccountantExportSection>,
    ): AccountantWorkbookBuilder.Input {
        val year = _uiState.value.year
        val resolved = AccountantWorkbookBuilder.resolveSections(sections)
        val loads = if (AccountantExportSection.LOADS in resolved) {
            loadRepository.getLoadsByYear(year)
        } else {
            emptyList()
        }
        val diesel = if (AccountantExportSection.DIESEL in resolved) {
            dieselRepository.getDieselForYear(year)
        } else {
            emptyList()
        }
        val perDiemDates = if (AccountantExportSection.PER_DIEM in resolved) {
            if (loads.isNotEmpty()) {
                PerDiemCalculator.uniqueOnDutyDates(loads, year)
            } else {
                PerDiemCalculator.uniqueOnDutyDates(loadRepository.getLoadsByYear(year), year)
            }
        } else {
            emptySet()
        }
        val maintenance = if (AccountantExportSection.MAINTENANCE in resolved) {
            maintenanceRepository.getArchiveForYear(year)
        } else {
            emptyList()
        }
        return AccountantWorkbookBuilder.Input(
            year = year,
            loads = loads,
            diesel = diesel,
            perDiemDates = perDiemDates,
            maintenance = maintenance,
            grossIncome = _uiState.value.totalGrossIncome,
        )
    }

    fun isWorkbookEmpty(
        input: AccountantWorkbookBuilder.Input,
        sections: Set<AccountantExportSection>,
    ): Boolean {
        val resolved = AccountantWorkbookBuilder.resolveSections(sections)
        return resolved.all { section ->
            when (section) {
                AccountantExportSection.LOADS -> input.loads.isEmpty()
                AccountantExportSection.DIESEL -> input.diesel.isEmpty()
                AccountantExportSection.PER_DIEM -> input.perDiemDates.isEmpty()
                AccountantExportSection.MAINTENANCE -> input.maintenance.isEmpty()
                AccountantExportSection.ALL -> true
            }
        }
    }

    private fun loadTaxData(year: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val paychecks = paycheckRepository.getPaychecksForYear(year)
                val diesel = dieselRepository.getDieselForYear(year)
                val loads = loadRepository.getLoadsByYear(year)

                val totalGross = paychecks.sumOf { paycheck ->
                    paycheck.grossAmount?.takeIf { it > 0.0 } ?: paycheck.netAmount
                }
                val dieselDed = diesel.sumOf { it.totalAmount }
                val perDiemDates = PerDiemCalculator.uniqueOnDutyDates(loads, year)
                val perDiemDays = perDiemDates.size
                val perDiemAmt = PerDiemCalculator.amount(perDiemDays)
                val totalDed = dieselDed
                val taxable = (totalGross - totalDed - perDiemAmt).coerceAtLeast(0.0)

                val seTax = taxable * 0.153 * 0.9235
                val fedTax = calculateFederalTax(taxable)
                val totalOwed = seTax + fedTax

                val (daysUntil, nextDate) = getNextQuarterlyDate()
                val reserved = 0.0
                val shortfall = (totalOwed - reserved).coerceAtLeast(0.0)

                _uiState.update {
                    it.copy(
                        year = year,
                        totalGrossIncome = totalGross,
                        dieselDeductions = dieselDed,
                        totalDeductions = totalDed,
                        perDiemDays = perDiemDays,
                        perDiemAmount = perDiemAmt,
                        perDiemDates = perDiemDates,
                        taxableIncome = taxable,
                        selfEmploymentTax = seTax,
                        federalTax = fedTax,
                        totalTaxOwed = totalOwed,
                        daysUntilNextQuarterly = daysUntil,
                        nextQuarterlyDate = nextDate,
                        reservedAmount = reserved,
                        shortfall = shortfall,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: error.message ?: error.javaClass.simpleName,
                    )
                }
            }
        }
    }

    private fun calculateFederalTax(taxable: Double): Double {
        if (taxable <= 0) return 0.0
        val brackets = listOf(
            11600.0 to 0.10,
            47150.0 to 0.12,
            100525.0 to 0.22,
            191950.0 to 0.24,
            243725.0 to 0.32,
            609350.0 to 0.35,
            Double.MAX_VALUE to 0.37,
        )
        var tax = 0.0
        var remaining = taxable
        var prevLimit = 0.0
        for ((limit, rate) in brackets) {
            val bracketSize = (limit - prevLimit).coerceAtLeast(0.0)
            val inBracket = minOf(remaining, bracketSize)
            tax += inBracket * rate
            remaining -= inBracket
            if (remaining <= 0) break
            prevLimit = limit
        }
        return tax
    }

    private fun getNextQuarterlyDate(): Pair<Int, String> {
        val todayCal = Calendar.getInstance()
        val todayYear = todayCal.get(Calendar.YEAR)
        for ((month, day, label) in QUARTERLY_DATES) {
            val qCal = Calendar.getInstance().apply {
                clear()
                set(Calendar.YEAR, if (month == 1) todayYear + 1 else todayYear)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, day)
            }
            val millisUntil = qCal.timeInMillis - todayCal.timeInMillis
            val daysUntil = ((millisUntil + 23 * 60 * 60 * 1000L) / (24 * 60 * 60 * 1000L)).toInt()
            if (daysUntil > 0) {
                return Pair(daysUntil, "$label $month/$day")
            }
        }
        return Pair(0, "")
    }
}

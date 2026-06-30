package com.truckerload.presentation.screens.tax

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class TaxTrackerUiState(
    val year: Int = Calendar.getInstance().get(Calendar.YEAR),
    val totalGrossIncome: Double = 0.0,
    val dieselDeductions: Double = 0.0,
    val totalDeductions: Double = 0.0,
    val perDiemDays: Int = 0,
    val perDiemAmount: Double = 0.0,
    val taxableIncome: Double = 0.0,
    val selfEmploymentTax: Double = 0.0,
    val federalTax: Double = 0.0,
    val totalTaxOwed: Double = 0.0,
    val daysUntilNextQuarterly: Int = 0,
    val nextQuarterlyDate: String = "",
    val reservedAmount: Double = 0.0,
    val shortfall: Double = 0.0
)

private val PER_DIEM_RATE = 69.0
private val QUARTERLY_DATES = listOf(
    Triple(4, 15, "Q1"),
    Triple(6, 15, "Q2"),
    Triple(9, 15, "Q3"),
    Triple(1, 15, "Q4")
)

class TaxTrackerViewModel(
    private val paycheckRepository: PaycheckRepository,
    private val dieselRepository: DieselRepository,
    private val loadRepository: LoadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaxTrackerUiState())
    val uiState = _uiState.asStateFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TaxTrackerUiState())

    init {
        loadTaxData(Calendar.getInstance().get(Calendar.YEAR))
    }

    fun refresh() {
        loadTaxData(_uiState.value.year)
    }

    fun setYear(year: Int) {
        _uiState.update { it.copy(year = year) }
        loadTaxData(year)
    }

    private fun loadTaxData(year: Int) {
        viewModelScope.launch {
            val paychecks = paycheckRepository.getPaychecksForYear(year)
            val diesel = dieselRepository.getDieselForYear(year)
            val loads = loadRepository.getLoadsByYear(year)

            val totalGross = paychecks.sumOf { it.netAmount }
            val dieselDed = diesel.sumOf { it.totalAmount }
            val perDiemDays = loads.size
            val perDiemAmt = perDiemDays * PER_DIEM_RATE
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
                    totalGrossIncome = totalGross,
                    dieselDeductions = dieselDed,
                    totalDeductions = totalDed,
                    perDiemDays = perDiemDays,
                    perDiemAmount = perDiemAmt,
                    taxableIncome = taxable,
                    selfEmploymentTax = seTax,
                    federalTax = fedTax,
                    totalTaxOwed = totalOwed,
                    daysUntilNextQuarterly = daysUntil,
                    nextQuarterlyDate = nextDate,
                    reservedAmount = reserved,
                    shortfall = shortfall
                )
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
            Double.MAX_VALUE to 0.37
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
        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_YEAR)
        val todayYear = cal.get(Calendar.YEAR)
        for ((month, day, label) in QUARTERLY_DATES) {
            val qCal = Calendar.getInstance().apply {
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.YEAR, if (month == 1) todayYear + 1 else todayYear)
            }
            val qDay = qCal.get(Calendar.DAY_OF_YEAR)
            val qYear = qCal.get(Calendar.YEAR)
            val daysUntil = (qYear - todayYear) * 365 + (qDay - today)
            if (daysUntil > 0) {
                return Pair(daysUntil, "$label ${month}/$day")
            }
        }
        return Pair(0, "")
    }

    class Factory(
        private val paycheckRepository: PaycheckRepository,
        private val dieselRepository: DieselRepository,
        private val loadRepository: LoadRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TaxTrackerViewModel(paycheckRepository, dieselRepository, loadRepository) as T
    }
}

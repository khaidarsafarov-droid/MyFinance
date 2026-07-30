package com.truckerload.presentation.screens.tax

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.domain.goal.LoadYieldCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val shortfall: Double = 0.0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
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
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    val paychecks = paycheckRepository.getPaychecksForYear(year)
                    val diesel = dieselRepository.getDieselForYear(year)
                    val loads = loadRepository.getLoadsByYear(year)

                    val totalGross = paychecks.sumOf { paycheck ->
                        paycheck.grossAmount?.takeIf { it > 0.0 } ?: paycheck.netAmount
                    }
                    val dieselDed = diesel.sumOf { it.totalAmount }
                    // Active days per load (min 1), summed — better than counting loads as days.
                    val perDiemDays = loads.sumOf {
                        LoadYieldCalculator.loadActiveDurationDays(it).toInt().coerceAtLeast(1)
                    }
                    val perDiemAmt = perDiemDays * PER_DIEM_RATE
                    val totalDed = dieselDed
                    val taxable = (totalGross - totalDed - perDiemAmt).coerceAtLeast(0.0)

                    val seTax = taxable * 0.153 * 0.9235
                    val fedTax = calculateFederalTax(taxable)
                    val totalOwed = seTax + fedTax

                    val (daysUntil, nextDate) = getNextQuarterlyDate()
                    // Reserved for quarterly: leave 0 until user enters a savings field (UI not yet).
                    val reserved = 0.0
                    val shortfall = (totalOwed - reserved).coerceAtLeast(0.0)

                    TaxComputeResult(
                        totalGross = totalGross,
                        dieselDed = dieselDed,
                        totalDed = totalDed,
                        perDiemDays = perDiemDays,
                        perDiemAmt = perDiemAmt,
                        taxable = taxable,
                        seTax = seTax,
                        fedTax = fedTax,
                        totalOwed = totalOwed,
                        daysUntil = daysUntil,
                        nextDate = nextDate,
                        reserved = reserved,
                        shortfall = shortfall,
                    )
                }
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        totalGrossIncome = result.totalGross,
                        dieselDeductions = result.dieselDed,
                        totalDeductions = result.totalDed,
                        perDiemDays = result.perDiemDays,
                        perDiemAmount = result.perDiemAmt,
                        taxableIncome = result.taxable,
                        selfEmploymentTax = result.seTax,
                        federalTax = result.fedTax,
                        totalTaxOwed = result.totalOwed,
                        daysUntilNextQuarterly = result.daysUntil,
                        nextQuarterlyDate = result.nextDate,
                        reservedAmount = result.reserved,
                        shortfall = result.shortfall,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = error.toUiMessage())
                }
            }
        }
    }

    private data class TaxComputeResult(
        val totalGross: Double,
        val dieselDed: Double,
        val totalDed: Double,
        val perDiemDays: Int,
        val perDiemAmt: Double,
        val taxable: Double,
        val seTax: Double,
        val fedTax: Double,
        val totalOwed: Double,
        val daysUntil: Int,
        val nextDate: String,
        val reserved: Double,
        val shortfall: Double,
    )

    private fun Throwable.toUiMessage(): String =
        localizedMessage ?: message ?: javaClass.simpleName

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

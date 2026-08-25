package com.truckerload.presentation.screens.paycheck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.domain.model.Paycheck
import com.truckerload.domain.model.PaycheckJournalFilter
import com.truckerload.utils.getCurrentWeekNumberAndYear
import com.truckerload.utils.getWeekRange
import com.truckerload.utils.shiftWeekNumberAndYear
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class PaycheckJournalUiState(
    val weekNumber: Int,
    val year: Int,
    val weekLabel: String,
    val showAllWeeks: Boolean,
    val searchQuery: String,
    val entries: List<Paycheck>,
    val total: Double,
)

@HiltViewModel
class PaycheckJournalViewModel @Inject constructor(
    paycheckRepository: PaycheckRepository,
) : ViewModel() {

    private val initialWeek = getCurrentWeekNumberAndYear()
    private val weekNumber = MutableStateFlow(initialWeek.first)
    private val year = MutableStateFlow(initialWeek.second)
    private val allWeeks = MutableStateFlow(true)
    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<PaycheckJournalUiState> = combine(
        paycheckRepository.getAllPaychecks(),
        weekNumber,
        year,
        allWeeks,
        searchQuery,
    ) { all, week, yr, showAll, query ->
        val scoped = if (showAll) {
            PaycheckJournalFilter.all(all)
        } else {
            PaycheckJournalFilter.forWeek(all, week, yr)
        }
        val visible = PaycheckJournalFilter.matching(scoped, query)
        val (_, _, weekLabel) = getWeekRange(week, yr)
        PaycheckJournalUiState(
            weekNumber = week,
            year = yr,
            weekLabel = weekLabel,
            showAllWeeks = showAll,
            searchQuery = query,
            entries = visible,
            total = scoped.sumOf { it.netAmount },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PaycheckJournalUiState(
            weekNumber = initialWeek.first,
            year = initialWeek.second,
            weekLabel = getWeekRange(initialWeek.first, initialWeek.second).third,
            showAllWeeks = true,
            searchQuery = "",
            entries = emptyList(),
            total = 0.0,
        ),
    )

    fun selectPreviousWeek() = shiftWeek(-1)

    fun selectNextWeek() = shiftWeek(1)

    fun showAllWeeks() {
        allWeeks.value = true
    }

    fun setSearchQuery(value: String) {
        searchQuery.value = value
    }

    private fun shiftWeek(delta: Int) {
        val (week, yr) = shiftWeekNumberAndYear(weekNumber.value, year.value, delta)
        weekNumber.value = week
        year.value = yr
        allWeeks.value = false
    }
}

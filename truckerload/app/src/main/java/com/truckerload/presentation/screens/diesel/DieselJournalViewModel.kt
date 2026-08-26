package com.truckerload.presentation.screens.diesel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.data.repository.DieselRepository
import com.truckerload.domain.model.Diesel
import com.truckerload.domain.model.DieselJournalFilter
import com.truckerload.domain.week.WeekStartRuntime
import com.truckerload.utils.getCurrentWeekNumberAndYear
import com.truckerload.utils.getWeekNumberAndYearFromDate
import com.truckerload.utils.getWeekRange
import com.truckerload.utils.shiftWeekNumberAndYear
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DieselJournalUiState(
    val weekNumber: Int,
    val year: Int,
    val weekLabel: String,
    val selectedDateIso: String?,
    val entries: List<Diesel>,
    val weekTotal: Double,
)

@HiltViewModel
class DieselJournalViewModel @Inject constructor(
    dieselRepository: DieselRepository,
) : ViewModel() {

    private val initialWeek = getCurrentWeekNumberAndYear(WeekStartRuntime.diesel)
    private val weekNumber = MutableStateFlow(initialWeek.first)
    private val year = MutableStateFlow(initialWeek.second)
    private val selectedDateIso = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            WeekStartRuntime.revision.collect {
                val (week, yr) = getCurrentWeekNumberAndYear(WeekStartRuntime.diesel)
                weekNumber.value = week
                year.value = yr
                selectedDateIso.value = null
            }
        }
    }

    val uiState: StateFlow<DieselJournalUiState> = combine(
        dieselRepository.getAllDiesel(),
        weekNumber,
        year,
        selectedDateIso,
        WeekStartRuntime.revision,
    ) { all, week, yr, dateIso, _ ->
        val inWeek = DieselJournalFilter.forWeek(all, week, yr)
        val visible = if (dateIso == null) inWeek else DieselJournalFilter.forDate(inWeek, dateIso)
        val (_, _, weekLabel) = getWeekRange(week, yr, WeekStartRuntime.diesel)
        DieselJournalUiState(
            weekNumber = week,
            year = yr,
            weekLabel = weekLabel,
            selectedDateIso = dateIso,
            entries = visible,
            weekTotal = inWeek.sumOf { it.totalAmount },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DieselJournalUiState(
            weekNumber = initialWeek.first,
            year = initialWeek.second,
            weekLabel = getWeekRange(initialWeek.first, initialWeek.second, WeekStartRuntime.diesel).third,
            selectedDateIso = null,
            entries = emptyList(),
            weekTotal = 0.0,
        ),
    )

    fun selectPreviousWeek() = shiftWeek(-1)

    fun selectNextWeek() = shiftWeek(1)

    fun clearDateFilter() {
        selectedDateIso.value = null
    }

    fun selectDate(dateIso: String) {
        val (week, yr) = getWeekNumberAndYearFromDate(dateIso, WeekStartRuntime.diesel)
        weekNumber.value = week
        year.value = yr
        selectedDateIso.value = dateIso
    }

    private fun shiftWeek(delta: Int) {
        val (week, yr) = shiftWeekNumberAndYear(weekNumber.value, year.value, delta, WeekStartRuntime.diesel)
        weekNumber.value = week
        year.value = yr
        selectedDateIso.value = null
    }
}

package com.truckerload.presentation.screens.home

import com.truckerload.domain.model.Load
import com.truckerload.utils.getCurrentWeekStartAndEnd
import com.truckerload.utils.getLastWeekStartAndEnd
import com.truckerload.utils.parseDateFromQuery
import com.truckerload.utils.getLoadDateRange
import com.truckerload.utils.getYesterdayDate
import java.util.Calendar

/**
 * Фильтрация грузов. Логика вынесена из ViewModel.
 */
class LoadFilterUseCase {

    fun filterLoads(
        loads: List<Load>,
        filter: LoadFilter,
        searchQuery: String,
        selectedDate: String?,
        selectedWeekStart: String?,
        selectedWeekEnd: String?,
        selectedYear: Int?,
        dateIndex: Map<String, List<Load>>? = null
    ): List<Load> {
        var list = loads

        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim()
            val parsedDate = parseDateFromQuery(q)
            list = if (parsedDate != null) {
                list.filter { it.date == parsedDate }
            } else {
                val qLower = q.lowercase()
                list.filter {
                    it.tripId.lowercase().contains(qLower) ||
                        it.pointA.lowercase().contains(qLower) ||
                        it.pointB.lowercase().contains(qLower) ||
                        it.date.contains(q)
                }
            }
        }

        list = when (filter) {
            LoadFilter.ALL -> {
                if (selectedYear != null) {
                    list.filter { it.date.length >= 4 && it.date.substring(0, 4).toIntOrNull() == selectedYear }
                } else list
            }
            LoadFilter.YESTERDAY -> list.filter { it.date == getYesterdayDate() }
            LoadFilter.THIS_WEEK -> {
                val (weekStart, weekEnd) = getCurrentWeekStartAndEnd()
                list.filter { load -> load.date.length >= 10 && load.date >= weekStart && load.date <= weekEnd }
            }
            LoadFilter.LAST_WEEK -> {
                val (start, end) = getLastWeekStartAndEnd()
                list.filter { load -> load.date.length >= 10 && load.date >= start && load.date <= end }
            }
            LoadFilter.THIS_MONTH -> {
                val cal = Calendar.getInstance()
                val prefix = "%04d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
                list.filter { it.date.startsWith(prefix) }
            }
            LoadFilter.CALENDAR_WEEK -> {
                if (selectedWeekStart != null && selectedWeekEnd != null) {
                    list.filter { load -> load.date.length >= 10 && load.date >= selectedWeekStart && load.date <= selectedWeekEnd }
                } else list
            }
            LoadFilter.CALENDAR_DATE -> {
                if (selectedDate != null && dateIndex != null) {
                    dateIndex[selectedDate] ?: emptyList()
                } else if (selectedDate != null) {
                    list.filter { load -> selectedDate in getLoadDateRange(load) }
                } else list
            }
        }

        return list.sortedWith(compareByDescending<Load> { it.date }.thenByDescending { it.parsedAt })
    }

    data class Totals(
        val loadCount: Int,
        val totalRate: Double,
        val totalMiles: Double,
        val avgRpm: Double = 0.0
    ) {
        val avgRpmFormatted: String
            get() = if (totalMiles > 0) "$${String.format("%.2f", totalRate / totalMiles)} / mi" else "—"
    }

    fun calculateTotals(loads: List<Load>): Totals {
        val totalRate = loads.sumOf { it.totalRate }
        val totalMiles = loads.sumOf { it.totalMiles }
        return Totals(
            loadCount = loads.size,
            totalRate = totalRate,
            totalMiles = totalMiles,
            avgRpm = if (totalMiles > 0) totalRate / totalMiles else 0.0
        )
    }
}

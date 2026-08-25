package com.truckerload.domain.filter

import com.truckerload.domain.model.Load
import com.truckerload.utils.getCurrentWeekNumberAndYear
import com.truckerload.utils.getLoadDateRange
import com.truckerload.utils.getPreviousWeekNumberAndYear
import com.truckerload.utils.getWeekNumberAndYearFromDate
import com.truckerload.utils.getYesterdayDate
import com.truckerload.utils.isLoadInWeek
import com.truckerload.utils.parseDateFromQuery
import java.util.Calendar
import java.util.Locale

/** Фильтрация и агрегация грузов — чистая domain-логика без UI-зависимостей. */
class LoadFilterUseCase {

    fun filterLoads(
        loads: List<Load>,
        filter: LoadFilter,
        searchQuery: String,
        selectedDate: String?,
        selectedWeekStart: String?,
        selectedWeekEnd: String?,
        selectedYear: Int?,
        dateIndex: Map<String, List<Load>>? = null,
    ): List<Load> {
        var list = loads

        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim()
            val parsedDate = parseDateFromQuery(q)
            list = if (parsedDate != null) {
                list.filter { parsedDate in getLoadDateRange(it) }
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
                } else {
                    list
                }
            }
            LoadFilter.YESTERDAY -> list.filter { getYesterdayDate() in getLoadDateRange(it) }
            LoadFilter.THIS_WEEK -> {
                val (week, year) = getCurrentWeekNumberAndYear()
                list.filter { isLoadInWeek(it, week, year) }
            }
            LoadFilter.LAST_WEEK -> {
                val (week, year) = getPreviousWeekNumberAndYear()
                list.filter { isLoadInWeek(it, week, year) }
            }
            LoadFilter.THIS_MONTH -> {
                val cal = Calendar.getInstance()
                val prefix = "%04d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
                list.filter { load -> getLoadDateRange(load).any { it.startsWith(prefix) } }
            }
            LoadFilter.CALENDAR_WEEK -> {
                if (selectedWeekStart != null) {
                    val (week, year) = getWeekNumberAndYearFromDate(selectedWeekStart)
                    list.filter { isLoadInWeek(it, week, year) }
                } else {
                    list
                }
            }
            LoadFilter.CALENDAR_DATE -> {
                if (selectedDate != null) {
                    list.filter { load -> selectedDate in getLoadDateRange(load) }
                } else {
                    list
                }
            }
            // Active and completed: a load that was disputed stays findable forever.
            LoadFilter.DISPUTE -> list.filter { it.isDispute }
        }

        return list.sortedWith(compareByDescending<Load> { it.date }.thenByDescending { it.parsedAt })
    }

    data class Totals(
        val loadCount: Int,
        val totalRate: Double,
        val totalMiles: Double,
        val avgRpm: Double = 0.0,
    ) {
        val avgRpmFormatted: String
            get() = if (totalMiles > 0) "$${String.format(Locale.US, "%.2f", totalRate / totalMiles)} / mi" else "—"
    }

    fun calculateTotals(loads: List<Load>): Totals {
        val totalRate = loads.sumOf { it.totalRate }
        val totalMiles = loads.sumOf { it.totalMiles }
        return Totals(
            loadCount = loads.size,
            totalRate = totalRate,
            totalMiles = totalMiles,
            avgRpm = if (totalMiles > 0) totalRate / totalMiles else 0.0,
        )
    }
}

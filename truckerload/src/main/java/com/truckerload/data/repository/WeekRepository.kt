package com.truckerload.data.repository

import com.truckerload.domain.model.WeekSummary
import com.truckerload.utils.getWeekRange
import com.truckerload.utils.getWeeksInMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class WeekRepository(
    private val loadRepository: LoadRepository,
    private val paycheckRepository: PaycheckRepository,
    private val dieselRepository: DieselRepository
) {

    fun getWeekSummary(weekNumber: Int, year: Int): Flow<WeekSummary> {
        val (weekStartDate, weekEndDate, weekLabel) = getWeekRange(weekNumber, year)
        val loads = loadRepository.getLoadsByWeek(weekNumber, year)
        val paychecks = paycheckRepository.getPaychecksForWeek(weekNumber, year)
        val diesel = dieselRepository.getDieselForWeek(weekNumber, year)
        return combine(loads, paychecks, diesel) { loadList, paycheckList, dieselList ->
            val totalLoadRate = loadList.sumOf { it.totalRate }
            val totalMiles = loadList.sumOf { it.totalMiles }
            // Зарплата — только из Paycheck.netAmount (поле "Зарплата")
            val paycheckAmount = paycheckList.firstOrNull()?.netAmount ?: 0.0
            val hasPaycheck = paycheckList.isNotEmpty()
            val dieselAmount = dieselList.sumOf { it.totalAmount }
            val hasDiesel = dieselList.isNotEmpty()
            WeekSummary(
                weekNumber = weekNumber,
                year = year,
                weekLabel = weekLabel,
                weekStartDate = weekStartDate,
                weekEndDate = weekEndDate,
                loadsCount = loadList.size,
                totalLoadRate = totalLoadRate,
                totalMiles = totalMiles,
                paycheckAmount = paycheckAmount,
                hasPaycheck = hasPaycheck,
                dieselAmount = dieselAmount,
                hasDiesel = hasDiesel,
                netProfit = paycheckAmount - dieselAmount
            )
        }
    }

    suspend fun getWeekSummaryOnce(weekNumber: Int, year: Int): WeekSummary {
        val (weekStartDate, weekEndDate, weekLabel) = getWeekRange(weekNumber, year)
        val loadList = loadRepository.getLoadsByWeek(weekNumber, year).first()
        val paycheckList = paycheckRepository.getPaychecksForWeek(weekNumber, year).first()
        val dieselList = dieselRepository.getDieselForWeek(weekNumber, year).first()
        val totalLoadRate = loadList.sumOf { it.totalRate }
        val totalMiles = loadList.sumOf { it.totalMiles }
        // Зарплата — только из Paycheck.netAmount (поле "Зарплата")
        val paycheckAmount = paycheckList.firstOrNull()?.netAmount ?: 0.0
        val dieselAmount = dieselList.sumOf { it.totalAmount }
        return WeekSummary(
            weekNumber = weekNumber,
            year = year,
            weekLabel = weekLabel,
            weekStartDate = weekStartDate,
            weekEndDate = weekEndDate,
            loadsCount = loadList.size,
            totalLoadRate = totalLoadRate,
            totalMiles = totalMiles,
            paycheckAmount = paycheckAmount,
            hasPaycheck = paycheckList.isNotEmpty(),
            dieselAmount = dieselAmount,
            hasDiesel = dieselList.isNotEmpty(),
            netProfit = paycheckAmount - dieselAmount
        )
    }

    /** Сводки по всем неделям месяца для календаря. */
    suspend fun getWeeksInMonthSummaries(month: Int, year: Int): List<WeekSummary> {
        val weeks = getWeeksInMonth(year, month)
        return weeks.map { (wn, wy) -> getWeekSummaryOnce(wn, wy) }
    }
}

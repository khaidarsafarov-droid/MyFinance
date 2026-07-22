package com.truckerload.data.repository

import com.truckerload.domain.model.PeriodSummary
import com.truckerload.domain.model.WeekSummary
import com.truckerload.utils.getWeekRange
import com.truckerload.utils.getWeeksInMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
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
            // Paycheck amount comes only from Paycheck.netAmount.
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
        }.flowOn(Dispatchers.IO)
    }

    suspend fun getWeekSummaryOnce(weekNumber: Int, year: Int): WeekSummary {
        val (weekStartDate, weekEndDate, weekLabel) = getWeekRange(weekNumber, year)
        val loadList = loadRepository.getLoadsByWeek(weekNumber, year).first()
        val paycheckList = paycheckRepository.getPaychecksForWeek(weekNumber, year).first()
        val dieselList = dieselRepository.getDieselForWeek(weekNumber, year).first()
        val totalLoadRate = loadList.sumOf { it.totalRate }
        val totalMiles = loadList.sumOf { it.totalMiles }
        // Paycheck amount comes only from Paycheck.netAmount.
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

    /** Summaries for all weeks in the selected month. */
    suspend fun getWeeksInMonthSummaries(month: Int, year: Int): List<WeekSummary> {
        val weeks = getWeeksInMonth(year, month)
        return weeks.map { (wn, wy) -> getWeekSummaryOnce(wn, wy) }
    }

    /** Summary for an arbitrary date range (month/year view). */
    suspend fun getPeriodSummaryOnce(
        startDate: String,
        endDate: String,
        periodLabel: String
    ): PeriodSummary {
        val loadList = loadRepository.getLoadsByDateRangeOnce(startDate, endDate)
        val allPaychecks = paycheckRepository.getAllPaychecksOnce()
        val allDiesel = dieselRepository.getAllDieselOnce()
        val paychecksInRange = allPaychecks.filter { it.weekEndDate >= startDate && it.weekStartDate <= endDate }
        val dieselInRange = allDiesel.filter { it.weekEndDate >= startDate && it.weekStartDate <= endDate }
        val totalLoadRate = loadList.sumOf { it.totalRate }
        val totalMiles = loadList.sumOf { it.totalMiles }
        val paycheckAmount = paychecksInRange.sumOf { it.netAmount }
        val dieselAmount = dieselInRange.sumOf { it.totalAmount }
        return PeriodSummary(
            periodLabel = periodLabel,
            startDate = startDate,
            endDate = endDate,
            loadsCount = loadList.size,
            totalLoadRate = totalLoadRate,
            totalMiles = totalMiles,
            paycheckAmount = paycheckAmount,
            dieselAmount = dieselAmount,
            netProfit = paycheckAmount - dieselAmount
        )
    }
}

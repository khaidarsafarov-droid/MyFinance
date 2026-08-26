package com.truckerload.data.repository

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.PeriodSummary
import com.truckerload.domain.model.WeekSummary
import com.truckerload.domain.week.WeekStartRebinder
import com.truckerload.utils.getWeekRange
import com.truckerload.utils.getWeeksInMonth
import com.truckerload.utils.weeksEndingInRange
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
        val diesel = dieselRepository.getAllDiesel()
        return combine(loads, paychecks, diesel) { loadList, paycheckList, allDiesel ->
            val dieselList = allDiesel.filter {
                WeekStartRebinder.dieselIsoInRange(it.addedAt, weekStartDate, weekEndDate)
            }
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
        val dieselList = dieselRepository.getAllDieselOnce().filter {
            WeekStartRebinder.dieselIsoInRange(it.addedAt, weekStartDate, weekEndDate)
        }
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

    /**
     * Month/year totals from reporting weeks whose Saturday falls in [startDate]…[endDate].
     * Same week math as [getWeekSummaryOnce], so a spanning Sun–Sat week is counted once.
     */
    suspend fun getPeriodSummaryOnce(
        startDate: String,
        endDate: String,
        periodLabel: String
    ): PeriodSummary {
        val summaries = weeksEndingInRange(startDate, endDate).map { (wn, wy) ->
            getWeekSummaryOnce(wn, wy)
        }
        val paycheckAmount = summaries.sumOf { it.paycheckAmount }
        val dieselAmount = summaries.sumOf { it.dieselAmount }
        return PeriodSummary(
            periodLabel = periodLabel,
            startDate = summaries.minOfOrNull { it.weekStartDate } ?: startDate,
            endDate = summaries.maxOfOrNull { it.weekEndDate } ?: endDate,
            loadsCount = summaries.sumOf { it.loadsCount },
            totalLoadRate = summaries.sumOf { it.totalLoadRate },
            totalMiles = summaries.sumOf { it.totalMiles },
            paycheckAmount = paycheckAmount,
            dieselAmount = dieselAmount,
            netProfit = paycheckAmount - dieselAmount
        )
    }

    /** Loads whose reporting week is owned by [startDate]…[endDate] (Saturday in range). */
    suspend fun getPeriodLoadsOnce(startDate: String, endDate: String): List<Load> =
        weeksEndingInRange(startDate, endDate).flatMap { (wn, wy) ->
            loadRepository.getLoadsByWeek(wn, wy).first()
        }
}

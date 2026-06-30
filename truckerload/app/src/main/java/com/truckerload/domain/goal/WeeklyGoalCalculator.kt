package com.truckerload.domain.goal

import com.truckerload.domain.model.Load
import com.truckerload.presentation.screens.home.LoadFilterUseCase
import com.truckerload.utils.getCurrentWeekNumberAndYear
import com.truckerload.utils.getDaysActiveForWeek
import com.truckerload.utils.getDaysRemainingForWeek
import com.truckerload.utils.getWeekRange
import com.truckerload.utils.isLoadInWeek
import kotlin.math.min

object WeeklyGoalCalculator {

    /** Gross per active day (PU → DEL durations, ceil, min 1 day per load). */
    fun calculateDailyPace(weekLoads: List<Load>): Double =
        LoadYieldCalculator.actualDailyYield(weekLoads)

    fun calculate(
        targetAmount: Double,
        weekLoads: List<Load>,
        weekNumber: Int,
        year: Int,
        sqlYield: WeekYieldSnapshot? = null
    ): WeeklyGoalProgress {
        val totals = LoadFilterUseCase().calculateTotals(weekLoads)
        val currentGross = GoalMoneyMath.roundMoney(sqlYield?.totalGross ?: totals.totalRate)
        val (_, _, weekLabel) = getWeekRange(weekNumber, year)

        val daysActiveCalendar = getDaysActiveForWeek(weekNumber, year)
        val daysRemaining = getDaysRemainingForWeek(weekNumber, year)
        val totalActiveDays = if (sqlYield != null && sqlYield.totalActiveDays > 0.0) {
            sqlYield.totalActiveDays
        } else {
            LoadYieldCalculator.totalActiveDays(weekLoads)
        }
        val actualDailyYield = if (sqlYield != null && sqlYield.totalActiveDays > 0.0) {
            sqlYield.actualDailyYield
        } else {
            calculateDailyPace(weekLoads)
        }

        val dailyTargetNeeded = GoalMoneyMath.dailyTarget(targetAmount, currentGross, daysRemaining)
        val expectedGrossByNow = GoalMoneyMath.expectedGrossByNow(targetAmount, daysActiveCalendar)
        val remaining = GoalMoneyMath.roundMoney((targetAmount - currentGross).coerceAtLeast(0.0))

        val progressPercent = if (targetAmount > 0) {
            min(100f, (currentGross / targetAmount * 100).toFloat())
        } else {
            0f
        }

        val paceStatus = when {
            targetAmount <= 0 -> PaceStatus.BEHIND
            currentGross >= targetAmount -> PaceStatus.GOAL_MET
            weekLoads.isEmpty() || actualDailyYield <= 0.0 -> PaceStatus.BEHIND
            actualDailyYield >= dailyTargetNeeded * 1.05 -> PaceStatus.AHEAD
            actualDailyYield >= dailyTargetNeeded * 0.92 -> PaceStatus.ON_TRACK
            else -> PaceStatus.BEHIND
        }

        return WeeklyGoalProgress(
            targetAmount = GoalMoneyMath.roundMoney(targetAmount),
            currentGross = currentGross,
            progressPercent = progressPercent,
            remainingAmount = remaining,
            daysActiveCalendar = daysActiveCalendar,
            daysRemainingInWeek = daysRemaining,
            totalActiveDays = kotlin.math.round(totalActiveDays * 10.0) / 10.0,
            actualDailyYield = actualDailyYield,
            dailyTargetNeeded = dailyTargetNeeded,
            expectedGrossByNow = expectedGrossByNow,
            paceStatus = paceStatus,
            weekLabel = weekLabel,
            weekNumber = weekNumber,
            year = year,
            loadsCount = totals.loadCount,
            totalMiles = GoalMoneyMath.roundMoney(totals.totalMiles)
        )
    }

    fun calculateCurrentWeek(
        targetAmount: Double,
        allLoads: List<Load>,
        sqlYield: WeekYieldSnapshot? = null
    ): WeeklyGoalProgress {
        val (weekNumber, year) = getCurrentWeekNumberAndYear()
        val weekLoads = allLoads.filter { isLoadInWeek(it, weekNumber, year) }
        return calculate(targetAmount, weekLoads, weekNumber, year, sqlYield)
    }
}

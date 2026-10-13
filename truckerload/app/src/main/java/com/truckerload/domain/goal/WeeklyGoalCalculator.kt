package com.truckerload.domain.goal

import com.truckerload.domain.model.Load
import com.truckerload.domain.filter.LoadFilterUseCase
import com.truckerload.utils.getCurrentWeekNumberAndYear
import com.truckerload.utils.getDaysActiveForWeek
import com.truckerload.utils.getDaysRemainingForWeek
import com.truckerload.utils.getWeekRange
import com.truckerload.utils.isLoadInWeek
import java.time.LocalDate
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
        val loadGross = totals.totalRate
        val sqlGross = sqlYield?.totalGross ?: 0.0
        // Prefer the larger gross: SQL misses weekNumber=0 rows that isLoadInWeek still includes.
        val currentGross = GoalMoneyMath.roundMoney(maxOf(sqlGross, loadGross))
        val (weekStartIso, weekEndIso, weekLabel) = getWeekRange(weekNumber, year)

        val (currentWeek, currentYear) = getCurrentWeekNumberAndYear()
        val isCurrentWeek = weekNumber == currentWeek && year == currentYear
        val daysActiveCalendar = getDaysActiveForWeek(weekNumber, year)
        val daysRemaining = getDaysRemainingForWeek(weekNumber, year)
        // Days on the road are unique calendar days through today, not the SQL sum of
        // each load's planned pickup-to-delivery length.
        val onRoadDays = LoadYieldCalculator.onRoadDays(
            loads = weekLoads,
            weekStart = LocalDate.parse(weekStartIso),
            weekEnd = LocalDate.parse(weekEndIso),
            today = LocalDate.now(),
        )
        val totalActiveDays = onRoadDays.toDouble()
        val actualDailyYield = if (onRoadDays > 0) {
            GoalMoneyMath.roundMoney(currentGross / totalActiveDays)
        } else {
            0.0
        }

        // FIX: past weeks used daysRemaining=1 → "needed/day" = entire remaining gap
        val dailyTargetNeeded = if (isCurrentWeek) {
            GoalMoneyMath.dailyTarget(targetAmount, currentGross, daysRemaining)
        } else {
            0.0
        }
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
            // Closed week: binary met / behind — no mid-week pace bands
            !isCurrentWeek -> PaceStatus.BEHIND
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

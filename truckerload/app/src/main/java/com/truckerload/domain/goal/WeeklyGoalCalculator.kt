package com.truckerload.domain.goal

import com.truckerload.domain.money.Money
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
    fun calculateDailyPace(weekLoads: List<Load>): Money =
        LoadYieldCalculator.actualDailyYield(weekLoads)

    fun calculate(
        targetAmount: Money,
        weekLoads: List<Load>,
        weekNumber: Int,
        year: Int,
        sqlYield: WeekYieldSnapshot? = null
    ): WeeklyGoalProgress {
        val totals = LoadFilterUseCase().calculateTotals(weekLoads)
        val loadGross = totals.totalRate
        val sqlGross = sqlYield?.totalGross ?: Money.ZERO
        // Prefer the larger gross: SQL misses weekNumber=0 rows that isLoadInWeek still includes.
        val currentGross = maxOf(sqlGross, loadGross)
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
            currentGross.dividedBy(onRoadDays)
        } else {
            Money.ZERO
        }

        // Past weeks do not invent a daily target from a one-day remainder.
        val dailyTargetNeeded = if (isCurrentWeek) {
            GoalMoneyMath.dailyTarget(targetAmount, currentGross, daysRemaining)
        } else {
            Money.ZERO
        }
        val expectedGrossByNow = GoalMoneyMath.expectedGrossByNow(targetAmount, daysActiveCalendar)
        val remaining = (targetAmount - currentGross).coerceAtLeast(Money.ZERO)

        val progressPercent = if (targetAmount.cents > 0L) {
            min(100f, (currentGross.cents.toFloat() / targetAmount.cents.toFloat() * 100f))
        } else {
            0f
        }

        val paceStatus = when {
            targetAmount.cents <= 0L -> PaceStatus.BEHIND
            currentGross >= targetAmount -> PaceStatus.GOAL_MET
            !isCurrentWeek -> PaceStatus.BEHIND
            weekLoads.isEmpty() || actualDailyYield.cents <= 0L -> PaceStatus.BEHIND
            actualDailyYield >= dailyTargetNeeded.scaledBy(1.05) -> PaceStatus.AHEAD
            actualDailyYield >= dailyTargetNeeded.scaledBy(0.92) -> PaceStatus.ON_TRACK
            else -> PaceStatus.BEHIND
        }

        return WeeklyGoalProgress(
            targetAmount = targetAmount,
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
            totalMiles = totals.totalMiles
        )
    }

    fun calculateCurrentWeek(
        targetAmount: Money,
        allLoads: List<Load>,
        sqlYield: WeekYieldSnapshot? = null
    ): WeeklyGoalProgress {
        val (weekNumber, year) = getCurrentWeekNumberAndYear()
        val weekLoads = allLoads.filter { isLoadInWeek(it, weekNumber, year) }
        return calculate(targetAmount, weekLoads, weekNumber, year, sqlYield)
    }
}

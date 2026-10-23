package com.truckerload.widget

import android.content.Context
import com.truckerload.R
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.DEFAULT_WEEKLY_GROSS_GOAL
import com.truckerload.data.preferences.RpmThresholdsStore
import com.truckerload.data.preferences.WeeklyProfitGoalStore
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.data.repository.WeekRepository
import com.truckerload.domain.goal.WeekYieldSnapshot
import com.truckerload.domain.units.DisplayUnits
import com.truckerload.domain.units.MeasurementUnits
import com.truckerload.domain.money.Money
import com.truckerload.domain.goal.WeeklyGoalCalculator
import com.truckerload.utils.getCurrentWeekNumberAndYear
import com.truckerload.utils.getWeekRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Locale

object WidgetStatsLoader {

    suspend fun refresh(context: Context): WidgetStats = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val db = AppDatabase.getInstanceForActiveUser(appContext)
            ?: runCatching { AppDatabase.getInstance(appContext) }.getOrNull()
        if (db == null) {
            val empty = WidgetStats(updatedAtMillis = System.currentTimeMillis())
            WidgetDataStore.save(appContext, empty)
            return@withContext empty
        }
        val loadRepository = LoadRepository(db)
        val weekRepository = WeekRepository(
            loadRepository,
            PaycheckRepository(db),
            DieselRepository(db),
        )
        val (weekNumber, year) = getCurrentWeekNumberAndYear()
        val (weekStartIso, _, weekLabel) = getWeekRange(weekNumber, year)
        val weekLoads = loadRepository.getLoadsByWeek(weekNumber, year).first()
        val weekStart = runCatching { LocalDate.parse(weekStartIso.take(10)) }
            .getOrElse { WidgetWeekDayHelper.startOfWeek() }
        val dayTotals = WidgetDayProjection.totalsByDay(weekLoads, weekStart)
        val weekLoadMask = WidgetDayProjection.maskFromDayTotals(dayTotals)
        val weekSummary = weekRepository.getWeekSummaryOnce(weekNumber, year)
        val sqlAgg = db.loadDao().watchWeekYieldAgg(weekNumber, year).first()
        val sqlYield = WeekYieldSnapshot(Money.fromCents(sqlAgg.totalGross), sqlAgg.totalActiveDays)
        val profitGoal = WeeklyProfitGoalStore(appContext).getGoal().takeIf { it > 0 }
            ?: DEFAULT_WEEKLY_GROSS_GOAL
        val goalProgress = WeeklyGoalCalculator.calculate(
            Money.fromDollars(profitGoal),
            weekLoads,
            weekNumber,
            year,
            sqlYield,
        )
        val grossDollars = goalProgress.currentGross.cents / 100.0
        val rpmTarget = RpmThresholdsStore(appContext).thresholds.value.targetProfit
        val weekRpm = if (goalProgress.totalMiles > 0) grossDollars / goalProgress.totalMiles else 0.0
        val statsLine = if (DisplayUnits.isMetric) {
            appContext.getString(
                R.string.home_stats_header_km,
                goalProgress.loadsCount,
                grossDollars,
                MeasurementUnits.displayDistance(goalProgress.totalMiles),
                if (goalProgress.totalMiles > 0) {
                    String.format(Locale.US, "%.2f", MeasurementUnits.displayRatePerDistance(weekRpm))
                } else {
                    "0.00"
                },
            )
        } else {
            appContext.getString(
                R.string.home_stats_header,
                goalProgress.loadsCount,
                grossDollars,
                goalProgress.totalMiles,
                if (goalProgress.totalMiles > 0) {
                    String.format(Locale.US, "%.2f", weekRpm)
                } else {
                    "0.00"
                },
            )
        }
        WidgetStats(
            loadsCount = goalProgress.loadsCount,
            avgCpm = if (goalProgress.totalMiles > 0) grossDollars / goalProgress.totalMiles else 0.0,
            totalMiles = goalProgress.totalMiles,
            totalLoadRate = grossDollars,
            netProfit = weekSummary.netProfit,
            dieselAmount = weekSummary.dieselAmount,
            weekLabel = weekLabel,
            statsLine = statsLine,
            cpmTarget = rpmTarget,
            weeklyProfitGoal = profitGoal,
            goalProgressPercent = goalProgress.progressPercent,
            goalRemainingAmount = goalProgress.remainingAmount.cents / 100.0,
            goalDailyNeeded = goalProgress.dailyTargetNeeded.cents / 100.0,
            goalActualDailyYield = goalProgress.actualDailyYield.cents / 100.0,
            goalDaysRemaining = goalProgress.daysRemainingInWeek,
            goalPaceStatus = goalProgress.paceStatus.name,
            weekLoadMask = weekLoadMask,
            dayLoads = dayTotals.map { it.loadsCount },
            dayGross = dayTotals.map { it.gross },
            dayMiles = dayTotals.map { it.miles },
            totalActiveDays = goalProgress.totalActiveDays,
            updatedAtMillis = System.currentTimeMillis()
        ).also { WidgetDataStore.save(appContext, it) }
    }
}

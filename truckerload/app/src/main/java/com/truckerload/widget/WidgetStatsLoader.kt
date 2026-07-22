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
import com.truckerload.domain.goal.LoadYieldCalculator
import com.truckerload.domain.goal.WeekYieldSnapshot
import com.truckerload.domain.goal.WeeklyGoalCalculator
import com.truckerload.utils.getCurrentWeekNumberAndYear
import com.truckerload.utils.getWeekRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

object WidgetStatsLoader {

    suspend fun refresh(context: Context): WidgetStats = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val db = AppDatabase.getInstanceForActiveUser(appContext)
            ?: return@withContext WidgetStats(updatedAtMillis = System.currentTimeMillis())
        val loadRepository = LoadRepository(db)
        val weekRepository = WeekRepository(
            loadRepository,
            PaycheckRepository(db),
            DieselRepository(db),
        )
        val (weekNumber, year) = getCurrentWeekNumberAndYear()
        val (_, _, weekLabel) = getWeekRange(weekNumber, year)
        val weekLoads = loadRepository.getLoadsByWeek(weekNumber, year).first()
        val weekSummary = weekRepository.getWeekSummaryOnce(weekNumber, year)
        val sqlAgg = db.loadDao().watchWeekYieldAgg(weekNumber, year).first()
        val sqlYield = WeekYieldSnapshot(sqlAgg.totalGross, sqlAgg.totalActiveDays)
        val profitGoal = WeeklyProfitGoalStore(appContext).getGoal().takeIf { it > 0 }
            ?: DEFAULT_WEEKLY_GROSS_GOAL
        val goalProgress = WeeklyGoalCalculator.calculate(
            profitGoal,
            weekLoads,
            weekNumber,
            year,
            sqlYield,
        )
        val rpmTarget = RpmThresholdsStore(appContext).thresholds.value.targetProfit
        val statsLine = appContext.getString(
            R.string.home_stats_header,
            goalProgress.loadsCount,
            goalProgress.currentGross,
            goalProgress.totalMiles,
            if (goalProgress.totalMiles > 0) {
                String.format("%.2f", goalProgress.currentGross / goalProgress.totalMiles)
            } else {
                "0.00"
            }
        )
        WidgetStats(
            loadsCount = goalProgress.loadsCount,
            avgCpm = if (goalProgress.totalMiles > 0) goalProgress.currentGross / goalProgress.totalMiles else 0.0,
            totalMiles = goalProgress.totalMiles,
            totalLoadRate = goalProgress.currentGross,
            netProfit = weekSummary.netProfit,
            weekLabel = weekLabel,
            statsLine = statsLine,
            cpmTarget = rpmTarget,
            weeklyProfitGoal = profitGoal,
            goalProgressPercent = goalProgress.progressPercent,
            goalRemainingAmount = goalProgress.remainingAmount,
            goalDailyNeeded = goalProgress.dailyTargetNeeded,
            goalActualDailyYield = goalProgress.actualDailyYield,
            goalDaysRemaining = goalProgress.daysRemainingInWeek,
            goalPaceStatus = goalProgress.paceStatus.name,
            totalActiveDays = sqlYield.totalActiveDays.takeIf { it > 0.0 }
                ?: LoadYieldCalculator.totalActiveDays(weekLoads),
            updatedAtMillis = System.currentTimeMillis()
        ).also { WidgetDataStore.save(appContext, it) }
    }
}

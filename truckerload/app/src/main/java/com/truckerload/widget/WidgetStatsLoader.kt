package com.truckerload.widget

import android.content.Context
import com.truckerload.R
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.DEFAULT_WEEKLY_GROSS_GOAL
import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.goal.WeekYieldSnapshot
import com.truckerload.domain.goal.WeeklyGoalCalculator
import com.truckerload.utils.getCurrentWeekNumberAndYear
import com.truckerload.utils.getWeekRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

object WidgetStatsLoader {

    private const val PREFS_GOAL = "truckerload_widget_goals"
    private const val KEY_WEEKLY_PROFIT_GOAL = "weekly_profit_goal"

    suspend fun refresh(context: Context): WidgetStats = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val db = AppDatabase.getInstance(appContext)
        val loadRepository = LoadRepository(db)
        val (weekNumber, year) = getCurrentWeekNumberAndYear()
        val (_, _, weekLabel) = getWeekRange(weekNumber, year)
        val allLoads = loadRepository.getAllLoadsOnce()
        val sqlAgg = db.loadDao().watchWeekYieldAgg(weekNumber, year).first()
        val sqlYield = WeekYieldSnapshot(sqlAgg.totalGross, sqlAgg.totalActiveDays)
        val profitGoal = appContext.getSharedPreferences(PREFS_GOAL, Context.MODE_PRIVATE).let { prefs ->
            if (prefs.contains(KEY_WEEKLY_PROFIT_GOAL)) {
                prefs.getFloat(KEY_WEEKLY_PROFIT_GOAL, 0f).toDouble()
            } else {
                DEFAULT_WEEKLY_GROSS_GOAL
            }
        }
        val goalProgress = WeeklyGoalCalculator.calculateCurrentWeek(profitGoal, allLoads, sqlYield)
        val rpmTarget = appContext.getSharedPreferences("truckerload_settings", Context.MODE_PRIVATE)
            .getFloat("target_profit_threshold", 2.5f)
            .toDouble()
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
            netProfit = 0.0,
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
            updatedAtMillis = System.currentTimeMillis()
        ).also { WidgetDataStore.save(appContext, it) }
    }
}

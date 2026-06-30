package com.truckerload.widget

import android.content.Context
import com.truckerload.data.preferences.DEFAULT_WEEKLY_GROSS_GOAL

object WidgetDataStore {
    private const val PREFS = "truckerload_widget"
    private const val KEY_LOADS = "loads_count"
    private const val KEY_CPM = "avg_cpm"
    private const val KEY_MILES = "total_miles"
    private const val KEY_TOTAL = "total_load_rate"
    private const val KEY_NET = "net_profit"
    private const val KEY_WEEK = "week_label"
    private const val KEY_STATS_LINE = "stats_line"
    private const val KEY_CPM_TARGET = "cpm_target"
    private const val KEY_PROFIT_GOAL = "profit_goal"
    private const val KEY_GOAL_PROGRESS = "goal_progress_percent"
    private const val KEY_GOAL_REMAINING = "goal_remaining"
    private const val KEY_GOAL_DAILY = "goal_daily_needed"
    private const val KEY_GOAL_ACTUAL = "goal_actual_daily"
    private const val KEY_GOAL_DAYS = "goal_days_remaining"
    private const val KEY_GOAL_PACE = "goal_pace_status"
    private const val KEY_UPDATED = "updated_at"

    fun save(context: Context, stats: WidgetStats) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_LOADS, stats.loadsCount)
            .putFloat(KEY_CPM, stats.avgCpm.toFloat())
            .putFloat(KEY_MILES, stats.totalMiles.toFloat())
            .putFloat(KEY_TOTAL, stats.totalLoadRate.toFloat())
            .putFloat(KEY_NET, stats.netProfit.toFloat())
            .putString(KEY_WEEK, stats.weekLabel)
            .putString(KEY_STATS_LINE, stats.statsLine)
            .putFloat(KEY_CPM_TARGET, stats.cpmTarget.toFloat())
            .putFloat(KEY_PROFIT_GOAL, stats.weeklyProfitGoal.toFloat())
            .putFloat(KEY_GOAL_PROGRESS, stats.goalProgressPercent)
            .putFloat(KEY_GOAL_REMAINING, stats.goalRemainingAmount.toFloat())
            .putFloat(KEY_GOAL_DAILY, stats.goalDailyNeeded.toFloat())
            .putFloat(KEY_GOAL_ACTUAL, stats.goalActualDailyYield.toFloat())
            .putInt(KEY_GOAL_DAYS, stats.goalDaysRemaining)
            .putString(KEY_GOAL_PACE, stats.goalPaceStatus)
            .putLong(KEY_UPDATED, stats.updatedAtMillis)
            .commit()
    }

    fun load(context: Context): WidgetStats {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return WidgetStats(
            loadsCount = prefs.getInt(KEY_LOADS, 0),
            avgCpm = prefs.getFloat(KEY_CPM, 0f).toDouble(),
            totalMiles = prefs.getFloat(KEY_MILES, 0f).toDouble(),
            totalLoadRate = prefs.getFloat(KEY_TOTAL, 0f).toDouble(),
            netProfit = prefs.getFloat(KEY_NET, 0f).toDouble(),
            weekLabel = prefs.getString(KEY_WEEK, "").orEmpty(),
            statsLine = prefs.getString(KEY_STATS_LINE, "").orEmpty(),
            cpmTarget = prefs.getFloat(KEY_CPM_TARGET, 2.5f).toDouble(),
            weeklyProfitGoal = prefs.getFloat(KEY_PROFIT_GOAL, DEFAULT_WEEKLY_GROSS_GOAL.toFloat()).toDouble(),
            goalProgressPercent = prefs.getFloat(KEY_GOAL_PROGRESS, 0f),
            goalRemainingAmount = prefs.getFloat(KEY_GOAL_REMAINING, 0f).toDouble(),
            goalDailyNeeded = prefs.getFloat(KEY_GOAL_DAILY, 0f).toDouble(),
            goalActualDailyYield = prefs.getFloat(KEY_GOAL_ACTUAL, 0f).toDouble(),
            goalDaysRemaining = prefs.getInt(KEY_GOAL_DAYS, 0),
            goalPaceStatus = prefs.getString(KEY_GOAL_PACE, "").orEmpty(),
            updatedAtMillis = prefs.getLong(KEY_UPDATED, 0L)
        )
    }
}

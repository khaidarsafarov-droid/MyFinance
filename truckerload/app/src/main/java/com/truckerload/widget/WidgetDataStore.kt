package com.truckerload.widget

import android.content.Context
import androidx.core.content.edit
import com.truckerload.data.preferences.AccountIds
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.DEFAULT_WEEKLY_GROSS_GOAL

/**
 * Cached widget stats, scoped per active account so account switch does not show
 * another user's week totals.
 */
object WidgetDataStore {
    private const val LEGACY_PREFS = "truckerload_widget"
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
    private const val KEY_ACTIVE_DAYS = "total_active_days"
    private const val KEY_UPDATED = "updated_at"

    private fun prefs(context: Context): android.content.SharedPreferences {
        val userId = AuthStore(context).currentUserIdOrNull() ?: AccountIds.LOCAL_DEV
        val name = "truckerload_widget_${AccountIds.sanitizeFilePart(userId)}"
        return context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE).also { scoped ->
            migrateFromLegacyIfEmpty(context.applicationContext, scoped)
        }
    }

    private fun migrateFromLegacyIfEmpty(
        context: Context,
        scoped: android.content.SharedPreferences,
    ) {
        if (scoped.contains(KEY_UPDATED) || scoped.contains(KEY_LOADS)) return
        val legacy = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
        if (legacy.all.isEmpty()) return
        scoped.edit {
            putInt(KEY_LOADS, legacy.getInt(KEY_LOADS, 0))
            putFloat(KEY_CPM, legacy.getFloat(KEY_CPM, 0f))
            putFloat(KEY_MILES, legacy.getFloat(KEY_MILES, 0f))
            putFloat(KEY_TOTAL, legacy.getFloat(KEY_TOTAL, 0f))
            putFloat(KEY_NET, legacy.getFloat(KEY_NET, 0f))
            putString(KEY_WEEK, legacy.getString(KEY_WEEK, "").orEmpty())
            putString(KEY_STATS_LINE, legacy.getString(KEY_STATS_LINE, "").orEmpty())
            putFloat(KEY_CPM_TARGET, legacy.getFloat(KEY_CPM_TARGET, 2.5f))
            putFloat(KEY_PROFIT_GOAL, legacy.getFloat(KEY_PROFIT_GOAL, DEFAULT_WEEKLY_GROSS_GOAL.toFloat()))
            putFloat(KEY_GOAL_PROGRESS, legacy.getFloat(KEY_GOAL_PROGRESS, 0f))
            putFloat(KEY_GOAL_REMAINING, legacy.getFloat(KEY_GOAL_REMAINING, 0f))
            putFloat(KEY_GOAL_DAILY, legacy.getFloat(KEY_GOAL_DAILY, 0f))
            putFloat(KEY_GOAL_ACTUAL, legacy.getFloat(KEY_GOAL_ACTUAL, 0f))
            putInt(KEY_GOAL_DAYS, legacy.getInt(KEY_GOAL_DAYS, 0))
            putString(KEY_GOAL_PACE, legacy.getString(KEY_GOAL_PACE, "").orEmpty())
            putFloat(KEY_ACTIVE_DAYS, legacy.getFloat(KEY_ACTIVE_DAYS, 0f))
            putLong(KEY_UPDATED, legacy.getLong(KEY_UPDATED, 0L))
        }
    }

    fun save(context: Context, stats: WidgetStats) {
        prefs(context).edit(commit = true) {
            putInt(KEY_LOADS, stats.loadsCount)
            putFloat(KEY_CPM, stats.avgCpm.toFloat())
            putFloat(KEY_MILES, stats.totalMiles.toFloat())
            putFloat(KEY_TOTAL, stats.totalLoadRate.toFloat())
            putFloat(KEY_NET, stats.netProfit.toFloat())
            putString(KEY_WEEK, stats.weekLabel)
            putString(KEY_STATS_LINE, stats.statsLine)
            putFloat(KEY_CPM_TARGET, stats.cpmTarget.toFloat())
            putFloat(KEY_PROFIT_GOAL, stats.weeklyProfitGoal.toFloat())
            putFloat(KEY_GOAL_PROGRESS, stats.goalProgressPercent)
            putFloat(KEY_GOAL_REMAINING, stats.goalRemainingAmount.toFloat())
            putFloat(KEY_GOAL_DAILY, stats.goalDailyNeeded.toFloat())
            putFloat(KEY_GOAL_ACTUAL, stats.goalActualDailyYield.toFloat())
            putInt(KEY_GOAL_DAYS, stats.goalDaysRemaining)
            putString(KEY_GOAL_PACE, stats.goalPaceStatus)
            putFloat(KEY_ACTIVE_DAYS, stats.totalActiveDays.toFloat())
            putLong(KEY_UPDATED, stats.updatedAtMillis)
        }
    }

    fun load(context: Context): WidgetStats {
        val prefs = prefs(context)
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
            totalActiveDays = prefs.getFloat(KEY_ACTIVE_DAYS, 0f).toDouble(),
            updatedAtMillis = prefs.getLong(KEY_UPDATED, 0L)
        )
    }
}

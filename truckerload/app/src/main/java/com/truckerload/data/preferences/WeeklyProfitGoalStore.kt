package com.truckerload.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.truckerload.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PREFS_NAME = "truckerload_widget_goals"
private const val KEY_WEEKLY_PROFIT_GOAL = "weekly_profit_goal"
const val DEFAULT_WEEKLY_GROSS_GOAL = 0.0

class WeeklyProfitGoalStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _goalAmount = MutableStateFlow(loadFromPrefs())
    val goalAmount: StateFlow<Double> = _goalAmount.asStateFlow()

    val isConfigured: Boolean get() = prefs.contains(KEY_WEEKLY_PROFIT_GOAL)

    private fun loadFromPrefs(): Double {
        if (!prefs.contains(KEY_WEEKLY_PROFIT_GOAL)) return DEFAULT_WEEKLY_GROSS_GOAL
        return prefs.getFloat(KEY_WEEKLY_PROFIT_GOAL, 0f).toDouble()
    }

    fun getGoal(): Double = _goalAmount.value

    fun save(goal: Double): Result<Unit> {
        if (goal <= 0) {
            return Result.failure(IllegalArgumentException(appContext.getString(R.string.goal_error_positive)))
        }
        prefs.edit()
            .putFloat(KEY_WEEKLY_PROFIT_GOAL, goal.toFloat())
            .commit()
        _goalAmount.value = goal
        return Result.success(Unit)
    }
}

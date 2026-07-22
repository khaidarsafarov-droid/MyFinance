package com.truckerload.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.truckerload.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val LEGACY_PREFS_NAME = "truckerload_widget_goals"
private const val KEY_WEEKLY_PROFIT_GOAL = "weekly_profit_goal"
const val DEFAULT_WEEKLY_GROSS_GOAL = 0.0

class WeeklyProfitGoalStore(
    context: Context,
    userId: String = AuthStore(context).currentUserIdOrNull() ?: AccountIds.LOCAL_DEV,
) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(
            "truckerload_widget_goals_${AccountIds.sanitizeFilePart(userId)}",
            Context.MODE_PRIVATE,
        ).also { scoped ->
            migrateFromLegacyIfEmpty(appContext, scoped)
        }

    private val _goalAmount = MutableStateFlow(loadFromPrefs())
    val goalAmount: StateFlow<Double> = _goalAmount.asStateFlow()

    val isConfigured: Boolean get() = prefs.contains(KEY_WEEKLY_PROFIT_GOAL)

    private fun loadFromPrefs(): Double {
        val raw = prefs.all[KEY_WEEKLY_PROFIT_GOAL] ?: return DEFAULT_WEEKLY_GROSS_GOAL
        val value = when (raw) {
            is String -> raw.toDoubleOrNull()
            is Float -> raw.toDouble()
            is Double -> raw
            is Int -> raw.toDouble()
            is Long -> raw.toDouble()
            else -> null
        } ?: return DEFAULT_WEEKLY_GROSS_GOAL
        if (raw !is String) {
            prefs.edit(commit = true) {
                putString(KEY_WEEKLY_PROFIT_GOAL, value.toString())
            }
        }
        return value
    }

    fun getGoal(): Double = _goalAmount.value

    fun save(goal: Double): Result<Unit> {
        if (goal <= 0) {
            return Result.failure(IllegalArgumentException(appContext.getString(R.string.goal_error_positive)))
        }
        prefs.edit(commit = true) {
            putString(KEY_WEEKLY_PROFIT_GOAL, goal.toString())
        }
        _goalAmount.value = goal
        return Result.success(Unit)
    }

    companion object {
        private fun migrateFromLegacyIfEmpty(context: Context, scoped: SharedPreferences) {
            if (scoped.contains(KEY_WEEKLY_PROFIT_GOAL)) return
            val legacy = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
            if (!legacy.contains(KEY_WEEKLY_PROFIT_GOAL)) return
            val raw = legacy.all[KEY_WEEKLY_PROFIT_GOAL] ?: return
            scoped.edit(commit = true) {
                putString(KEY_WEEKLY_PROFIT_GOAL, raw.toString())
            }
        }
    }
}

package com.truckerload.utils

import android.content.Context

/** Prevents goal fanfare from firing repeatedly for the same week/goal. */
object GoalCelebrationStore {

    private const val PREFS = "truckerload_goal_celebration"

    fun wasCelebrated(context: Context, weekNumber: Int, year: Int, goalAmount: Double): Boolean {
        val key = celebrationKey(weekNumber, year, goalAmount)
        return context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(key, false)
    }

    fun markCelebrated(context: Context, weekNumber: Int, year: Int, goalAmount: Double) {
        val key = celebrationKey(weekNumber, year, goalAmount)
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(key, true)
            .apply()
    }

    private fun celebrationKey(weekNumber: Int, year: Int, goalAmount: Double): String =
        "celebrated_${year}_w${weekNumber}_${goalAmount.toLong()}"
}

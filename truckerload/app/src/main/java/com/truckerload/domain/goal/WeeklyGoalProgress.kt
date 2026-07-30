package com.truckerload.domain.goal

enum class PaceStatus {
    GOAL_MET,
    AHEAD,
    ON_TRACK,
    BEHIND
}

data class WeeklyGoalProgress(
    val targetAmount: Double,
    val currentGross: Double,
    val progressPercent: Float,
    val remainingAmount: Double,
    /** Calendar days Sun…today (for progress ring marker). */
    val daysActiveCalendar: Int,
    val daysRemainingInWeek: Int,
    /** Trip span for the week: first PU → last finish (override or last DEL). */
    val totalActiveDays: Double,
    /** totalGross / totalActiveDays — fact $/day in transit. */
    val actualDailyYield: Double,
    /** (goal − gross) / calendar days remaining. */
    val dailyTargetNeeded: Double,
    val expectedGrossByNow: Double,
    val paceStatus: PaceStatus,
    val weekLabel: String,
    val weekNumber: Int,
    val year: Int,
    val loadsCount: Int = 0,
    val totalMiles: Double = 0.0
)

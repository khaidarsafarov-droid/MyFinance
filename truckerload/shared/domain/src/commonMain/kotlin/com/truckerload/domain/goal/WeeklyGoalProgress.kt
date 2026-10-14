package com.truckerload.domain.goal

import com.truckerload.domain.money.Money

enum class PaceStatus {
    GOAL_MET,
    AHEAD,
    ON_TRACK,
    BEHIND
}

data class WeeklyGoalProgress(
    val targetAmount: Money,
    val currentGross: Money,
    val progressPercent: Float,
    val remainingAmount: Money,
    /** Calendar days Sun…today (for progress ring marker). */
    val daysActiveCalendar: Int,
    val daysRemainingInWeek: Int,
    /** Sum of PU→DEL durations across week loads (fractional days). */
    val totalActiveDays: Double,
    /** totalGross / totalActiveDays — fact $/day in transit. */
    val actualDailyYield: Money,
    /** (goal − gross) / calendar days remaining. */
    val dailyTargetNeeded: Money,
    val expectedGrossByNow: Money,
    val paceStatus: PaceStatus,
    val weekLabel: String,
    val weekNumber: Int,
    val year: Int,
    val loadsCount: Int = 0,
    val totalMiles: Double = 0.0
)

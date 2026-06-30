package com.truckerload.domain.goal

/** Room SQL aggregate for week efficiency (PU→DEL durations). */
data class WeekYieldSnapshot(
    val totalGross: Double,
    val totalActiveDays: Double
) {
    val actualDailyYield: Double
        get() = if (totalActiveDays > 0.0) GoalMoneyMath.roundMoney(totalGross / totalActiveDays) else 0.0
}

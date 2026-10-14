package com.truckerload.domain.goal

import com.truckerload.domain.money.Money

/** Room SQL aggregate for week efficiency (PU→DEL durations). */
data class WeekYieldSnapshot(
    val totalGross: Money,
    val totalActiveDays: Double,
) {
    val actualDailyYield: Money
        get() = if (totalActiveDays > 0.0) {
            totalGross.dividedBy(kotlin.math.ceil(totalActiveDays).toInt().coerceAtLeast(1))
        } else {
            Money.ZERO
        }
}

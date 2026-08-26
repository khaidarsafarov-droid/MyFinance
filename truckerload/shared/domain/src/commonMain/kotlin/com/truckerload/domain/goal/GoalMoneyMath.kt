package com.truckerload.domain.goal

import kotlin.math.round

object GoalMoneyMath {
    fun roundMoney(value: Double): Double = round(value * 100.0) / 100.0

    fun dailyTarget(goal: Double, totalGross: Double, daysRemaining: Int): Double {
        if (goal <= 0.0) return 0.0
        val remaining = (goal - totalGross).coerceAtLeast(0.0)
        if (remaining <= 0.0) return 0.0
        return roundMoney(remaining / daysRemaining.coerceAtLeast(1))
    }

    /** Linear calendar plan marker for progress ring. */
    fun expectedGrossByNow(goal: Double, daysActive: Int): Double {
        if (goal <= 0.0 || daysActive <= 0) return 0.0
        return roundMoney(goal * daysActive.coerceAtMost(7) / 7.0)
    }
}

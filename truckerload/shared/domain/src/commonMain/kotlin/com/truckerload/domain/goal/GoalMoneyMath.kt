package com.truckerload.domain.goal

import com.truckerload.domain.money.Money

object GoalMoneyMath {
    fun dailyTarget(goal: Money, totalGross: Money, daysRemaining: Int): Money {
        if (goal.cents <= 0L) return Money.ZERO
        val remaining = (goal - totalGross).coerceAtLeast(Money.ZERO)
        if (remaining.cents <= 0L) return Money.ZERO
        return remaining.dividedBy(daysRemaining.coerceAtLeast(1))
    }

    /** Linear calendar plan marker for the progress ring. */
    fun expectedGrossByNow(goal: Money, daysActive: Int): Money {
        if (goal.cents <= 0L || daysActive <= 0) return Money.ZERO
        val days = daysActive.coerceAtMost(7)
        val product = goal.cents * days.toLong()
        val negative = product < 0
        val absProduct = kotlin.math.abs(product)
        val cents = (absProduct + 3L) / 7L
        return Money(if (negative) -cents else cents)
    }
}

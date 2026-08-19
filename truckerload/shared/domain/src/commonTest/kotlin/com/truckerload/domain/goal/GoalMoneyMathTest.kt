package com.truckerload.domain.goal

import kotlin.test.Test
import kotlin.test.assertEquals

class GoalMoneyMathTest {
    @Test
    fun `roundMoney keeps two decimal places`() {
        assertEquals(12.34, GoalMoneyMath.roundMoney(12.344))
        assertEquals(12.35, GoalMoneyMath.roundMoney(12.346))
    }

    @Test
    fun `dailyTarget splits remaining goal across remaining days`() {
        assertEquals(100.0, GoalMoneyMath.dailyTarget(goal = 700.0, totalGross = 200.0, daysRemaining = 5))
        assertEquals(0.0, GoalMoneyMath.dailyTarget(goal = 500.0, totalGross = 500.0, daysRemaining = 2))
        assertEquals(0.0, GoalMoneyMath.dailyTarget(goal = 0.0, totalGross = 0.0, daysRemaining = 3))
    }

    @Test
    fun `expectedGrossByNow is linear across a seven day week`() {
        assertEquals(100.0, GoalMoneyMath.expectedGrossByNow(goal = 700.0, daysActive = 1))
        assertEquals(700.0, GoalMoneyMath.expectedGrossByNow(goal = 700.0, daysActive = 7))
    }
}

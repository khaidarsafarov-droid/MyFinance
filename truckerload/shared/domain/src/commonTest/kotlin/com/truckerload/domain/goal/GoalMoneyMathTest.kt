package com.truckerload.domain.goal

import com.truckerload.domain.money.Money
import kotlin.test.Test
import kotlin.test.assertEquals

class GoalMoneyMathTest {
    @Test
    fun dailyTargetSplitsRemainingGoalAcrossRemainingDays() {
        assertEquals(
            Money.parse("100.00"),
            GoalMoneyMath.dailyTarget(
                goal = Money.parse("700.00"),
                totalGross = Money.parse("200.00"),
                daysRemaining = 5,
            ),
        )
        assertEquals(
            Money.ZERO,
            GoalMoneyMath.dailyTarget(
                goal = Money.parse("500.00"),
                totalGross = Money.parse("500.00"),
                daysRemaining = 2,
            ),
        )
        assertEquals(
            Money.ZERO,
            GoalMoneyMath.dailyTarget(goal = Money.ZERO, totalGross = Money.ZERO, daysRemaining = 3),
        )
    }

    @Test
    fun expectedGrossByNowIsLinearAcrossASevenDayWeek() {
        assertEquals(
            Money.parse("100.00"),
            GoalMoneyMath.expectedGrossByNow(goal = Money.parse("700.00"), daysActive = 1),
        )
        assertEquals(
            Money.parse("700.00"),
            GoalMoneyMath.expectedGrossByNow(goal = Money.parse("700.00"), daysActive = 7),
        )
        assertEquals(Money.ZERO, GoalMoneyMath.expectedGrossByNow(goal = Money.parse("700.00"), daysActive = 0))
    }
}

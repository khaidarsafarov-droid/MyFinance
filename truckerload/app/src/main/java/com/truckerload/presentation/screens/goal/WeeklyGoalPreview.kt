package com.truckerload.presentation.screens.goal

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.truckerload.domain.goal.PaceStatus
import com.truckerload.domain.goal.WeeklyDayPlan
import com.truckerload.domain.goal.WeeklyGoalProgress
import com.truckerload.domain.money.Money
import com.truckerload.presentation.theme.TruckerLoadTheme
import java.time.LocalDate

private fun previewProgress(): WeeklyGoalProgress = WeeklyGoalProgress(
    targetAmount = Money.fromDollars(13_000.0),
    currentGross = Money.fromDollars(990.90),
    progressPercent = 7.62f,
    remainingAmount = Money.fromDollars(12_009.10),
    daysActiveCalendar = 1,
    daysRemainingInWeek = 7,
    totalActiveDays = 1.0,
    actualDailyYield = Money.fromDollars(990.90),
    dailyTargetNeeded = Money.fromCents(171_559L),
    expectedGrossByNow = Money.fromDollars(1_857.0),
    paceStatus = PaceStatus.BEHIND,
    weekLabel = "окт 18 – окт 24, 2026",
    weekNumber = 43,
    year = 2026,
)

@Preview(name = "Weekly goal light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun WeeklyGoalLightPreview() {
    TruckerLoadTheme(darkTheme = false, reduceMotion = true) {
        WeeklyGoalContent(
            progress = previewProgress(),
            dayPlan = WeeklyDayPlan.rows(
                weekStart = LocalDate.of(2026, 10, 18),
                today = LocalDate.of(2026, 10, 18),
                grossCentsBySqliteDow = mapOf(0 to 99_090L),
                dailyNeeded = Money.fromCents(171_559L),
            ),
            motionSettled = true,
            isEditingGoal = false,
            goalInput = "",
            goalError = null,
            isSavingGoal = false,
            suggestedGoals = emptyList(),
            onStartEdit = {},
            onGoalInputChange = {},
            onSaveGoal = {},
            onCancelEdit = {},
            onSuggestedGoal = {},
        )
    }
}

@Preview(name = "Weekly goal dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun WeeklyGoalDarkPreview() {
    TruckerLoadTheme(darkTheme = true, reduceMotion = true) {
        WeeklyGoalContent(
            progress = previewProgress(),
            dayPlan = WeeklyDayPlan.rows(
                weekStart = LocalDate.of(2026, 10, 18),
                today = LocalDate.of(2026, 10, 18),
                grossCentsBySqliteDow = mapOf(0 to 99_090L),
                dailyNeeded = Money.fromCents(171_559L),
            ),
            motionSettled = true,
            isEditingGoal = false,
            goalInput = "",
            goalError = null,
            isSavingGoal = false,
            suggestedGoals = emptyList(),
            onStartEdit = {},
            onGoalInputChange = {},
            onSaveGoal = {},
            onCancelEdit = {},
            onSuggestedGoal = {},
        )
    }
}

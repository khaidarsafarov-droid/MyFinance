package com.truckerload.presentation.screens.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.data.local.entities.LoadStatsAgg
import com.truckerload.data.local.entities.WeeklyLoadStatsAgg
import com.truckerload.domain.goal.PaceStatus
import com.truckerload.domain.goal.WeeklyGoalCalculator
import com.truckerload.domain.ux.ActivationChecklistFactory
import com.truckerload.domain.ux.ActivationStep
import com.truckerload.domain.ux.LossAversionKind
import com.truckerload.domain.ux.UxMotivation
import com.truckerload.presentation.components.ActivationChecklistCard
import com.truckerload.presentation.components.HomeWeekHeroCard
import com.truckerload.presentation.components.LossAversionBanner
import com.truckerload.presentation.di.LocalDieselRepository
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.di.LocalUserProfileStore
import com.truckerload.presentation.di.LocalWeeklyProfitGoalStore
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.MoneyFormat
import com.truckerload.presentation.utils.adaptiveHorizontalPadding
import com.truckerload.utils.getCurrentWeekNumberAndYear
import kotlinx.coroutines.flow.map
import java.util.Locale

/**
 * Home motivators: goal-gradient checklist, week hero (ownership),
 * loss-aversion banner, and diesel/gross contrast.
 */
@Composable
fun HomeUxMotivators(
    onAddLoad: () -> Unit,
    onWeeklyGoal: () -> Unit,
    onAddDiesel: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val loadRepository = LocalLoadRepository.current
    val dieselRepository = LocalDieselRepository.current
    val goalStore = LocalWeeklyProfitGoalStore.current
    val userProfileStore = LocalUserProfileStore.current
    val tc = LocalTruckColors.current

    val (week, year) = remember { getCurrentWeekNumberAndYear() }
    val goal by goalStore.goalAmount.collectAsStateWithLifecycle()
    val weekStats by remember(week, year) {
        loadRepository.watchWeeklyLoadStats(week, year)
    }.collectAsStateWithLifecycle(initialValue = WeeklyLoadStatsAgg(0, 0.0, 0.0))
    val totalStats by loadRepository.watchTotalLoadStats()
        .collectAsStateWithLifecycle(initialValue = LoadStatsAgg(0, 0.0, 0.0))
    val weekLoads by remember(week, year) {
        loadRepository.getLoadsByWeek(week, year)
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val dieselWeek by remember(week, year) {
        dieselRepository.getDieselForWeek(week, year).map { list -> list.sumOf { it.totalAmount } }
    }.collectAsStateWithLifecycle(initialValue = 0.0)
    val hasAnyDiesel by dieselRepository.getAllDiesel()
        .map { it.isNotEmpty() }
        .collectAsStateWithLifecycle(initialValue = false)
    val profileOk by userProfileStore.setupComplete.collectAsStateWithLifecycle()

    val checklist = remember(profileOk, totalStats.totalLoads, goal, hasAnyDiesel) {
        ActivationChecklistFactory.build(
            profileComplete = profileOk,
            hasLoad = totalStats.totalLoads > 0,
            hasWeeklyGoal = goal > 0,
            hasDiesel = hasAnyDiesel,
        )
    }

    val progress = remember(goal, weekLoads) {
        WeeklyGoalCalculator.calculateCurrentWeek(
            targetAmount = goal,
            allLoads = weekLoads,
            sqlYield = null,
        )
    }
    val currentGross = progress.currentGross
    val progressPercent = progress.progressPercent
    val rpm = if (weekStats.totalMiles > 0) weekStats.totalRevenue / weekStats.totalMiles else null

    val lossSignal = remember(progress, weekStats.loadCount, goal) {
        UxMotivation.lossAversion(
            goalConfigured = goal > 0,
            targetAmount = goal,
            currentGross = progress.currentGross,
            remainingAmount = progress.remainingAmount,
            daysRemaining = progress.daysRemainingInWeek,
            isBehindPace = progress.paceStatus == PaceStatus.BEHIND,
            loadCountThisWeek = weekStats.loadCount,
        )
    }

    val dieselContrast = remember(currentGross, dieselWeek) {
        UxMotivation.dieselContrast(currentGross, dieselWeek)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (goal > 0 || currentGross > 0) {
            HomeWeekHeroCard(
                gross = currentGross,
                goal = goal.coerceAtLeast(0.0),
                progressPercent = progressPercent,
                rpm = rpm,
                onClick = onWeeklyGoal,
            )
        }

        LossAversionBanner(
            signal = lossSignal,
            onClick = {
                when (lossSignal.kind) {
                    LossAversionKind.NO_LOADS_THIS_WEEK -> onAddLoad()
                    else -> onWeeklyGoal()
                }
            },
        )

        ActivationChecklistCard(
            checklist = checklist,
            onStepClick = { step ->
                when (step) {
                    ActivationStep.ACCOUNT_READY -> Unit
                    ActivationStep.PROFILE_SETUP -> onOpenProfile()
                    ActivationStep.FIRST_LOAD -> onAddLoad()
                    ActivationStep.WEEKLY_GOAL -> onWeeklyGoal()
                    ActivationStep.FIRST_DIESEL -> onAddDiesel()
                }
            },
        )

        dieselContrast?.let { contrast ->
            Text(
                text = stringResource(
                    R.string.ux_contrast_diesel_of_gross,
                    MoneyFormat.formatCurrency(contrast.anchorAmount),
                    MoneyFormat.formatCurrency(contrast.comparedAmount),
                    String.format(Locale.US, "%.0f", contrast.comparedPercentOfAnchor),
                ),
                style = MaterialTheme.typography.labelMedium,
                color = tc.TextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = adaptiveHorizontalPadding(), vertical = 4.dp),
            )
        }
    }
}

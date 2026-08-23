package com.truckerload.presentation.screens.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.data.local.entities.LoadStatsAgg
import com.truckerload.domain.ux.ActivationChecklistFactory
import com.truckerload.domain.ux.ActivationStep
import com.truckerload.presentation.components.ActivationChecklistCard
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.di.LocalUserProfileStore
import com.truckerload.presentation.di.LocalWeeklyProfitGoalStore

/**
 * Newcomer-only next steps. The week hero lives in [PeriodSummarySection] so Home
 * has one place the eye lands first.
 */
@Composable
fun HomeUxMotivators(
    onAddLoad: () -> Unit,
    onWeeklyGoal: () -> Unit,
    onAddDiesel: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val loadRepository = LocalLoadRepository.current
    val goalStore = LocalWeeklyProfitGoalStore.current
    val userProfileStore = LocalUserProfileStore.current

    val goal by goalStore.goalAmount.collectAsStateWithLifecycle()
    val totalStats by loadRepository.watchTotalLoadStats()
        .collectAsStateWithLifecycle(initialValue = LoadStatsAgg(0, 0.0, 0.0))
    val profileOk by userProfileStore.setupComplete.collectAsStateWithLifecycle()
    val hasLoad = totalStats.totalLoads > 0

    // After the first load, hide the checklist — it nags drivers who already haul.
    if (hasLoad) return

    val checklist = remember(profileOk, goal) {
        ActivationChecklistFactory.build(
            profileComplete = profileOk,
            hasLoad = false,
            hasWeeklyGoal = goal > 0,
            hasDiesel = false,
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
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
    }
}

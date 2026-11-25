package com.truckerload.presentation.screens.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.truckerload.domain.ux.ActivationChecklistFactory
import com.truckerload.domain.ux.ActivationStep
import com.truckerload.presentation.components.ActivationChecklistCard

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
    totalLoadCount: Int,
    profileComplete: Boolean,
    weeklyGoal: Double,
) {
    // After the first load, hide the checklist — it nags drivers who already haul.
    if (totalLoadCount > 0) return

    val checklist = remember(profileComplete, weeklyGoal) {
        ActivationChecklistFactory.build(
            profileComplete = profileComplete,
            hasLoad = false,
            hasWeeklyGoal = weeklyGoal > 0,
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

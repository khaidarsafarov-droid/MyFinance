package com.truckerload.domain.ux

/**
 * Goal-gradient activation checklist.
 *
 * Account creation counts as a completed head-start step so progress never starts at 0%.
 */
enum class ActivationStep {
    ACCOUNT_READY,
    PROFILE_SETUP,
    FIRST_LOAD,
    WEEKLY_GOAL,
    FIRST_DIESEL,
}

data class ActivationChecklist(
    val steps: List<Pair<ActivationStep, Boolean>>,
) {
    val completedCount: Int get() = steps.count { it.second }
    val totalCount: Int get() = steps.size
    val allDone: Boolean get() = steps.isNotEmpty() && steps.all { it.second }

    /** Visual progress 0f..1f — never below the head-start fraction when account exists. */
    val progressFraction: Float
        get() = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount.toFloat()

    val incompleteSteps: List<ActivationStep>
        get() = steps.filterNot { it.second }.map { it.first }
}

object ActivationChecklistFactory {
    fun build(
        profileComplete: Boolean,
        hasLoad: Boolean,
        hasWeeklyGoal: Boolean,
        hasDiesel: Boolean,
    ): ActivationChecklist = ActivationChecklist(
        steps = listOf(
            ActivationStep.ACCOUNT_READY to true,
            ActivationStep.PROFILE_SETUP to profileComplete,
            ActivationStep.FIRST_LOAD to hasLoad,
            ActivationStep.WEEKLY_GOAL to hasWeeklyGoal,
            ActivationStep.FIRST_DIESEL to hasDiesel,
        ),
    )
}

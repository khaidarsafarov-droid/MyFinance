package com.truckerload.presentation.screens.auth

/** Where registration back should go. Only step 0 leaves the flow. */
enum class RegistrationBackAction {
    Leave,
    Previous,
}

object RegistrationNavigation {
    fun onBack(stepIndex: Int): RegistrationBackAction =
        if (stepIndex <= 0) RegistrationBackAction.Leave else RegistrationBackAction.Previous

    fun previousStep(stepIndex: Int): Int = if (stepIndex <= 0) 0 else stepIndex - 1
}

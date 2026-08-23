package com.truckerload.presentation.screens.assistant

import com.truckerload.domain.model.Diesel
import com.truckerload.domain.model.Paycheck
import com.truckerload.domain.model.WeekSummary

sealed class AssistantPhase {
    data object Idle : AssistantPhase()
    data object Listening : AssistantPhase()
    data object Processing : AssistantPhase()
    data object Ready : AssistantPhase()
    data object Error : AssistantPhase()
}

sealed class PendingAssistantMutation {
    data class DieselDraft(val diesel: Diesel) : PendingAssistantMutation()
    data class PaycheckDraft(val paycheck: Paycheck) : PendingAssistantMutation()
}

enum class AssistantFailKind {
    NO_API_KEY,
    NETWORK,
}

sealed class AssistantResult {
    data class Confirm(val mutation: PendingAssistantMutation) : AssistantResult()
    data class WeeklyGross(val summary: WeekSummary) : AssistantResult()
    data object Ambiguous : AssistantResult()
    data class Saved(val mutation: PendingAssistantMutation) : AssistantResult()
    data class Failed(val kind: AssistantFailKind) : AssistantResult()
}

data class VoiceAssistantUiState(
    val phase: AssistantPhase = AssistantPhase.Idle,
    val transcript: String = "",
    val result: AssistantResult? = null,
    val errorMessageRes: Int? = null,
    val isSaving: Boolean = false,
    val needsMicPermission: Boolean = false,
)

package com.truckerload.presentation.screens.assistant

import com.truckerload.data.repository.WeekRepository
import com.truckerload.domain.assistant.AssistantToolCall
import com.truckerload.domain.assistant.JournalEntryFactory
import com.truckerload.voice.VoiceAssistantLogger
import javax.inject.Inject

/**
 * Shared diesel / paycheck / weekly-gross mapping used by the on-device voice
 * assistant and Google Assistant App Actions. Mutations are preview-only.
 */
class JournalAssistantPreview @Inject constructor(
    private val weekRepository: WeekRepository,
) {
    suspend fun fromToolCall(
        call: AssistantToolCall,
        nowMillis: Long = System.currentTimeMillis(),
    ): AssistantResult {
        return when (call) {
            is AssistantToolCall.AddDiesel -> {
                val draft = JournalEntryFactory.diesel(
                    call.amount,
                    call.gallons,
                    call.date,
                    nowMillis,
                ) ?: return AssistantResult.Ambiguous
                VoiceAssistantLogger.logOutcome("assistant/add_diesel", "confirm")
                AssistantResult.Confirm(PendingAssistantMutation.DieselDraft(draft))
            }
            is AssistantToolCall.AddPaycheck -> {
                val draft = JournalEntryFactory.paycheck(
                    amount = call.amount,
                    weekNumber = call.weekNumber,
                    year = call.year,
                    nowMillis = nowMillis,
                ) ?: return AssistantResult.Ambiguous
                VoiceAssistantLogger.logOutcome("assistant/add_paycheck", "confirm")
                AssistantResult.Confirm(PendingAssistantMutation.PaycheckDraft(draft))
            }
            is AssistantToolCall.QueryWeeklyGross -> {
                val (weekNumber, year) = JournalEntryFactory.resolveWeek(
                    call.weekNumber,
                    call.year,
                    nowMillis,
                )
                val summary = weekRepository.getWeekSummaryOnce(weekNumber, year)
                VoiceAssistantLogger.logOutcome("assistant/query_weekly_gross", "ready")
                AssistantResult.WeeklyGross(summary)
            }
        }
    }
}

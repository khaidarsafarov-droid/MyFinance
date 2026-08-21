package com.truckerload.presentation.screens.assistant

import com.truckerload.data.assistant.GeminiFunctionCallingClient
import com.truckerload.data.repository.WeekRepository
import com.truckerload.domain.assistant.AssistantFunctionCallParser
import com.truckerload.domain.assistant.AssistantToolCall
import com.truckerload.domain.assistant.JournalEntryFactory
import com.truckerload.utils.getCurrentWeekNumberAndYear
import com.truckerload.voice.VoiceAssistantLogger
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Maps a Gemini function call onto local repositories.
 * Mutations are preview-only until [VoiceAssistantViewModel] confirms.
 */
class GeminiFunctionDispatcher @Inject constructor(
    private val gemini: GeminiFunctionCallingClient,
    private val weekRepository: WeekRepository,
) {
    suspend fun interpret(transcript: String, localeTag: String): AssistantResult {
        val trimmed = transcript.trim()
        if (trimmed.isEmpty()) return AssistantResult.Ambiguous
        if (!gemini.isConfigured()) {
            VoiceAssistantLogger.logOutcome("assistant", "no_api_key")
            return AssistantResult.Failed(AssistantFailKind.NO_API_KEY)
        }
        val now = System.currentTimeMillis()
        val (week, year) = getCurrentWeekNumberAndYear()
        val dateIso = DateTimeFormatter.ISO_LOCAL_DATE.format(
            Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate(),
        )
        val body = gemini.generateFunctionCall(
            userText = trimmed,
            localeTag = localeTag,
            currentDateIso = dateIso,
            currentWeekNumber = week,
            currentWeekYear = year,
        ).getOrElse {
            VoiceAssistantLogger.logOutcome("assistant", "gemini_error")
            return AssistantResult.Failed(AssistantFailKind.NETWORK)
        }
        val call = AssistantFunctionCallParser.parseGenerateContentBody(body)
        if (call == null) {
            VoiceAssistantLogger.logOutcome("assistant", "ambiguous")
            return AssistantResult.Ambiguous
        }
        return dispatch(call, now)
    }

    internal suspend fun dispatch(call: AssistantToolCall, nowMillis: Long): AssistantResult {
        return when (call) {
            is AssistantToolCall.AddDiesel -> {
                val draft = JournalEntryFactory.diesel(call.amount, call.gallons, call.date, nowMillis)
                    ?: return AssistantResult.Ambiguous
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

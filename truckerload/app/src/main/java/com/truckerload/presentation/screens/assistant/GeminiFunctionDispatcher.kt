package com.truckerload.presentation.screens.assistant

import com.truckerload.data.assistant.GeminiFunctionCallingClient
import com.truckerload.domain.assistant.AssistantFunctionCallParser
import com.truckerload.domain.assistant.AssistantToolCall
import com.truckerload.utils.getCurrentWeekNumberAndYear
import com.truckerload.voice.VoiceAssistantLogger
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Sends a transcript to Gemini, then maps the tool call through [JournalAssistantPreview].
 * Mutations stay preview-only until the user confirms.
 */
class GeminiFunctionDispatcher @Inject constructor(
    private val gemini: GeminiFunctionCallingClient,
    private val preview: JournalAssistantPreview,
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
        return preview.fromToolCall(call, now)
    }

    internal suspend fun dispatch(call: AssistantToolCall, nowMillis: Long): AssistantResult =
        preview.fromToolCall(call, nowMillis)
}

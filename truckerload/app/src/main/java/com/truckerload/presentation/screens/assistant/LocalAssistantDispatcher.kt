package com.truckerload.presentation.screens.assistant

import com.truckerload.domain.assistant.AssistantToolCall
import com.truckerload.voice.LocalSpokenAssistantParser
import com.truckerload.voice.VoiceAssistantLogger
import javax.inject.Inject

/**
 * Interprets a speech transcript on-device (no cloud LLM), then maps the tool
 * call through [JournalAssistantPreview]. Mutations stay preview-only until confirm.
 */
class LocalAssistantDispatcher @Inject constructor(
    private val preview: JournalAssistantPreview,
) {
    suspend fun interpret(
        transcript: String,
        @Suppress("UNUSED_PARAMETER") localeTag: String
    ): AssistantResult {
        val trimmed = transcript.trim()
        if (trimmed.isEmpty()) return AssistantResult.Ambiguous
        val call = LocalSpokenAssistantParser.parse(trimmed)
        if (call == null) {
            VoiceAssistantLogger.logOutcome("assistant", "ambiguous")
            return AssistantResult.Ambiguous
        }
        return preview.fromToolCall(call, System.currentTimeMillis())
    }

    internal suspend fun dispatch(call: AssistantToolCall, nowMillis: Long): AssistantResult =
        preview.fromToolCall(call, nowMillis)
}

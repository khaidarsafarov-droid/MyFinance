package com.truckerload.data.assistant

/**
 * Sends a spoken transcript to Gemini with the assistant tool schema.
 * Implementations must not log [userText] or the model payload.
 */
interface GeminiFunctionCallingClient {
    fun isConfigured(): Boolean

    /**
     * @return raw generateContent JSON body on success.
     */
    suspend fun generateFunctionCall(
        userText: String,
        localeTag: String,
        currentDateIso: String,
        currentWeekNumber: Int,
        currentWeekYear: Int,
    ): Result<String>
}

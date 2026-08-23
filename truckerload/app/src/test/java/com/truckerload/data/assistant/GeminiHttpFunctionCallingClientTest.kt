package com.truckerload.data.assistant

import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiHttpFunctionCallingClientTest {
    @Test
    fun requestBodyDeclaresAssistantTools() {
        val client = GeminiHttpFunctionCallingClient(apiKey = "test-key", model = "gemini-2.0-flash")
        val body = client.requestBody(
            userText = "placeholder",
            localeTag = "ru",
            currentDateIso = "2026-08-21",
            currentWeekNumber = 34,
            currentWeekYear = 2026,
        )
        assertTrue(body.contains("add_diesel"))
        assertTrue(body.contains("add_paycheck"))
        assertTrue(body.contains("query_weekly_gross"))
        assertTrue(body.contains("functionCallingConfig"))
        assertTrue(body.contains("2026-08-21"))
        assertTrue(!body.contains("test-key"))
        assertTrue(!body.contains("x-goog-api-key"))
    }
}

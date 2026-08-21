package com.truckerload.domain.assistant

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantFunctionCallParserTest {

    @Test
    fun parseAddDieselWithGallonsAndDate() {
        val body = generateContent(
            name = "add_diesel",
            args = """{"amount":80.5,"gallons":20,"date":"2026-08-21"}""",
        )
        val call = AssistantFunctionCallParser.parseGenerateContentBody(body)
            as AssistantToolCall.AddDiesel
        assertEquals(80.5, call.amount, 0.0)
        assertEquals(20.0, call.gallons ?: 0.0, 0.0)
        assertEquals("2026-08-21", call.date)
    }

    @Test
    fun parseAddPaycheckAmountOnly() {
        val body = generateContent(
            name = "add_paycheck",
            args = """{"amount":2500}""",
        )
        val call = AssistantFunctionCallParser.parseGenerateContentBody(body)
            as AssistantToolCall.AddPaycheck
        assertEquals(2500.0, call.amount, 0.0)
        assertNull(call.weekNumber)
        assertNull(call.year)
    }

    @Test
    fun parseWeeklyGrossCurrentWeek() {
        val body = generateContent(name = "query_weekly_gross", args = "{}")
        val call = AssistantFunctionCallParser.parseGenerateContentBody(body)
            as AssistantToolCall.QueryWeeklyGross
        assertNull(call.weekNumber)
        assertNull(call.year)
    }

    @Test
    fun missingAmountIsAmbiguous() {
        val body = generateContent(name = "add_diesel", args = """{"gallons":10}""")
        assertNull(AssistantFunctionCallParser.parseGenerateContentBody(body))
    }

    @Test
    fun textOnlyResponseIsAmbiguous() {
        val body = """
            {"candidates":[{"content":{"parts":[{"text":"I am not sure"}]}}]}
        """.trimIndent()
        assertNull(AssistantFunctionCallParser.parseGenerateContentBody(body))
    }

    @Test
    fun unknownFunctionIsAmbiguous() {
        val body = generateContent(name = "delete_everything", args = """{"amount":1}""")
        assertNull(AssistantFunctionCallParser.parseGenerateContentBody(body))
    }

    @Test
    fun zeroAmountIsAmbiguous() {
        val call = AssistantFunctionCallParser.parseFunctionCall(
            JSONObject("""{"name":"add_paycheck","args":{"amount":0}}"""),
        )
        assertNull(call)
    }

    @Test
    fun toolsSchemaIncludesRequiredFunctions() {
        val json = AssistantFunctionDeclarations.functionDeclarations().toString()
        assertTrue(json.contains(AssistantToolNames.ADD_DIESEL))
        assertTrue(json.contains(AssistantToolNames.ADD_PAYCHECK))
        assertTrue(json.contains(AssistantToolNames.QUERY_WEEKLY_GROSS))
    }

    private fun generateContent(name: String, args: String): String = """
        {"candidates":[{"content":{"parts":[{"functionCall":{"name":"$name","args":$args}}]}}]}
    """.trimIndent()
}

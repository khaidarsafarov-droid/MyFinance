package com.truckerload.domain.advisor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeterministicAdvisorServiceTest {

    private val advisor = DeterministicAdvisorService()

    @Test
    fun chat_emptyMessage_returnsFallbackGuidance() {
        val result = advisor.chat(history = emptyList(), userMessage = "", appContext = null)
        assertTrue(result.isSuccess)
        val reply = result.getOrThrow()
        assertTrue(reply.contains("rules", ignoreCase = true) || reply.contains("loads", ignoreCase = true))
    }

    @Test
    fun chat_withLoadContext_summarizesRecentLoads() {
        val ctx = """
            Recent:
            - SWF2 → TEB3 · $2500 · 850 mi
            - ATL2 → DEN3 · $1800 · 600 mi
        """.trimIndent()

        val result = advisor.chat(
            history = emptyList(),
            userMessage = "what are my loads?",
            appContext = ctx,
        )

        assertTrue(result.isSuccess)
        val reply = result.getOrThrow()
        assertTrue(reply.contains("Recent loads"))
        assertTrue(reply.contains("SWF2"))
    }

    @Test
    fun chat_rpmKeyword_mentionsStatistics() {
        val reply = advisor.chat(emptyList(), "my rpm this week", null).getOrThrow()
        assertTrue(reply.contains("RPM", ignoreCase = true))
        assertTrue(reply.contains("Statistics", ignoreCase = true) || reply.contains("mile", ignoreCase = true))
    }

    @Test
    fun generateInsight_emptyMetrics_stillReturnsActions() {
        val insight = advisor.generateInsight(
            rpm = 0.0,
            profit = 0.0,
            fuelCost = 0.0,
            miles = 0.0,
            topStates = emptyList(),
            anomalies = "",
        ).getOrThrow()

        assertFalse(insight.insight.isBlank())
        assertTrue(insight.actions.isNotEmpty())
    }
}

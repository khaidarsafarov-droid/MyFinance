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
        assertTrue(reply.contains("правилах") || reply.contains("лоуды", ignoreCase = true))
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
            userMessage = "какие у меня лоуды?",
            appContext = ctx,
        )

        assertTrue(result.isSuccess)
        val reply = result.getOrThrow()
        assertTrue(reply.contains("Последние лоуды"))
        assertTrue(reply.contains("SWF2"))
    }

    @Test
    fun chat_rpmKeyword_mentionsStatistics() {
        val reply = advisor.chat(emptyList(), "мой rpm за неделю", null).getOrThrow()
        assertTrue(reply.contains("RPM", ignoreCase = true))
        assertTrue(reply.contains("Статистик") || reply.contains("милю"))
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

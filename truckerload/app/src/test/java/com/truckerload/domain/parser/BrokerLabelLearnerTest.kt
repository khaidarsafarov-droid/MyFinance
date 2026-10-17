package com.truckerload.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BrokerLabelLearnerTest {

    @Test
    fun labelBefore_readsTheWordsInFrontOfTheToken() {
        val text = """
            Carrier Rate Confirmation
            Load Number: 8844221
            Total Rate: $1,850.00
            Pickup: Dallas, TX
        """.trimIndent()
        assertEquals("Load Number", BrokerLabelLearner.labelBefore(text, "8844221"))
        assertEquals("Total Rate", BrokerLabelLearner.labelBefore(text, "1850.00"))
        assertNull(BrokerLabelLearner.labelBefore(text, "nope"))
    }

    @Test
    fun learnedTemplate_isUsedOnlyWhenNoBuiltInBrokerMatches() {
        val learned = BrokerTemplate(
            id = "user-load-number",
            matchAny = emptyList(),
            matchAll = listOf("Load Number", "Carrier Rate Confirmation"),
            tripId = listOf(FieldRule(type = "after", label = "Load Number", section = null)),
            rate = listOf(FieldRule(type = "lineAmount", label = "Total Rate", section = null)),
            linehaul = emptyList(),
            fuel = emptyList(),
            accessorials = emptyList(),
        )
        BrokerRateTemplateCatalog.userTemplates = listOf(learned)
        try {
            val text = """
                Carrier Rate Confirmation
                Load Number: 8844221
                Total Rate: $1,850.00
                Pickup
                Address: 100 Main St Dallas, TX 75001
                Delivery
                Address: 200 Oak Ave Denver, CO 80202
            """.trimIndent()
            val load = BrokerRateTemplateEngine.parseOne(text, nowMillis = 1_700_000_000_000L)
            assertEquals("8844221", load?.tripId)
            assertEquals(1850.0, load?.totalRate?.cents?.div(100.0) ?: 0.0, 0.001)
        } finally {
            BrokerRateTemplateCatalog.userTemplates = emptyList()
        }
    }
}

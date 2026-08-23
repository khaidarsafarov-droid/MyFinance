package com.truckerload.voice

import com.truckerload.domain.assistant.AssistantToolCall
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalSpokenAssistantParserTest {

    @Test
    fun parsesDieselWithDollarsAndGallonsRu() {
        val call = LocalSpokenAssistantParser.parse("Добавь дизель 80 долларов 20 галлонов")
                as AssistantToolCall.AddDiesel
        assertEquals(80.0, call.amount, 0.0)
        assertEquals(20.0, call.gallons ?: 0.0, 0.0)
    }

    @Test
    fun parsesDieselEnglish() {
        val call = LocalSpokenAssistantParser.parse("add diesel 80 dollars 20 gallons")
                as AssistantToolCall.AddDiesel
        assertEquals(80.0, call.amount, 0.0)
        assertEquals(20.0, call.gallons ?: 0.0, 0.0)
    }

    @Test
    fun dieselWithoutAmountIsNull() {
        assertNull(LocalSpokenAssistantParser.parse("Привет добавь меня дизель пожалуйста"))
    }

    @Test
    fun parsesPaycheckAmount() {
        val call = LocalSpokenAssistantParser.parse("добавь зарплату 2500")
                as AssistantToolCall.AddPaycheck
        assertEquals(2500.0, call.amount, 0.0)
        assertNull(call.weekNumber)
    }

    @Test
    fun parsesWeeklyGrossRu() {
        val call = LocalSpokenAssistantParser.parse("Какой был гросс на этой неделе")
        assertTrue(call is AssistantToolCall.QueryWeeklyGross)
    }

    @Test
    fun unknownPhraseIsNull() {
        assertNull(LocalSpokenAssistantParser.parse("ну сделай что-нибудь"))
    }
}

package com.truckerload.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramChatGateTest {

    @Test
    fun extractFromMessage_supportsStartPairAndBareCode() {
        assertEquals("123456", TelegramPairingCodes.extractFromMessage("/start 123456"))
        assertEquals("654321", TelegramPairingCodes.extractFromMessage("/pair 654321"))
        assertEquals("000111", TelegramPairingCodes.extractFromMessage("/start@MyBot 000111"))
        assertEquals("999888", TelegramPairingCodes.extractFromMessage("999888"))
        assertNull(TelegramPairingCodes.extractFromMessage("/start"))
        assertNull(TelegramPairingCodes.extractFromMessage("hello"))
    }

    @Test
    fun decide_allowsPairedChat() {
        val d = TelegramChatGate.decide(
            allowedChatId = 42L,
            incomingChatId = 42L,
            text = "/stats",
            expectedPairCode = null,
            pairCodeExpiresAtMillis = null,
            nowMillis = 1_000L,
        )
        assertEquals(TelegramAuthDecision.Allow, d)
    }

    @Test
    fun decide_rejectsOtherChatWhenPaired() {
        val d = TelegramChatGate.decide(
            allowedChatId = 42L,
            incomingChatId = 99L,
            text = "/start 123456",
            expectedPairCode = "123456",
            pairCodeExpiresAtMillis = 10_000L,
            nowMillis = 1_000L,
        )
        assertEquals(TelegramAuthDecision.RejectUnauthorized, d)
    }

    @Test
    fun decide_rejectsPlainStartWithoutCode() {
        val d = TelegramChatGate.decide(
            allowedChatId = null,
            incomingChatId = 7L,
            text = "/start",
            expectedPairCode = "123456",
            pairCodeExpiresAtMillis = 10_000L,
            nowMillis = 1_000L,
        )
        assertEquals(TelegramAuthDecision.RejectNeedPairCode, d)
    }

    @Test
    fun decide_pairsWithValidCode() {
        val d = TelegramChatGate.decide(
            allowedChatId = null,
            incomingChatId = 7L,
            text = "/start 123456",
            expectedPairCode = "123456",
            pairCodeExpiresAtMillis = 10_000L,
            nowMillis = 1_000L,
        )
        assertEquals(TelegramAuthDecision.PairAndAllow(7L), d)
    }

    @Test
    fun decide_rejectsExpiredCode() {
        val d = TelegramChatGate.decide(
            allowedChatId = null,
            incomingChatId = 7L,
            text = "/start 123456",
            expectedPairCode = "123456",
            pairCodeExpiresAtMillis = 500L,
            nowMillis = 1_000L,
        )
        assertEquals(TelegramAuthDecision.RejectBadPairCode, d)
    }

    @Test
    fun generate_isSixDigits() {
        val code = TelegramPairingCodes.generate()
        assertEquals(6, code.length)
        assertTrue(code.all { it.isDigit() })
    }
}

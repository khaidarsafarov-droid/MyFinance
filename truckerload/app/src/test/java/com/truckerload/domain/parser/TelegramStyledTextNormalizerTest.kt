package com.truckerload.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramStyledTextNormalizerTest {

    @Test
    fun normalize_mathematicalBoldSansSerif_toAscii() {
        val styled = "𝗧𝗿𝗶𝗽 𝗜𝗗:  T-116KYL6KW"
        assertEquals("Trip ID:  T-116KYL6KW", TelegramStyledTextNormalizer.normalize(styled))
    }

    @Test
    fun normalize_leavesPlainAsciiUntouched() {
        val plain = "Trip ID: T-116KYL6KW\nPU# 115S1Q2P1"
        assertEquals(plain, TelegramStyledTextNormalizer.normalize(plain))
    }
}

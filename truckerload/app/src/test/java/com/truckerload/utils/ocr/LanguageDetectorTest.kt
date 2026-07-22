package com.truckerload.utils.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageDetectorTest {

    @Test
    fun detect_returnsUnknownForBlankInput() {
        assertEquals("unknown", LanguageDetector.detect(""))
        assertEquals("unknown", LanguageDetector.detect("   "))
    }

    @Test
    fun detect_identifiesRussianText() {
        val sample = "Груз доставлен в Чикаго, штат Иллинойс"

        assertEquals("ru", LanguageDetector.detect(sample))
        assertTrue(LanguageDetector.isRussianText(sample))
    }

    @Test
    fun detect_identifiesLatinText() {
        val sample = "Trip ID: T-116KYL6KW Total Rate 2500.00"

        assertEquals("en", LanguageDetector.detect(sample))
        assertTrue(LanguageDetector.isLatinText(sample))
    }

    @Test
    fun detect_returnsUnknownWhenNoLetters() {
        assertEquals("unknown", LanguageDetector.detect("12345 !!!"))
    }

    @Test
    fun isLatinText_falseForRussianSample() {
        val sample = "Груз доставлен в Чикаго"

        assertFalse(LanguageDetector.isLatinText(sample))
    }
}

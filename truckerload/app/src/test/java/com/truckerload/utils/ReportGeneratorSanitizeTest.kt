package com.truckerload.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ReportGeneratorSanitizeTest {

    @Test
    fun sanitizeFileLabel_stripsTripIdSpecialChars() {
        assertEquals(
            "T-116KYL6KW_week",
            ReportGeneratorService.sanitizeFileLabel("T-116KYL6KW/week"),
        )
        assertEquals(
            "Trip_50",
            ReportGeneratorService.sanitizeFileLabel("Trip#50%"),
        )
        assertEquals("report", ReportGeneratorService.sanitizeFileLabel("@@@"))
    }

    @Test
    fun sanitizeFileLabel_keepsLettersDigitsSpacesHyphen() {
        val label = ReportGeneratorService.sanitizeFileLabel("Week 30 - 2026")
        assertEquals("Week 30 - 2026", label)
        assertFalse(label.contains("/"))
        assertFalse(label.contains("%"))
    }
}

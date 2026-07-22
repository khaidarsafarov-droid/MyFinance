package com.truckerload.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class NormalizeTripIdTest {

    @Test
    fun trimsAndUppercases() {
        assertEquals("T-116KYL6KW", normalizeTripId("  t-116kyl6kw  "))
    }

    @Test
    fun blankStaysBlankAfterTrim() {
        assertEquals("", normalizeTripId("   "))
    }

    @Test
    fun caseInsensitiveDuplicatesMatch() {
        val a = normalizeTripId("abc-1")
        val b = normalizeTripId("ABC-1")
        assertEquals(a, b)
    }
}

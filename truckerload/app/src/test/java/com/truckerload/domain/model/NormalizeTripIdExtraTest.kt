package com.truckerload.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class NormalizeTripIdExtraTest {
    @Test fun trimsAndUppercases() = assertEquals("T-ABC", normalizeTripId(" t-abc "))
    @Test fun blankStaysBlank() = assertEquals("", normalizeTripId("   "))
}

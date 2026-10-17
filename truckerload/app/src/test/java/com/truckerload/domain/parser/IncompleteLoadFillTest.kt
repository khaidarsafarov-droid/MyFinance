package com.truckerload.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IncompleteLoadFillTest {

    @Test
    fun shortCityFillsTheMissingPickup() {
        val draft = LoadDraftFields(tripId = "T-1", rate = "2500", miles = "850")
        val filled = IncompleteLoadFill.apply(draft, "Garner, NC")
        assertEquals("Garner, NC", filled?.pointA)
        assertEquals("T-1 · $2500 · 850 mi", IncompleteLoadFill.foundSummary(draft))
    }

    @Test
    fun longPasteIsANewDocument() {
        val draft = LoadDraftFields(rate = "2500")
        val pasted = "Total Rate: 2500\n".repeat(20)
        assertNull(IncompleteLoadFill.apply(draft, pasted))
    }

    @Test
    fun typedRateFillsAMissingRate() {
        val draft = LoadDraftFields(pointA = "Garner, NC", pointB = "Dallas, TX")
        val filled = IncompleteLoadFill.apply(draft, "$2,500.00")
        assertEquals("2500", filled?.rate)
    }
}

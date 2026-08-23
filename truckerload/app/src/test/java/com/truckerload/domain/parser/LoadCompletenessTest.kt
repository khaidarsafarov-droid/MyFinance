package com.truckerload.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadCompletenessTest {

    @Test
    fun fullDraftNeedsNoConfirmation() {
        val result = LoadCompletenessChecker.of(
            rate = 2500.0,
            miles = 850.0,
            points = listOf("Garner, NC", "Dallas, TX"),
            tripId = "T-1",
            date = "2026-08-20",
        )
        assertTrue(result.isComplete)
        assertTrue(result.canSave)
        assertFalse(result.needsConfirmation)
    }

    @Test
    fun missingRateAndRouteBlockSave() {
        val result = LoadCompletenessChecker.of(
            rate = 0.0,
            miles = 0.0,
            points = listOf("", ""),
            tripId = "",
            date = "",
        )
        assertFalse(result.canSave)
        assertFalse(result.needsConfirmation)
        assertTrue(result.missingRequired.contains(LoadField.RATE))
        assertTrue(result.missingRequired.contains(LoadField.PICKUP))
    }

    @Test
    fun softGapsOnlyAskForConfirmation() {
        val result = LoadCompletenessChecker.of(
            rate = 2500.0,
            miles = 0.0,
            points = listOf("Garner, NC", ""),
            tripId = "",
            date = "",
        )
        assertTrue(result.canSave)
        assertTrue(result.needsConfirmation)
        assertEquals(
            listOf(LoadField.DELIVERY, LoadField.MILES, LoadField.DATE, LoadField.TRIP_ID),
            result.missingOptional,
        )
    }

    @Test
    fun draftFromPartialOcrReportsGaps() {
        val draft = MessageParseService().extractLoadFields("Total Rate: 1000\nPickup: Reno, NV")
        val result = LoadCompletenessChecker.of(draft)
        assertTrue(result.canSave)
        assertTrue(result.missingOptional.contains(LoadField.DELIVERY))
        assertTrue(result.missingOptional.contains(LoadField.MILES))
    }

    @Test
    fun serviceCompletenessFlagsUnreadableText() {
        val result = MessageParseService().completenessOf("random note with no load data")
        assertFalse(result.canSave)
        assertTrue(result.missingRequired.contains(LoadField.RATE))
    }
}

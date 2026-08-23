package com.truckerload.utils

import com.truckerload.domain.model.Load
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadDateRepairEnsureDateTest {

    @Test
    fun ensureDate_fillsBlankDateFromReferenceMillis() {
        val load = blankDateLoad(parsedAt = 1L)
        val ref = 1_724_025_600_000L // 2024-08-19 approx UTC noon-ish

        val ensured = LoadDateRepair.ensureDate(load, referenceMillis = ref)

        assertEquals(formatIsoDate(ref), ensured.date)
        assertTrue(ensured.weekNumber > 0)
        assertTrue(ensured.year > 0)
    }

    @Test
    fun ensureDate_usesParsedAtWhenReferenceMissing() {
        val parsedAt = 1_724_025_600_000L
        val load = blankDateLoad(parsedAt = parsedAt)

        val ensured = LoadDateRepair.ensureDate(load)

        assertEquals(formatIsoDate(parsedAt), ensured.date)
    }

    @Test
    fun ensureDate_keepsExistingIsoDate() {
        val load = blankDateLoad(parsedAt = 1_724_025_600_000L).copy(date = "2025-03-10")

        val ensured = LoadDateRepair.ensureDate(load, referenceMillis = 1_724_025_600_000L)

        assertEquals("2025-03-10", ensured.date)
    }

    private fun blankDateLoad(parsedAt: Long) = Load(
        id = "L1",
        tripId = "T-BLANK",
        date = "",
        totalRate = 1000.0,
        totalMiles = 100.0,
        pointA = "A",
        pointB = "B",
        puCount = 1,
        delCount = 1,
        weekNumber = 0,
        year = 0,
        rawMessage = "",
        parsedAt = parsedAt,
        updatedAt = parsedAt,
    )
}

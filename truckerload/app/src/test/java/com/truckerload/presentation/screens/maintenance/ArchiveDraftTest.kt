package com.truckerload.presentation.screens.maintenance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveDraftTest {

    @Test
    fun validation_requiresNamedPricedLines() {
        assertEquals("empty_lines", ArchiveDraft().validationError())
        assertEquals(
            "empty_description",
            ArchiveDraft(lines = listOf(ArchiveLineDraft(amount = "10"))).validationError(),
        )
        assertEquals(
            "invalid_amount",
            ArchiveDraft(lines = listOf(ArchiveLineDraft(description = "Oil", amount = "x"))).validationError(),
        )
        assertNull(
            ArchiveDraft(
                lines = listOf(
                    ArchiveLineDraft(description = "Oil", amount = "80.50"),
                    ArchiveLineDraft(),
                ),
            ).validationError(),
        )
    }

    @Test
    fun toEntries_savesEachLineWithSharedShopAndTotal() {
        val draft = ArchiveDraft(
            serviceName = "Love's",
            serviceDate = "2026-08-25",
            lines = listOf(
                ArchiveLineDraft("VALVE ADJUSTMENT", "500.00"),
                ArchiveLineDraft("OIL FILTER", "43.12"),
                ArchiveLineDraft(),
            ),
        )
        assertEquals(543.12, draft.lineTotal(), 0.01)
        val entries = draft.toEntries(createdAt = 42L)
        assertEquals(2, entries.size)
        assertTrue(entries.all { it.serviceName == "Love's" })
        assertTrue(entries.all { it.serviceDate == "2026-08-25" })
        assertTrue(entries.all { it.createdAt == 42L })
        assertEquals("VALVE ADJUSTMENT", entries[0].description)
        assertEquals(500.0, entries[0].amount, 0.01)
        assertEquals(43.12, entries[1].amount, 0.01)
    }
}

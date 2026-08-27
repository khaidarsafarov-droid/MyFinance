package com.truckerload.domain.maintenance

import com.truckerload.domain.model.MaintenanceArchiveEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class MaintenanceVisitsTest {

    @Test
    fun group_sameCreatedAtAndDate_isOneVisit() {
        val created = 1_700_000_000_000L
        val entries = listOf(
            line(1, "VALVE ADJUSTMENT", 500.0, created),
            line(2, "OIL FILTER", 43.12, created),
        )
        val visits = MaintenanceVisits.group(entries)
        assertEquals(1, visits.size)
        assertEquals(543.12, visits[0].total, 0.01)
        assertEquals(listOf(1L, 2L), visits[0].ids)
        assertEquals("Shop", visits[0].shopName)
    }

    @Test
    fun group_differentCreatedAt_areSeparateVisits() {
        val a = line(1, "Oil", 80.0, createdAt = 10L)
        val b = line(2, "Tires", 400.0, createdAt = 20L, date = "2026-08-24")
        val visits = MaintenanceVisits.group(listOf(a, b))
        assertEquals(2, visits.size)
        assertEquals("Oil", visits[0].lines.single().description)
        assertEquals("Tires", visits[1].lines.single().description)
    }

    private fun line(
        id: Long,
        description: String,
        amount: Double,
        createdAt: Long,
        date: String = "2026-08-25",
    ) = MaintenanceArchiveEntry(
        id = id,
        serviceName = "Shop",
        serviceDate = date,
        description = description,
        amount = amount,
        createdAt = createdAt,
    )
}

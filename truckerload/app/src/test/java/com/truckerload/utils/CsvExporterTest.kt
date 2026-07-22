package com.truckerload.utils

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {

    @Test
    fun `header matches export contract`() {
        assertEquals("date,route,income,miles,rpm", CsvExporter.CSV_HEADER)
    }

    @Test
    fun `formatLoadCsvRow escapes route and computes rpm`() {
        val load = Load(
            id = "1",
            tripId = "T-1",
            date = "2026-06-10",
            totalRate = 2460.0,
            totalMiles = 1000.0,
            pointA = "Hopewell Junction, NY",
            pointB = "Garner, NC",
            puCount = 1,
            delCount = 1,
            weekNumber = 24,
            year = 2026,
            rawMessage = "",
            parsedAt = 1L,
            updatedAt = 1L,
            stops = listOf(
                Stop(
                    id = 1,
                    loadId = "1",
                    stopNumber = 1,
                    type = StopType.PU,
                    puNumber = null,
                    note = null,
                    scheduledTime = "",
                    timezone = "UTC",
                    facilityCode = null,
                    fullAddress = "Hopewell Junction, NY",
                    city = "Hopewell Junction",
                    state = "NY",
                    zip = "",
                ),
                Stop(
                    id = 2,
                    loadId = "1",
                    stopNumber = 2,
                    type = StopType.DEL,
                    puNumber = null,
                    note = null,
                    scheduledTime = "",
                    timezone = "UTC",
                    facilityCode = null,
                    fullAddress = "Garner, NC",
                    city = "Garner",
                    state = "NC",
                    zip = "",
                ),
            ),
        )

        val row = CsvExporter.formatLoadCsvRow(load)
        assertTrue(row.startsWith("2026-06-10,"))
        assertTrue(row.contains("Hopewell Junction, NY"))
        assertTrue(row.contains("Garner, NC"))
        assertTrue(row.contains("2460.00"))
        assertTrue(row.contains("1000"))
        assertTrue(row.endsWith("2.46"))
    }

    @Test
    fun formatLoadCsvRow_escapesQuotesAndCommasInRoute() {
        val load = Load(
            id = "2",
            tripId = "T-2",
            date = "2026-06-11",
            totalRate = 500.0,
            totalMiles = 100.0,
            pointA = "Hopewell \"Junction\", NY",
            pointB = "Garner, NC",
            puCount = 1,
            delCount = 1,
            weekNumber = 24,
            year = 2026,
            rawMessage = "",
            parsedAt = 1L,
            updatedAt = 1L,
            stops = listOf(
                Stop(
                    id = 1,
                    loadId = "2",
                    stopNumber = 1,
                    type = StopType.PU,
                    puNumber = null,
                    note = null,
                    scheduledTime = "",
                    timezone = "UTC",
                    facilityCode = null,
                    fullAddress = "Hopewell \"Junction\", NY",
                    city = "Hopewell \"Junction\"",
                    state = "NY",
                    zip = "",
                ),
                Stop(
                    id = 2,
                    loadId = "2",
                    stopNumber = 2,
                    type = StopType.DEL,
                    puNumber = null,
                    note = null,
                    scheduledTime = "",
                    timezone = "UTC",
                    facilityCode = null,
                    fullAddress = "Garner, NC",
                    city = "Garner",
                    state = "NC",
                    zip = "",
                ),
            ),
        )
        val row = CsvExporter.formatLoadCsvRow(load)
        // Quotes in city are doubled; commas keep the field quoted.
        assertTrue(row.contains("\"\""))
        assertTrue(row.startsWith("2026-06-11,\""))
        assertTrue(row.contains("500.00"))
        assertTrue(row.contains("Hopewell \"\"Junction\"\""))
    }
}

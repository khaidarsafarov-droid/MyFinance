package com.truckerload.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanDocumentFinderTest {

    @Test
    fun infer_loadId_isLoad() {
        assertEquals(
            ScanDocumentCategory.LOAD,
            ScanDocumentFinder.infer("load-1", "scan.pdf", "diesel gallons"),
        )
    }

    @Test
    fun infer_paycheckKeywords() {
        assertEquals(
            ScanDocumentCategory.PAYCHECK,
            ScanDocumentFinder.infer(null, "week.pdf", "Settlement paycheck for week 34"),
        )
    }

    @Test
    fun infer_dieselKeywords() {
        assertEquals(
            ScanDocumentCategory.DIESEL,
            ScanDocumentFinder.infer(null, "fuel.pdf", "Diesel 500 gallons"),
        )
    }

    @Test
    fun infer_truckKeywords() {
        assertEquals(
            ScanDocumentCategory.TRUCK,
            ScanDocumentFinder.infer(null, "cdl.pdf", "Insurance and CDL registration"),
        )
    }

    @Test
    fun infer_unknown_isOther() {
        assertEquals(
            ScanDocumentCategory.OTHER,
            ScanDocumentFinder.infer(null, "note.pdf", "hello world"),
        )
    }

    @Test
    fun matches_filterAndSearch() {
        assertTrue(
            ScanDocumentFinder.matches(
                storedCategory = "PAYCHECK",
                fileName = "week.pdf",
                ocrText = "",
                tripId = "T-1",
                routeLabel = "—",
                dateLabel = "25.08.2026",
                filter = ScanDocumentCategory.PAYCHECK,
                query = "зарплата",
            ),
        )
        assertFalse(
            ScanDocumentFinder.matches(
                storedCategory = "PAYCHECK",
                fileName = "week.pdf",
                ocrText = "",
                tripId = "T-1",
                routeLabel = "—",
                dateLabel = "25.08.2026",
                filter = ScanDocumentCategory.DIESEL,
                query = "",
            ),
        )
        assertTrue(
            ScanDocumentFinder.matches(
                storedCategory = "LOAD",
                fileName = "bol.pdf",
                ocrText = "Trip ID: T-116KYL6KW",
                tripId = "T-116KYL6KW",
                routeLabel = "Garner, NC",
                dateLabel = "25.08.2026",
                filter = null,
                query = "116KYL",
            ),
        )
    }

    @Test
    fun fromStored_unknownFallsBackToOther() {
        assertEquals(ScanDocumentCategory.OTHER, ScanDocumentCategory.fromStored(null))
        assertEquals(ScanDocumentCategory.OTHER, ScanDocumentCategory.fromStored("NOPE"))
        assertEquals(ScanDocumentCategory.LOAD, ScanDocumentCategory.fromStored("load"))
    }
}

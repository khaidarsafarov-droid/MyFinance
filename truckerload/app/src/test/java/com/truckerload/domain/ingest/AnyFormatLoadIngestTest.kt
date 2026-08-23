package com.truckerload.domain.ingest

import com.truckerload.domain.parser.LoadField
import com.truckerload.domain.parser.MessageParseService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * One load, expressed in every format a driver can send, must land on the same
 * numbers whether it arrives through the bot or the add-load screen.
 */
class AnyFormatLoadIngestTest {

    @Test
    fun jsonPayloadSavesAsLoad() = assertSameLoad(
        """{"trip_id":"T-77001","rate":2100.50,"miles":700,
           "pickup":"Garner, NC","delivery":"Dallas, TX","date":"2026-08-20"}""",
        fileName = "load.json",
    )

    @Test
    fun plainTextSavesAsLoad() = assertSameLoad(
        """
        Trip ID: T-77001
        Total Rate: 2100.50
        Total Loaded Miles: 700 mi
        Pu-address: Garner, NC
        Del-address: Dallas, TX
        """.trimIndent(),
        fileName = "load.txt",
    )

    @Test
    fun csvExportSavesAsLoad() {
        val text = DocumentBytesDecoder.decode(
            """
            Load #,Rate,Miles,Origin,Destination
            T-77001,2100.50,700,"Garner, NC","Dallas, TX"
            """.trimIndent().toByteArray(),
            fileName = "loads.csv",
            mimeType = "text/csv",
        )
        assertNotNull(text)
        val load = MessageParseService()
            .parseLoadsFromInboundText(text!!, REF, "loads.csv")
            .getOrThrow()
            .firstOrNull()
        assertNotNull(load)
        assertEquals(2100.50, load!!.totalRate, 0.01)
        assertTrue(load.pointA.contains("Garner"))
    }

    @Test
    fun htmlEmailSavesAsLoad() {
        val html = """
            <html><body>
            <p>Trip ID: T-77001</p><p>Total Rate: 2100.50</p>
            <p>Total Loaded Miles: 700 mi</p>
            <p>Pu-address: Garner, NC</p><p>Del-address: Dallas, TX</p>
            </body></html>
        """.trimIndent()
        val text = DocumentBytesDecoder.decode(html.toByteArray(), "rate.html", "text/html")
        assertNotNull(text)
        assertSameLoad(text!!, fileName = "rate.html")
    }

    @Test
    fun ocrTextWithStackedLabelsSavesAsLoad() = assertSameLoad(
        """
        Trip ID
        T-77001
        Total Rate
        ${'$'}2,100.50
        Total Loaded Miles
        700 mi
        Pickup
        Garner, NC
        Delivery
        Dallas, TX
        """.trimIndent(),
        fileName = "scan.jpg",
    )

    @Test
    fun brokerRateConfirmationSavesAsLoad() {
        val load = MessageParseService().parseLoadsFromInboundText(
            """
            Rate Confirmation IEL PO#: 77001
            Estimated Rate (To Truck): ${'$'}2,100.50 Total: ${'$'}2,100.50
            Miles: 700
            Pick Ups
            Shed:ACME Address: 100 Main St Garner, NC 27529
            Deliveries
            Shed:BETA Address: 200 Oak Ave Dallas, TX 75201
            """.trimIndent(),
            REF,
            "carrier-rate-confirmation.pdf",
        ).getOrThrow().firstOrNull()
        assertNotNull(load)
        assertEquals(2100.50, load!!.totalRate, 0.01)
        assertEquals(700.0, load.totalMiles, 0.01)
        assertTrue(load.pointA.contains("Garner"))
        assertTrue(load.pointB.contains("Dallas"))
    }

    @Test
    fun partialJsonIsHeldForConfirmationInsteadOfSaving() {
        val service = MessageParseService()
        val partial = """{"rate":2100.50,"pickup":"Garner, NC"}"""
        val gaps = service.completenessOf(partial, REF, "partial.json")

        assertTrue("rate + one address is enough to store", gaps.canSave)
        assertTrue("driver must confirm the gaps", gaps.needsConfirmation)
        assertTrue(gaps.missingOptional.contains(LoadField.DELIVERY))
        assertTrue(gaps.missingOptional.contains(LoadField.MILES))
    }

    @Test
    fun payloadWithoutRateBlocksSaveUntilDriverFillsIt() {
        val gaps = MessageParseService().completenessOf(
            """{"pickup":"Garner, NC","delivery":"Dallas, TX"}""",
            REF,
            "no-rate.json",
        )
        assertTrue(gaps.missingRequired.contains(LoadField.RATE))
        assertTrue(!gaps.canSave)
    }

    @Test
    fun botAutoSavesJsonLoadWithoutAskingForReceiptType() {
        val decision = InboundDocumentResolver.resolve(
            text = """{"trip_id":"T-77001","total_rate":2100.50,"total_miles":700,
                       "pu_address":"Garner, NC","del_address":"Dallas, TX"}""",
            fileName = "load.json",
            referenceMillis = REF,
        )
        assertTrue(decision.autoSaveLoads)
        assertEquals(1, decision.loads.size)
        assertEquals(2100.50, decision.loads.first().totalRate, 0.01)
    }

    @Test
    fun botReportsMissingFieldsForIncompleteRateConfirmation() {
        val decision = InboundDocumentResolver.resolve(
            text = """
                Rate Confirmation
                Load Information
                Pick Ups
                Address: 100 Main St Garner, NC 27529
            """.trimIndent(),
            fileName = "rate-confirmation.pdf",
            referenceMillis = REF,
        )
        assertTrue(decision.loads.isEmpty())
        val gaps = decision.incompleteLoad
        assertNotNull("bot should explain the gap, not ask diesel/DEF/paycheck", gaps)
        assertTrue(gaps!!.missingRequired.contains(LoadField.RATE))
    }

    private fun assertSameLoad(text: String, fileName: String) {
        val load = MessageParseService()
            .parseLoadsFromInboundText(text, REF, fileName)
            .getOrThrow()
            .firstOrNull()
        assertNotNull("no load parsed from $fileName", load)
        assertEquals("T-77001", load!!.tripId)
        assertEquals(2100.50, load.totalRate, 0.01)
        assertEquals(700.0, load.totalMiles, 0.01)
        assertTrue(load.pointA.contains("Garner"))
        assertTrue(load.pointB.contains("Dallas"))
    }

    private companion object {
        const val REF = 1_776_441_600_000L
    }
}

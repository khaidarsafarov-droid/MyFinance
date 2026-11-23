package com.truckerload.domain.parser

/**
 * Made-up rate confirmation. The words "Integrity Express" and "IEL PO#" stay
 * because the broker template matches on those labels. The load itself
 * (carrier, phone, PO, streets, amount) is fictional.
 */
object FakeRateConfirmation {
    const val FILE_NAME = "4401901-Carrier-Rate-Confirmation.pdf"
    const val TRIP = "4401901"
    const val RATE = 1864.20
    const val MILES = 512.25
    const val PICKUP_CITY = "CEDAR RAPIDS"
    const val DELIVERY_CITY = "BOISE"
    const val HEADER_CITY = "Springfield"

    val TEXT = """
        Rate Confirmation IEL PO#: $TRIP
        Integrity Express Logistics
        PO Box 100 - Springfield, OH 45501
        Phone: (555) 010-0199 Ext: 0199
        Load Information
        IEL PO#: $TRIP Trailer: Reefer
        Team
        Size: 53 ft Temp: 38
        Pick Up: 07/25/25 Delivery: 07/26/25 Weight: 42000 CONTINUOUS RUN
        Miles: $MILES
        Carrier: Sample Haul LLC
        Estimated Rate (To Truck): ${'$'}1,864.20 Unloading: ${'$'}0.00 Total: ${'$'}1,864.20
        Rate Description Quantity Total
        ${'$'}1,864.20 Flat 1.00 ${'$'}1,864.20
        Pick Ups
        Shed:DEMO DOCK Address: 400 DEMO DOCK RD CEDAR RAPIDS, IA 52404
        Phone: Date: 07/25/25 Time: 2PM
        RECOMMENDED NEARBY LOCATIONS: RIVERFRONT OASIS
        TRAVEL PLAZA / LOVES TRAVEL STOP #800 - 1900 SAMPLE ROAD
        Deliveries
        Shed:SAMPLE YARD Address: 900 SAMPLE LN BOISE, ID 83702
        Date: 07/26/25 Time: 9PM
        xvi. A fee of ${'$'}7.50 per pallet will be charged on loads that the carrier is responsible
        to supply pallets for exchange and they do not.
        xii. ${'$'}35 will be deducted from your invoice for each comcheck issued for a fuel or cash advance.
    """.trimIndent()
}

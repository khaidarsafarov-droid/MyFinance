package com.truckerload.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cases ported from the external JUnit reference (money filtering, OCR repair, load-board formats, edge cases),
 * run against the real [MessageParseService] / [RateConTextExtractor] API.
 */
class ReferenceCasesTest {

    private val ref = 1_795_000_000_000L

    private fun parse(raw: String) = MessageParseService().extractLoadFields(raw, ref)

    private fun rate(raw: String): Double? = parse(raw).parsedRate()

    // ---- money: not-pay amounts, REVISED, reversed layouts ---------------------------------------

    @Test
    fun `cargo value and seal fine are not the rate (Arrive)`() {
        val raw = """
            Load                      Carrier
            Arrive Order 6050001      Carrier Acme Trucking LLC
            Cargo Value ${'$'}100,000.00   Attn Dispatch
            Total Miles 1191 Miles

            CHARGES
            Line Haul                 ${'$'}2,350.00
            Fuel Surcharge              ${'$'}250.00
            Total Rate:               ${'$'}2,600.00

            Special Instructions:
            ${'$'}350 fine for not applying seal before leaving shipper.
            Auto Liability ${'$'}1,000,000 required.
        """.trimIndent()
        assertEquals(2600.0, rate(raw)!!, 0.001)
    }

    @Test
    fun `amount before TOTAL and POD fine (Sunteck)`() {
        val raw = """
            SUNTECK TRANSPORT CO., LLC
            CARRIER RATE CONFIRMATION
            LOAD NUMBER 13650001 MUST APPEAR ON YOUR INVOICE!
            CHARGES
            ${'$'}1,650.00 FLAT RATE
            ${'$'}150.00 GUARANTEED SERVICE ON TIME DELIVERY
            ${'$'}1,800.00 TOTAL

            DELIVERY NOTES
            MUST SEND POD WITHIN 2 HOURS.
            FAILURE TO COMPLY WILL RESULT IN ${'$'}150 FINE.
        """.trimIndent()
        assertEquals(1800.0, rate(raw)!!, 0.001)
    }

    @Test
    fun `detention layover and admin fee are not the rate (Allen Lund)`() {
        val raw = """
            ALLEN LUND RATE CONFIRMATION
            Truck Rate        FLT   ${'$'}2,100.00   1   ${'$'}2,100.00
            Total: 2100

            REQUIREMENTS:
            Authorized detention will be paid at ${'$'}25.00/hour, not to exceed ${'$'}150.00 per day.
            ${'$'}150.00 layover will be paid. Unexpected calls after hours for lumper comchecks may result in a ${'$'}25 administrative fee.
            a. Fee for an advance is 2% of the due carrier amount or ${'$'}25.00, whichever amount is higher.
        """.trimIndent()
        assertEquals(2100.0, rate(raw)!!, 0.001)
    }

    @Test
    fun `revised total beats previous rate`() {
        val raw = """
            REVISED CARRIER RATE CONFIRMATION
            CHARGES
            Previous Rate: ${'$'}1,800.00
            Detention Added: ${'$'}250.00
            Revised Total: ${'$'}2,050.00
        """.trimIndent()
        assertEquals(2050.0, rate(raw)!!, 0.001)
    }

    @Test
    fun `carrier pay total is the sum, not a single accessorial (RXO)`() {
        val raw = """
            CARRIER RATE CONFIRMATION
            PAYMENT
            Carrier Pay Breakdown
            LNH | Line Haul | Flat                        ${'$'}1,500.00
            AT | Auto Tracking | Flat                       ${'$'}150.00
            MS | Miscellaneous | Flat | on time del         ${'$'}150.00
            Total Carrier Pay                             ${'$'}1,800.00
        """.trimIndent()
        assertEquals(1800.0, rate(raw)!!, 0.001)
    }

    @Test
    fun `per-unit fuel rate is not the total (Hub Group)`() {
        val raw = """
            Hub Group, Inc. Dispatch Sheet and Terms
            Overview of Charges / Load # 142750001
            Service          R.P.U.      Unit   Amount      Cur
            Rate             ${'$'}1,523.04   1      ${'$'}1,523.04   USD
            FUEL SURCHARGE   ${'$'}.32        553    ${'$'}176.96     USD
            Grand Total:                        ${'$'}1,700.00   USD
        """.trimIndent()
        assertEquals(1700.0, rate(raw)!!, 0.001)
    }

    // ---- OCR ------------------------------------------------------------------------------------

    @Test
    fun `dollar sign read as a leading 5 is repaired against the itemised sum`() {
        val raw = """
            CARRIER RATE CONFIRMATION
            CHARGES
            Line Haul: ${'$'}1,600.00
            Fuel Surcharge: ${'$'}250.00
            Total Pay: 51,850.00
        """.trimIndent()
        assertEquals(1850.0, rate(raw)!!, 0.001)
    }

    @Test
    fun `letters read as digits and dollar signs inside the total are repaired`() {
        val raw = """
            CARRIER RATE CONFIRMATION
            CHARGES
            Line Haul      S1,500.00
            Fuel Surcharge S250.00
            Total Pay:     S1,75O.OO
        """.trimIndent()
        assertEquals(1750.0, rate(raw)!!, 0.001)
    }

    @Test
    fun `OCR noise sample total with letter O`() {
        assertEquals(3100.0, rate("Rate Confirmation\nTotal Pay: ${'$'}3,1O0.00\nPU: Allentown, PA 18106\nDEL: Phoenix, AZ 85043")!!, 0.001)
    }

    // ---- load boards -------------------------------------------------------------------------------

    @Test
    fun `amazon relay trip confirmation with hub codes`() {
        val raw = """
            Amazon Relay - Trip Confirmation
            Tour ID: T-114H9K2M8
            Load ID / VRID: 114H9K2M8
            Equipment: Power Only (53' Trailer Provided)
            Distance: 492 mi
            Estimated Payout: ${'$'}1,385.40
              Linehaul: ${'$'}1,180.00
              Fuel Surcharge: ${'$'}175.40
              Tolls: ${'$'}30.00

            Stop 1 - Pick Up (Drop & Hook)
            BFI4 - Kent, WA 98032
            Arrival: Nov 29, 2026 22:15 PST

            Stop 2 - Stop Off (Drop & Hook)
            PDX9 - Troutdale, OR 97060
            Arrival: Nov 30, 2026 03:30 PST

            Stop 3 - Drop Off (Drop & Hook)
            GEG1 - Spokane, WA 99224
            Arrival: Nov 30, 2026 11:00 PST
        """.trimIndent()
        val d = parse(raw)
        assertEquals(1385.40, d.parsedRate()!!, 0.001)
        assertEquals(492.0, d.parsedMiles(), 0.001)
        assertTrue(d.tripId, d.tripId.contains("114H9K2M8"))
        assertTrue(d.pointA, d.pointA.contains("Kent"))
        assertTrue(d.pointB, d.pointB.contains("Spokane"))
    }

    @Test
    fun `DAT load details split trip miles from deadhead and rate from rpm`() {
        val raw = """
            DAT One Load Details
            Origin: Atlanta, GA (DH-O: 14 mi)
            Destination: Columbus, OH
            Trip: 568 mi
            Pickup: 11/30 08:00 - 14:00
            Delivery: 12/01 09:00
            Equip: V | 53 ft | 42,000 lbs | Full
            Rate: ${'$'}1,550 (${'$'}2.73/mi)
            Broker: Horizon Freight Brokerage | MC# 884210 | CS: 96 | DTP: 27
            Ref #: 9941205
            Comments: No touch freight, macropoint required, FCFS 8-2.
        """.trimIndent()
        val d = parse(raw)
        assertEquals(1550.0, d.parsedRate()!!, 0.001)
        assertEquals(568.0, d.parsedMiles(), 0.001)
        assertTrue(d.pointA, d.pointA.contains("Atlanta"))
        assertTrue(d.pointB, d.pointB.contains("Columbus"))
    }

    @Test
    fun `truckstop book it now rate wins over market estimate`() {
        val raw = """
            Truckstop Load Posting
            Pickup: Laredo, TX 78045 | 11/30/2026 (08:00 - 15:00)
            Delivery: Memphis, TN 38118 | 12/02/2026 (07:00 Appt)
            Miles: 992 mi | Weight: 41,500 lbs | Length: 53'
            Equipment: VAN (Full)
            Book It Now Rate: ${'$'}2,450.00 (${'$'}2.47/mi)
            Market Est: ${'$'}2,210.00
            Company: Apex Logistics LLC (MC 771029)
            Credit Score: A (95) | DTP: 29 days | Factorable: Yes
            Reference #: TS-448210
        """.trimIndent()
        val d = parse(raw)
        assertEquals(2450.0, d.parsedRate()!!, 0.001)
        assertEquals(992.0, d.parsedMiles(), 0.001)
        assertTrue(d.pointA, d.pointA.contains("Laredo"))
        assertTrue(d.pointB, d.pointB.contains("Memphis"))
    }

    // ---- edge cases ------------------------------------------------------------------------------------

    @Test
    fun `several deliveries under one consignees header keep the first pickup and the last delivery`() {
        val raw = """
            Load # 550012
            SHIPPER
            Tyson Foods, 2200 Don Tyson Pkwy, Springdale, AR 72762 (11/29 06:00)

            CONSIGNEES / DELIVERIES
            1) Walmart DC #6011 - 1000 Main St, Bentonville, AR 72712 (11/29 08:00)
            2) Target RDC #589 - 4500 Hwy 71, Rogers, AR 72756 (11/29 11:30)
            3) Kroger #44 - 220 Oak Rd, Fayetteville, AR 72701 (11/29 14:00)

            Total Pay: ${'$'}950.00
        """.trimIndent()
        val d = parse(raw)
        assertEquals(950.0, d.parsedRate()!!, 0.001)
        assertTrue(d.pointA, d.pointA.contains("Springdale"))
        assertTrue(d.pointB, d.pointB.contains("Fayetteville"))
    }

    @Test
    fun `delivery block printed before pickup keeps the roles`() {
        val raw = """
            Load # 778899
            CONSIGNEE / DELIVERY:
            Miami Produce Market, Miami, FL 33142 - 11/30/2026 Before 11:00 AM

            SHIPPER / PICKUP:
            Savannah Port Terminal, Savannah, GA 31408 - 11/28/2026 08:00-15:00

            Total Rate: ${'$'}1,300.00
        """.trimIndent()
        val d = parse(raw)
        assertTrue(d.pointA, d.pointA.contains("Savannah"))
        assertTrue(d.pointB, d.pointB.contains("Miami"))
        assertEquals(1300.0, d.parsedRate()!!, 0.001)
    }

    @Test
    fun `city and state without zip, IN OR ME are not states in prose`() {
        val raw = """
            Driver arrived in time, call me or text dispatch.
            PU: Gary IN 11/29 08:00
            DEL: St. Louis MO 11/30 10:00
            Rate: ${'$'}850 all in
        """.trimIndent()
        val d = parse(raw)
        assertEquals(850.0, d.parsedRate()!!, 0.001)
        assertTrue(d.pointA, d.pointA.contains("Gary"))
        assertTrue(d.pointB, d.pointB.contains("Louis"))
    }

    // ---- keyword after the number ------------------------------------------------------------------------

    @Test
    fun `keyword after the number is also a rate label`() {
        assertEquals(1300.0, rate("Savannah GA to Miami FL\n1300 баксов флэт, 480 mi")!!, 0.001)
        assertEquals(780.0, rate("El Paso TX -> Albuquerque NM\n${'$'}780 rate (265 mi)")!!, 0.001)
    }

    // ---- multilingual dates (reference day: Saturday 2026-11-28) -------------------------------------------

    private val today = java.time.LocalDate.of(2026, 11, 28)

    private fun date(text: String, from: java.time.LocalDate = today) = MultilingualDate.extract(text, from)

    @Test
    fun `relative words and weekdays in four languages`() {
        val cases = mapOf(
            "PU today FCFS" to "2026-11-28",
            "Picks tomorrow 10am-2pm" to "2026-11-29",
            "Drops Wed before 2:00 PM" to "2026-12-02",
            "Загрузка сегодня до 16:00" to "2026-11-28",
            "Хороший груз на завтра" to "2026-11-29",
            "Сдача послезавтра утром" to "2026-11-30",
            "Выгрузка понедельник до 11am" to "2026-11-30",
            "Загрузка суббота 8-3" to "2026-11-28",
            "Carga lista para hoy" to "2026-11-28",
            "Descarga mañana 08:00" to "2026-11-29",
            "Entrega viernes 14:00" to "2026-12-04",
            "Oladi bugun 14:00" to "2026-11-28",
            "Yaxshi yuk ertaga ertalab" to "2026-11-29",
            "Topshiradi indinga 09:00" to "2026-11-30",
            "Yuklash dushanba 08:00" to "2026-11-30",
        )
        cases.forEach { (text, iso) -> assertEquals(text, java.time.LocalDate.parse(iso), date(text)) }
    }

    @Test
    fun `slash order is decided for the whole message`() {
        assertEquals(java.time.LocalDate.of(2026, 11, 30), date("DEL 11/30 0700"))
        assertEquals(java.time.LocalDate.of(2026, 12, 1), date("PU 12/01"))
        // 11/30 in the same message forces month-first, so 12/01 is December 1st, not January 12th.
        val chat = "Recoge: Ontario CA 11/30 0700\nEntrega 2: Tucson AZ 12/01 antes de las 10:00 AM"
        assertEquals(java.time.LocalDate.of(2026, 12, 1), MultilingualDate.extract("Tucson AZ 12/01", today, chat))
        // 29/11 forces day-first: 01/12 is December 1st.
        val ru = "Берем: Fresno CA 29/11 в 19:00\nСдаем: Denver CO 01/12 06:00"
        assertEquals(java.time.LocalDate.of(2026, 11, 29), MultilingualDate.extract("Fresno CA 29/11", today, ru))
        assertEquals(java.time.LocalDate.of(2026, 12, 1), MultilingualDate.extract("Denver CO 01/12", today, ru))
        assertEquals(java.time.LocalDate.of(2026, 11, 30), date("Tushirish 30/11 soat 07:00"))
    }

    @Test
    fun `dots are day-month and the year rolls over`() {
        assertEquals(java.time.LocalDate.of(2026, 12, 2), date("Выгрузка 02.12 в 07:00"))
        assertEquals(java.time.LocalDate.of(2026, 12, 1), date("PU: 01.12 08:00-12:00"))
        assertEquals(java.time.LocalDate.of(2027, 1, 2), date("DEL 01/02 08:00", java.time.LocalDate.of(2026, 12, 29)))
    }

    @Test
    fun `24-7 and amounts are not dates`() {
        assertEquals(null, date("Receiving 24/7"))
        assertEquals(null, date("Rate \$1.50 per mile, 42,000 lbs"))
    }

    @Test
    fun `chat draft carries the pickup date`() {
        val msg = "Carga #309184 - 2 paradas\nRecoge: Ontario CA 11/30 0700\nEntrega 1: Phoenix AZ 11/30 16:00\n" +
            "Entrega 2: Tucson AZ 12/01 antes de las 10:00 AM\nTarifa \$1,600 todo incluido (485 mi). Reefer 36F. BOL# 77219A"
        val d = MessageParseService().extractLoadFields(msg, 1_795_000_000_000L)
        assertEquals("2026-11-30", d.date)
        assertEquals("309184", d.tripId)
        assertEquals(1600.0, d.parsedRate()!!, 0.001)
        assertTrue(d.pointA, d.pointA.contains("Ontario"))
        assertTrue(d.pointB, d.pointB.contains("Tucson"))
    }
}

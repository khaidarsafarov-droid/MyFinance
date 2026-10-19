package com.truckerload.presentation.components

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import com.truckerload.domain.money.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoadCardModelTest {

    @Test
    fun mapsDisplayedValuesAndRpmTier() {
        val load = sample(rate = 2_500.0, miles = 1_000.0, deadhead = 0.0)
        val card = load.toCardModel(minProfitRpm = 2.0, targetProfitRpm = 3.0)

        assertEquals("load-1", card.id)
        assertEquals("T-100", card.tripId)
        assertEquals("2026-07-21", card.date)
        assertEquals("2026-07-22", card.headerDate)
        assertEquals(Money.fromDollars(2_500.0), card.totalRate)
        assertEquals(Money.fromDollars(500.0), card.pace)
        assertEquals(1_000.0, card.drivenMiles, 0.001)
        assertEquals(2.5, card.rpm!!, 0.001)
        assertEquals(RpmTier.Ok, card.rpmTier)
        assertEquals("Austin, TX", card.originCity)
        assertEquals("Dallas, TX", card.destCity)
        assertEquals("100 Main St", card.originStreet)
        assertEquals("200 Elm St", card.destStreet)
        assertEquals(2, card.stopCount)
        assertEquals(2, card.durationDays)
        assertEquals(LoadSourceTag.RELAY, card.source)
        assertEquals("Austin, TX → Dallas, TX", card.route)
        assertEquals(true, card.isDispute)
        assertEquals(false, card.hadDispute)
        assertEquals(true, card.isActiveDispute)
    }

    @Test
    fun rpmTierFollowsThresholdsAndZeroMiles() {
        assertEquals(RpmTier.Low, sample(rate = 1_000.0, miles = 1_000.0).toCardModel(2.0, 3.0).rpmTier)
        assertEquals(RpmTier.Good, sample(rate = 4_000.0, miles = 1_000.0).toCardModel(2.0, 3.0).rpmTier)
        val emptyMiles = sample(rate = 100.0, miles = 0.0).toCardModel(2.0, 3.0)
        assertNull(emptyMiles.rpm)
        assertNull(emptyMiles.rpmTier)
    }

    private fun sample(rate: Double, miles: Double, deadhead: Double = 0.0) = Load(
        id = "load-1",
        tripId = "T-100",
        date = "2026-07-21",
        totalRate = Money.fromDollars(rate),
        totalMiles = miles,
        pointA = "Austin, TX",
        pointB = "Dallas, TX",
        puCount = 1,
        delCount = 1,
        weekNumber = 30,
        year = 2026,
        rawMessage = "Amazon Relay\nTrip ID: T-100\nTotal Rate: $rate",
        parsedAt = 10L,
        updatedAt = 11L,
        route = "Austin, TX → Dallas, TX",
        firstPuCityState = "Austin, TX",
        lastDelCityState = "Dallas, TX",
        durationDays = 1.2,
        pace = Money.fromDollars(500.0),
        stopCount = 2,
        isDispute = true,
        actualFinishDate = "2026-07-22",
        deadheadMiles = deadhead,
        stops = listOf(
            Stop(
                0, "load-1", 1, StopType.PU, null, null,
                "07/21 08:00", "CDT", null,
                "100 Main St, Austin, TX", "Austin", "TX", "",
            ),
            Stop(
                0, "load-1", 2, StopType.DEL, null, null,
                "07/22 18:00", "CDT", null,
                "200 Elm St, Dallas, TX", "Dallas", "TX", "",
            ),
        ),
    )
}

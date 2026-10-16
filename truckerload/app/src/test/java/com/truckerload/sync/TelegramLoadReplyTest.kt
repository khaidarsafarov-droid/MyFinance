package com.truckerload.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class TelegramLoadReplyTest {

    @Test
    fun detail_namesRateMilesAndCities() {
        assertEquals(
            "$2,500.00 · 850 mi\nGarner, NC → Dallas, TX",
            TelegramLoadReply.detail(
                rateCents = 250_000,
                miles = 850.0,
                origin = "Garner, NC",
                destination = "Dallas, TX",
            ),
        )
    }

    @Test
    fun detail_keepsAPartialRoute() {
        assertEquals(
            "$1,200.50\nPhoenix, AZ",
            TelegramLoadReply.detail(
                rateCents = 120_050,
                miles = 0.0,
                origin = "Phoenix, AZ",
                destination = "  ",
            ),
        )
    }
}

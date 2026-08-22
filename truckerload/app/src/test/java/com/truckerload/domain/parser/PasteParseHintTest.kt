package com.truckerload.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class PasteParseHintTest {

    @Test
    fun blankText_needsRateAndAddress() {
        assertEquals(PasteParseGap.MISSING_BOTH, PasteParseHint.of("hello from dispatch"))
    }

    @Test
    fun rateWithoutAddress_asksForPuOrDel() {
        assertEquals(
            PasteParseGap.MISSING_ADDRESS,
            PasteParseHint.of("Trip ID: T-1\nTotal Rate: 2500.00"),
        )
    }

    @Test
    fun addressWithoutRate_asksForTotalRate() {
        assertEquals(
            PasteParseGap.MISSING_RATE,
            PasteParseHint.of("Pu-address: SWF2, Garner, NC"),
        )
    }

    @Test
    fun completeLookingRelay_isIncompleteIfParserStillFailed() {
        assertEquals(
            PasteParseGap.INCOMPLETE,
            PasteParseHint.of(
                "Trip ID: T-1\nTotal Rate: 2500.00\nPu-address: SWF2, Garner, NC",
            ),
        )
    }
}

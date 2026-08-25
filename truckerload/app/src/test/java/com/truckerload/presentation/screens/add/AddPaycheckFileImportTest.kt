package com.truckerload.presentation.screens.add

import com.truckerload.domain.parser.PaycheckTextParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AddPaycheckFileImportTest {

    @Test
    fun formatAmountText_stripsTrailingZeros() {
        assertEquals("2750", AddPaycheckViewModel.formatAmountText(2750.0))
        assertEquals("2750.25", AddPaycheckViewModel.formatAmountText(2750.25))
    }

    @Test
    fun paycheckParser_fillsNetTotalFromSettlementText() {
        val text = """
            Driver: Alex Driver
            Week Start: 07/14/2026
            Week End: 07/20/2026
            Gross Pay: $3,400.00
            Net Pay: $2,750.25
        """.trimIndent()
        val parsed = PaycheckTextParser.parse(text)
        assertNotNull(parsed)
        assertEquals(2750.25, parsed!!.netAmount, 0.01)
        assertEquals("2026-07-14", parsed.weekStartDate)
        assertEquals(
            "2750.25",
            AddPaycheckViewModel.formatAmountText(parsed.netAmount),
        )
    }
}

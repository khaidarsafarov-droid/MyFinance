package com.truckerload.presentation.screens.diesel

import com.truckerload.domain.model.Diesel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DieselFillCardMetaFormatterTest {

    private fun diesel(
        gallons: Double? = 500.0,
        pricePerGallon: Double? = 5.20,
        discountPricePerGallon: Double? = 4.60,
    ) = Diesel(
        id = 1,
        weekNumber = 35,
        year = 2026,
        weekLabel = "W35",
        weekStartDate = "2026-08-23",
        weekEndDate = "2026-08-29",
        totalAmount = 2300.0,
        gallons = gallons,
        pricePerGallon = pricePerGallon,
        discountPricePerGallon = discountPricePerGallon,
        location = "Pilot",
        rawExtractedText = "",
        sourceFileName = null,
        addedAt = 0L,
    )

    @Test
    fun format_showsListAndDiscountPricePerGallon() {
        val line = DieselFillCardMetaFormatter.format(
            diesel = diesel(),
            withDiscountFormat = "%1\$s · \$%2\$.2f/gal → \$%3\$.2f/gal",
            listOnlyFormat = "%1\$s · \$%2\$.2f/gal",
            savedSuffixFormat = " · −%1\$s",
            formatSavedAmount = { String.format(java.util.Locale.US, "%.2f", it) },
        )
        assertTrue(line.contains("500.00 gal"))
        assertTrue(line.contains("5.20"))
        assertTrue(line.contains("4.60"))
        assertTrue(line.contains("→"))
        assertTrue(line.contains("300.00"))
    }

    @Test
    fun format_withoutDiscount_showsListPriceOnly() {
        val line = DieselFillCardMetaFormatter.format(
            diesel = diesel(discountPricePerGallon = null),
            withDiscountFormat = "%1\$s · \$%2\$.2f/gal → \$%3\$.2f/gal",
            listOnlyFormat = "%1\$s · \$%2\$.2f/gal",
            savedSuffixFormat = " · −%1\$s",
            formatSavedAmount = { it.toString() },
        )
        assertEquals("500.00 gal · \$5.20/gal", line)
    }

    @Test
    fun format_gallonsOnly_whenNoPrices() {
        val line = DieselFillCardMetaFormatter.format(
            diesel = diesel(pricePerGallon = null, discountPricePerGallon = null),
            withDiscountFormat = "",
            listOnlyFormat = "",
            savedSuffixFormat = "",
            formatSavedAmount = { it.toString() },
        )
        assertEquals("500.00 gal", line)
    }
}

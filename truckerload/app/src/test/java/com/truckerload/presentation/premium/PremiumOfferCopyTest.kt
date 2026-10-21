package com.truckerload.presentation.premium

import com.truckerload.R
import com.truckerload.domain.premium.PremiumOfferPrice
import com.truckerload.domain.premium.PremiumPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PremiumOfferCopyTest {

    @Test
    fun price_usesPlayTextOrTheUsCatalogConstant() {
        assertEquals("$2.99", PremiumPolicy.usMonthlyPriceLabel())
        assertEquals("$2.99", PremiumOfferPrice.display(null))
        assertEquals("$2.99", PremiumOfferPrice.display("  "))
        assertEquals("2,99 €", PremiumOfferPrice.display(" 2,99 € "))
    }

    @Test
    fun englishHeadline_leadsWithTheFreeMonth() {
        val resources = RuntimeEnvironment.getApplication().resources
        assertEquals("First month free", resources.getString(R.string.premium_offer_title))
        assertEquals(
            "Then $2.99/month · cancel anytime",
            resources.getString(R.string.premium_offer_price_line, "$2.99"),
        )
        val body = resources.getString(R.string.premium_offer_body)
        assertTrue(body.length < 160)
        assertFalse(body.contains("Telegram, analytics"))
        assertTrue(resources.getString(R.string.premium_terms).contains("Google Play"))
        assertEquals("Try 1 month free", resources.getString(R.string.premium_subscribe))
        assertEquals(
            "Continue with the free version",
            resources.getString(R.string.premium_offer_continue_free),
        )
    }

    @Test
    @Config(qualifiers = "ru")
    fun russianHeadline_isTheFreeMonth() {
        val resources = RuntimeEnvironment.getApplication().resources
        assertEquals("Первый месяц бесплатно", resources.getString(R.string.premium_offer_title))
        assertEquals(
            "Затем 2,99 $/мес · отмена в любой момент",
            resources.getString(R.string.premium_offer_price_line, "2,99 $"),
        )
        assertTrue(resources.getString(R.string.premium_terms).contains("Google Play"))
    }

    @Test
    @Config(qualifiers = "es")
    fun spanishHeadline_isTheFreeMonth() {
        val resources = RuntimeEnvironment.getApplication().resources
        assertEquals("Primer mes gratis", resources.getString(R.string.premium_offer_title))
        assertEquals(
            "Luego 2,99 $/mes · cancela cuando quieras",
            resources.getString(R.string.premium_offer_price_line, "2,99 $"),
        )
        assertTrue(resources.getString(R.string.premium_terms).contains("Google Play"))
    }
}

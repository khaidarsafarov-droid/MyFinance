package com.truckerload.presentation.screens.social

import com.truckerload.domain.premium.PremiumOfferQuote
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileHeroStateTest {

    @Test
    fun initials_useTheFirstLetterOfEachName() {
        assertEquals("KS", ProfileHeroState.initials("Khaidar", "Safarov"))
        assertEquals("ХС", ProfileHeroState.initials("хайдар", "сафаров"))
        assertEquals("K", ProfileHeroState.initials("Khaidar", ""))
        assertEquals("S", ProfileHeroState.initials("  ", "Safarov"))
        assertNull(ProfileHeroState.initials("", "  "))
        assertNull(ProfileHeroState.initials("...", "---"))
    }

    @Test
    fun emptyState_isZeroLoads() {
        assertTrue(ProfileHeroState.isEmpty(0))
        assertFalse(ProfileHeroState.isEmpty(1))
        assertFalse(ProfileHeroState.isEmpty(303))
    }

    @Test
    fun rpmChip_followsTheGreenAndRedThresholds() {
        assertEquals(ProfileRpmChip.ABOVE, ProfileHeroState.rpmChip(2.70, redThreshold = 2.0, greenThreshold = 2.50))
        assertEquals(ProfileRpmChip.ABOVE, ProfileHeroState.rpmChip(2.50, redThreshold = 2.0, greenThreshold = 2.50))
        assertEquals(ProfileRpmChip.MID, ProfileHeroState.rpmChip(2.20, redThreshold = 2.0, greenThreshold = 2.50))
        assertEquals(ProfileRpmChip.BELOW, ProfileHeroState.rpmChip(1.90, redThreshold = 2.0, greenThreshold = 2.50))
    }

    @Test
    fun premium_subscribedIgnoresTheOffer() {
        val trial = PremiumOfferQuote("$2.99", includesFreeMonth = true)
        val active = ProfileHeroState.premium(subscribed = true, offerLoaded = false, quote = null)
        assertEquals(ProfilePremiumPhase.ACTIVE, active.phase)
        assertEquals(
            ProfilePremiumPhase.ACTIVE,
            ProfileHeroState.premium(subscribed = true, offerLoaded = true, quote = trial).phase,
        )
    }

    @Test
    fun premium_trialOnlyWhenPlayReturnedAFreeMonthAndAPrice() {
        val trial = PremiumOfferQuote("$2.99", includesFreeMonth = true)
        val paid = PremiumOfferQuote("$4.99", includesFreeMonth = false)
        assertEquals(
            ProfilePremiumPhase.UNKNOWN,
            ProfileHeroState.premium(subscribed = false, offerLoaded = false, quote = trial).phase,
        )
        assertEquals(
            ProfilePremiumPhase.UNKNOWN,
            ProfileHeroState.premium(
                subscribed = false,
                offerLoaded = true,
                quote = PremiumOfferQuote(" ", includesFreeMonth = true),
            ).phase,
        )
        val free = ProfileHeroState.premium(subscribed = false, offerLoaded = true, quote = trial)
        assertEquals(ProfilePremiumPhase.FREE_MONTH, free.phase)
        assertEquals("$2.99", free.price)
        val paidModel = ProfileHeroState.premium(subscribed = false, offerLoaded = true, quote = paid)
        assertEquals(ProfilePremiumPhase.PAID, paidModel.phase)
        assertEquals("$4.99", paidModel.price)
    }

    @Test
    fun driveSyncStamp_formatsOrSkips() {
        val zone = ZoneId.of("America/New_York")
        val at = ZonedDateTime.of(2026, 10, 18, 9, 41, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals("18.10.2026 09:41", ProfileHeroState.driveSyncStamp(at, zone))
        assertNull(ProfileHeroState.driveSyncStamp(0L, zone))
        assertNull(ProfileHeroState.driveSyncStamp(-5L, zone))
    }
}

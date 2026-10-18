package com.truckerload.presentation.screens.settings

import com.truckerload.R
import com.truckerload.domain.premium.PremiumFeature
import com.truckerload.domain.premium.PremiumOfferQuote
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsPremiumStateTest {
    private val freeMonth = PremiumOfferQuote(formattedPrice = "$2.99", includesFreeMonth = true)
    private val paid = PremiumOfferQuote(formattedPrice = "$4.99", includesFreeMonth = false)

    @Test
    fun hero_subscribedIgnoresWhetherTheOfferLoaded() {
        val beforeLoad = SettingsPremiumState.hero(subscribed = true, offerLoaded = false, quote = null)
        val withTrial = SettingsPremiumState.hero(subscribed = true, offerLoaded = true, quote = freeMonth)
        assertEquals(SettingsPremiumPhase.ACTIVE, beforeLoad.phase)
        assertNull(beforeLoad.price)
        assertEquals(SettingsPremiumPhase.ACTIVE, withTrial.phase)
        assertEquals("$2.99", withTrial.price)
    }

    @Test
    fun hero_freeMonthOnlyWhenPlayReturnedAPrice() {
        assertEquals(
            SettingsPremiumPhase.UNKNOWN,
            SettingsPremiumState.hero(subscribed = false, offerLoaded = false, quote = freeMonth).phase,
        )
        assertEquals(
            SettingsPremiumPhase.UNKNOWN,
            SettingsPremiumState.hero(subscribed = false, offerLoaded = true, quote = null).phase,
        )
        assertEquals(
            SettingsPremiumPhase.UNKNOWN,
            SettingsPremiumState.hero(
                subscribed = false,
                offerLoaded = true,
                quote = PremiumOfferQuote("  ", includesFreeMonth = true),
            ).phase,
        )
        val trial = SettingsPremiumState.hero(subscribed = false, offerLoaded = true, quote = freeMonth)
        assertEquals(SettingsPremiumPhase.FREE_MONTH, trial.phase)
        assertEquals("$2.99", trial.price)
    }

    @Test
    fun hero_paidPlanWhenThereIsNoFreeMonth() {
        val model = SettingsPremiumState.hero(subscribed = false, offerLoaded = true, quote = paid)
        assertEquals(SettingsPremiumPhase.PAID, model.phase)
        assertEquals("$4.99", model.price)
    }

    @Test
    fun mark_crownWhenUnlockedAndLockWhenNot() {
        assertEquals(SettingsPremiumMark.NONE, SettingsPremiumState.mark(premiumRow = false, unlocked = true))
        assertEquals(SettingsPremiumMark.NONE, SettingsPremiumState.mark(premiumRow = false, unlocked = false))
        assertEquals(SettingsPremiumMark.CROWN, SettingsPremiumState.mark(premiumRow = true, unlocked = true))
        assertEquals(SettingsPremiumMark.LOCK, SettingsPremiumState.mark(premiumRow = true, unlocked = false))
    }

    @Test
    fun everyPremiumFeatureMapsOntoTheHeroLines() {
        val used = PremiumFeature.entries.map { SettingsPremiumState.groupOf(it) }.toSet()
        assertEquals(SettingsPremiumGroup.entries.toSet(), used)
        val labels = SettingsPremiumGroup.entries.map { SettingsPremiumState.featureLabelRes(it) }
        assertEquals(labels.size, labels.toSet().size)
        assertEquals(R.string.settings_premium_feature_telegram, labels.first())
        assertEquals(R.string.settings_premium_feature_export, labels.last())
    }

    @Test
    fun syncMoment_todayVersusAnotherDay() {
        val zone = ZoneId.of("America/New_York")
        val now = ZonedDateTime.of(2026, 10, 18, 15, 4, 0, 0, zone).toInstant().toEpochMilli()
        val today = ZonedDateTime.of(2026, 10, 18, 9, 41, 0, 0, zone).toInstant().toEpochMilli()
        val moment = SettingsPremiumState.syncMoment(today, now, zone)
        assertTrue(moment!!.isToday)
        assertEquals("09:41", moment.clock)
        assertEquals("18.10.2026 09:41", moment.dateTime)

        val earlier = ZonedDateTime.of(2026, 10, 17, 9, 41, 0, 0, zone).toInstant().toEpochMilli()
        val other = SettingsPremiumState.syncMoment(earlier, now, zone)
        assertFalse(other!!.isToday)
        assertEquals("17.10.2026 09:41", other.dateTime)
        assertNull(SettingsPremiumState.syncMoment(0L, now, zone))
        assertNull(SettingsPremiumState.syncMoment(-1L, now, zone))
    }

    @Test
    fun hourLabelAndRpmAmount_keepStableDigits() {
        assertEquals("07:00", SettingsPremiumState.hourLabel(7))
        assertEquals("22:00", SettingsPremiumState.hourLabel(22))
        assertEquals("23:00", SettingsPremiumState.hourLabel(99))
        assertEquals("2", SettingsPremiumState.rpmAmount(2.0))
        assertEquals("2.50", SettingsPremiumState.rpmAmount(2.5))
        assertEquals("2.99", SettingsPremiumState.rpmAmount(2.99))
    }
}

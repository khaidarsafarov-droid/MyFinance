package com.truckerload.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadAlarmPlannerTest {

    private val now = 1_700_000_000_000L // fixed epoch for determinism
    private val threeHours = 3 * LoadAlarmPlanner.HOUR_MS
    private val ninetyMinutes = (1.5 * LoadAlarmPlanner.HOUR_MS).toLong()

    @Test
    fun shouldOffer_whenPickupInFuture() {
        assertTrue(LoadAlarmPlanner.shouldOfferAlarm(now + threeHours, now))
    }

    @Test
    fun shouldNotOffer_whenPickupNullOrPastOrNow() {
        assertFalse(LoadAlarmPlanner.shouldOfferAlarm(null, now))
        assertFalse(LoadAlarmPlanner.shouldOfferAlarm(now, now))
        assertFalse(LoadAlarmPlanner.shouldOfferAlarm(now - 1_000L, now))
    }

    @Test
    fun buildOffer_exposesBothPresets_whenPickupFarEnough() {
        val offer = LoadAlarmPlanner.buildOffer(now + threeHours, now)
        assertNotNull(offer)
        assertEquals(
            listOf(LoadAlarmPlanner.Preset.TWO_HOURS, LoadAlarmPlanner.Preset.ONE_HOUR),
            offer!!.availablePresets,
        )
        assertTrue(offer.canOffer)
    }

    @Test
    fun buildOffer_hidesTwoHourPreset_whenLessThanTwoHoursRemain() {
        val offer = LoadAlarmPlanner.buildOffer(now + ninetyMinutes, now)
        assertNotNull(offer)
        assertEquals(listOf(LoadAlarmPlanner.Preset.ONE_HOUR), offer!!.availablePresets)
    }

    @Test
    fun buildOffer_hidesAllPresets_whenLessThanOneHourRemain_butStillOffersCustom() {
        val offer = LoadAlarmPlanner.buildOffer(now + 30 * 60_000L, now)
        assertNotNull(offer)
        assertTrue(offer!!.availablePresets.isEmpty())
        assertTrue(offer.canOffer)
    }

    @Test
    fun buildOffer_returnsNull_whenPickupAlreadyStarted() {
        assertNull(LoadAlarmPlanner.buildOffer(now - 1_000L, now))
    }

    @Test
    fun triggerAt_subtractsHours() {
        val pickup = now + 5 * LoadAlarmPlanner.HOUR_MS
        assertEquals(pickup - 2 * LoadAlarmPlanner.HOUR_MS, LoadAlarmPlanner.triggerAt(pickup, 2))
        assertEquals(pickup - LoadAlarmPlanner.HOUR_MS, LoadAlarmPlanner.triggerAt(pickup, 1))
    }

    @Test
    fun isValidAlarmTime_rejectsPastEqualPickupAndAfterPickup() {
        val pickup = now + threeHours
        assertTrue(LoadAlarmPlanner.isValidAlarmTime(now + LoadAlarmPlanner.HOUR_MS, pickup, now))
        assertFalse(LoadAlarmPlanner.isValidAlarmTime(now - 1, pickup, now))
        assertFalse(LoadAlarmPlanner.isValidAlarmTime(now, pickup, now))
        assertFalse(LoadAlarmPlanner.isValidAlarmTime(pickup, pickup, now))
        assertFalse(LoadAlarmPlanner.isValidAlarmTime(pickup + 1, pickup, now))
    }
}

package com.truckerload.domain.alarm

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadAlarmPlannerTest {

    private val now = 1_700_000_000_000L // fixed epoch for determinism

    @Test
    fun futurePickup_offersAlarmWithBothPresets() {
        val pickup = now + 5 * LoadAlarmPlanner.HOUR_MS
        val load = loadWithPu("01/15 10:00 EDT", pickup)
        val offer = LoadAlarmPlanner.offerForLoad(load, nowMillis = now)
        assertNotNull(offer)
        assertEquals(
            listOf(
                LoadAlarmPlanner.Preset.TWO_HOURS_BEFORE,
                LoadAlarmPlanner.Preset.ONE_HOUR_BEFORE,
                LoadAlarmPlanner.Preset.CUSTOM,
            ),
            offer!!.availablePresets,
        )
    }

    @Test
    fun pickupInNinetyMinutes_hidesTwoHourPreset() {
        val pickup = now + (90L * 60L * 1000L)
        val load = loadWithPu("01/15 10:00 EDT", pickup)
        val offer = LoadAlarmPlanner.offerForLoad(load, nowMillis = now)
        assertNotNull(offer)
        assertEquals(
            listOf(
                LoadAlarmPlanner.Preset.ONE_HOUR_BEFORE,
                LoadAlarmPlanner.Preset.CUSTOM,
            ),
            offer!!.availablePresets,
        )
    }

    @Test
    fun pickupInThirtyMinutes_onlyCustom() {
        val pickup = now + (30L * 60L * 1000L)
        val load = loadWithPu("01/15 10:00 EDT", pickup)
        val offer = LoadAlarmPlanner.offerForLoad(load, nowMillis = now)
        assertNotNull(offer)
        assertEquals(listOf(LoadAlarmPlanner.Preset.CUSTOM), offer!!.availablePresets)
    }

    @Test
    fun pastPickup_noOffer() {
        val pickup = now - LoadAlarmPlanner.HOUR_MS
        val load = loadWithPu("01/15 08:00 EDT", pickup)
        assertNull(LoadAlarmPlanner.offerForLoad(load, nowMillis = now))
    }

    @Test
    fun dateOnlyStop_noOffer() {
        val load = sampleLoad(
            stops = listOf(
                Stop(
                    id = 1,
                    loadId = "L1",
                    stopNumber = 1,
                    type = StopType.PU,
                    puNumber = null,
                    note = null,
                    scheduledTime = "2024-01-15",
                    timezone = "",
                    facilityCode = null,
                    fullAddress = "A",
                    city = "A",
                    state = "NC",
                    zip = "",
                ),
            ),
        )
        // Even if date-only parses to a future midnight, we require a clock time.
        assertNull(LoadAlarmPlanner.offerForLoad(load, nowMillis = now))
    }

    @Test
    fun triggerMillis_presets() {
        val pickup = now + 10 * LoadAlarmPlanner.HOUR_MS
        assertEquals(
            pickup - LoadAlarmPlanner.TWO_HOURS_MS,
            LoadAlarmPlanner.triggerMillis(LoadAlarmPlanner.Preset.TWO_HOURS_BEFORE, pickup),
        )
        assertEquals(
            pickup - LoadAlarmPlanner.HOUR_MS,
            LoadAlarmPlanner.triggerMillis(LoadAlarmPlanner.Preset.ONE_HOUR_BEFORE, pickup),
        )
        assertEquals(
            now + 1000L,
            LoadAlarmPlanner.triggerMillis(LoadAlarmPlanner.Preset.CUSTOM, pickup, customMillis = now + 1000L),
        )
    }

    @Test
    fun isValidTrigger_rejectsPastAndAfterPickup() {
        val pickup = now + 5 * LoadAlarmPlanner.HOUR_MS
        assertTrue(LoadAlarmPlanner.isValidTrigger(now + 1000L, pickup, now))
        assertFalse(LoadAlarmPlanner.isValidTrigger(now - 1000L, pickup, now))
        assertFalse(LoadAlarmPlanner.isValidTrigger(pickup + 1000L, pickup, now))
        assertTrue(LoadAlarmPlanner.isValidTrigger(pickup, pickup, now))
    }

    @Test
    fun defaultCustom_prefersTwoHoursBefore() {
        val pickup = now + 5 * LoadAlarmPlanner.HOUR_MS
        assertEquals(
            pickup - LoadAlarmPlanner.TWO_HOURS_MS,
            LoadAlarmPlanner.defaultCustomMillis(pickup, now),
        )
    }

    /**
     * Build a load whose first PU scheduledTime string is ignored for millis — we stub by
     * crafting a US Relay time that [parseScheduledTimeToMillis] will resolve using calendar year.
     * For unit tests we inject millis by using ISO datetime which parses deterministically.
     */
    private fun loadWithPu(@Suppress("UNUSED_PARAMETER") label: String, expectedMillis: Long): Load {
        // Use ISO form so parse is year-explicit and independent of device calendar year.
        val iso = formatIso(expectedMillis)
        return sampleLoad(
            stops = listOf(
                Stop(
                    id = 1,
                    loadId = "L1",
                    stopNumber = 1,
                    type = StopType.PU,
                    puNumber = null,
                    note = null,
                    scheduledTime = iso,
                    timezone = "",
                    facilityCode = null,
                    fullAddress = "Garner, NC",
                    city = "Garner",
                    state = "NC",
                    zip = "",
                ),
            ),
        )
    }

    private fun formatIso(millis: Long): String {
        val cal = java.util.Calendar.getInstance(java.util.Locale.getDefault())
        cal.timeInMillis = millis
        return "%04d-%02d-%02d %02d:%02d".format(
            java.util.Locale.US,
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH) + 1,
            cal.get(java.util.Calendar.DAY_OF_MONTH),
            cal.get(java.util.Calendar.HOUR_OF_DAY),
            cal.get(java.util.Calendar.MINUTE),
        )
    }

    private fun sampleLoad(stops: List<Stop>): Load =
        Load(
            id = "L1",
            tripId = "T-TEST",
            date = "2024-01-15",
            totalRate = 2500.0,
            totalMiles = 850.0,
            pointA = "Garner, NC",
            pointB = "Charlotte, NC",
            puCount = 1,
            delCount = 1,
            weekNumber = 3,
            year = 2024,
            rawMessage = "",
            parsedAt = now,
            updatedAt = now,
            stops = stops,
        )
}

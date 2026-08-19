package com.truckerload.domain.friends

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FriendsLocationSharePolicyTest {

    @Test
    fun clampInterval_mapsToAllowedBuckets() {
        assertEquals(15, FriendsLocationSharePolicy.clampIntervalMinutes(1))
        assertEquals(15, FriendsLocationSharePolicy.clampIntervalMinutes(15))
        assertEquals(30, FriendsLocationSharePolicy.clampIntervalMinutes(30))
        assertEquals(30, FriendsLocationSharePolicy.clampIntervalMinutes(44))
        assertEquals(60, FriendsLocationSharePolicy.clampIntervalMinutes(60))
        assertEquals(60, FriendsLocationSharePolicy.clampIntervalMinutes(120))
    }

    @Test
    fun still_stretchesToSixty_evenIfUserChoseFifteen() {
        assertEquals(
            60,
            FriendsLocationSharePolicy.effectiveIntervalMinutes(
                15,
                FriendsLocationSharePolicy.Motion.STILL,
            ),
        )
    }

    @Test
    fun moving_keepsUserInterval() {
        assertEquals(
            15,
            FriendsLocationSharePolicy.effectiveIntervalMinutes(
                15,
                FriendsLocationSharePolicy.Motion.MOVING,
            ),
        )
        assertEquals(
            30,
            FriendsLocationSharePolicy.effectiveIntervalMinutes(
                30,
                FriendsLocationSharePolicy.Motion.MOVING,
            ),
        )
        assertEquals(
            30,
            FriendsLocationSharePolicy.effectiveIntervalMinutes(
                30,
                FriendsLocationSharePolicy.Motion.UNKNOWN,
            ),
        )
    }

    @Test
    fun skipStationaryFix_onlyWithinSixtyMinutes() {
        val published = 1_000_000L
        assertTrue(
            FriendsLocationSharePolicy.shouldSkipStationaryFix(
                FriendsLocationSharePolicy.Motion.STILL,
                published,
                published + 10 * 60_000L,
            ),
        )
        assertFalse(
            FriendsLocationSharePolicy.shouldSkipStationaryFix(
                FriendsLocationSharePolicy.Motion.STILL,
                published,
                published + 61 * 60_000L,
            ),
        )
        assertFalse(
            FriendsLocationSharePolicy.shouldSkipStationaryFix(
                FriendsLocationSharePolicy.Motion.MOVING,
                published,
                published + 1_000L,
            ),
        )
        assertFalse(
            FriendsLocationSharePolicy.shouldSkipStationaryFix(
                FriendsLocationSharePolicy.Motion.STILL,
                0L,
                published,
            ),
        )
    }

    @Test
    fun motionFromActivityType_stillAndVehicle() {
        assertEquals(FriendsLocationSharePolicy.Motion.STILL, FriendsLocationSharePolicy.motionFromActivityType(3))
        assertEquals(FriendsLocationSharePolicy.Motion.MOVING, FriendsLocationSharePolicy.motionFromActivityType(0))
        assertEquals(FriendsLocationSharePolicy.Motion.UNKNOWN, FriendsLocationSharePolicy.motionFromActivityType(99))
    }

    @Test
    fun liveSession_expiresAfterFifteenMinutes() {
        val now = 5_000_000L
        val until = FriendsLocationSharePolicy.liveUntilFromNow(now)
        assertTrue(FriendsLocationSharePolicy.liveSessionActive(until, now + 60_000L))
        assertFalse(
            FriendsLocationSharePolicy.liveSessionActive(
                until,
                now + FriendsLocationSharePolicy.LIVE_SESSION_MS + 1,
            ),
        )
    }

    @Test
    fun workFlex_isAtLeastFiveMinutes() {
        assertEquals(5L, FriendsLocationSharePolicy.workFlexMinutes(15))
        assertEquals(10L, FriendsLocationSharePolicy.workFlexMinutes(30))
        assertEquals(20L, FriendsLocationSharePolicy.workFlexMinutes(60))
    }
}

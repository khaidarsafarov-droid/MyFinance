package com.truckerload.domain.friends

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FriendsRouteDisplayModeTest {

    @Test
    fun remainingMode_hidesPastAndKeepsAhead() {
        val past = listOf(LatLngPoint(1.0, 1.0), LatLngPoint(2.0, 2.0))
        val remaining = listOf(LatLngPoint(2.0, 2.0), LatLngPoint(3.0, 3.0))
        assertTrue(
            FriendsRouteDisplay.pastToDraw(FriendsRouteDisplayMode.REMAINING, past).isEmpty(),
        )
        assertEquals(
            remaining,
            FriendsRouteDisplay.remainingToDraw(FriendsRouteDisplayMode.REMAINING, remaining),
        )
    }

    @Test
    fun traveledMode_hidesRemainingAndKeepsPast() {
        val past = listOf(LatLngPoint(1.0, 1.0), LatLngPoint(2.0, 2.0))
        val remaining = listOf(LatLngPoint(2.0, 2.0), LatLngPoint(3.0, 3.0))
        assertEquals(
            past,
            FriendsRouteDisplay.pastToDraw(FriendsRouteDisplayMode.TRAVELED, past),
        )
        assertTrue(
            FriendsRouteDisplay.remainingToDraw(FriendsRouteDisplayMode.TRAVELED, remaining).isEmpty(),
        )
    }

    @Test
    fun fromStored_mapsBoolean() {
        assertEquals(FriendsRouteDisplayMode.REMAINING, FriendsRouteDisplayMode.fromStored(false))
        assertEquals(FriendsRouteDisplayMode.TRAVELED, FriendsRouteDisplayMode.fromStored(true))
    }
}

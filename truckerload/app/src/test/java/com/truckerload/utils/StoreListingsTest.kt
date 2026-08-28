package com.truckerload.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreListingsTest {

    @Test
    fun playStoreHttpsUrl_usesPackageName() {
        assertEquals(
            "https://play.google.com/store/apps/details?id=com.truckorig",
            StoreListings.playStoreHttpsUrl("com.truckorig"),
        )
    }

    @Test
    fun appStoreHttpsUrl_blankUntilAccountReady() {
        assertNull(StoreListings.appStoreHttpsUrl(""))
        assertNull(StoreListings.appStoreHttpsUrl("   "))
        assertNull(StoreListings.appStoreHttpsUrl())
        assertEquals(
            "https://apps.apple.com/app/id1234567890",
            StoreListings.appStoreHttpsUrl("1234567890"),
        )
        assertEquals(
            "https://apps.apple.com/app/id1234567890",
            StoreListings.appStoreHttpsUrl("id1234567890"),
        )
    }

    @Test
    fun shareText_addsIosLinkOnlyWhenReady() {
        val playOnly = StoreListings.shareText(
            intro = "TruckoRig",
            androidLabel = "Android:",
            iosLabel = "iPhone:",
            playUrl = "https://play.google.com/store/apps/details?id=com.truckorig",
            appStoreUrl = null,
        )
        assertTrue(playOnly.contains("https://play.google.com/store/apps/details?id=com.truckorig"))
        assertFalse(playOnly.contains("iPhone:"))

        val both = StoreListings.shareText(
            intro = "TruckoRig",
            androidLabel = "Android:",
            iosLabel = "iPhone:",
            playUrl = "https://play.google.com/store/apps/details?id=com.truckorig",
            appStoreUrl = "https://apps.apple.com/app/id1234567890",
        )
        assertTrue(both.contains("iPhone:"))
        assertTrue(both.contains("https://apps.apple.com/app/id1234567890"))
    }
}

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

    @Test
    fun shareAndDriveCopy_staysUserFacingInAllLocales() {
        val folders = listOf("values", "values-en", "values-ru", "values-es")
        val banned = listOf(
            "store account is ready",
            "аккаунт в магазине",
            "cuenta de la tienda",
            "will be added automatically",
            "App Store для iPhone подставится",
            "App Store para iPhone se agregará",
            "stays in Room",
            "остаются в Room",
            "se quedan en Room",
            "iCloud is not available",
            "iCloud на Android",
            "iCloud no está disponible",
            "журнал грузов Amazon Relay",
            "Amazon Relay journal",
            "diario de Amazon Relay",
            "Вставить из Relay",
            "Paste from Relay",
            "Pegar desde Relay",
        )
        folders.forEach { folder ->
            val file = java.io.File("src/main/res/$folder/strings.xml").takeIf { it.isFile }
                ?: java.io.File("app/src/main/res/$folder/strings.xml")
            val xml = file.readText()
            banned.forEach { phrase ->
                assertFalse("$folder still contains “$phrase”", xml.contains(phrase))
            }
            assertTrue(
                "$folder is missing share play-only copy",
                xml.contains("name=\"settings_share_app_body_play_only\""),
            )
        }
    }
}

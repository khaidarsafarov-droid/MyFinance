package com.truckerload.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TelegramFgsQuotaTest {

    @Before
    fun setUp() {
        TelegramFgsQuota.resetForTests()
    }

    @Test
    fun markTimedOut_pausesUntilCleared() {
        assertFalse(TelegramFgsQuota.isPaused())
        TelegramFgsQuota.markTimedOut()
        assertTrue(TelegramFgsQuota.isPaused())
        TelegramFgsQuota.clearPause()
        assertFalse(TelegramFgsQuota.isPaused())
    }
}

class TelegramManifestDropsDataSyncFgsTest {

    @Test
    fun manifest_hasNoDataSyncForegroundService() {
        val candidates = listOf(
            java.io.File("src/main/AndroidManifest.xml"),
            java.io.File("app/src/main/AndroidManifest.xml"),
            java.io.File("../app/src/main/AndroidManifest.xml"),
        )
        val text = candidates.firstOrNull { it.isFile }?.readText()
            ?: error("AndroidManifest.xml not found")
        assertTrue(text.contains("FOREGROUND_SERVICE_DATA_SYNC\" tools:node=\"remove\""))
        assertTrue(!text.contains("android:foregroundServiceType"))
        assertTrue(!text.contains(".sync.TelegramBotForegroundService"))
    }
}

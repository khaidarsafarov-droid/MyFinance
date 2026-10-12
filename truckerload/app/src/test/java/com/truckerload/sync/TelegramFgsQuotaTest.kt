package com.truckerload.sync

import org.junit.Assert.assertTrue
import org.junit.Test

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

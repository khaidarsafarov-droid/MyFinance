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
        // WorkManager 2.10.5 no longer merges DATA_SYNC, so a tools:node=remove
        // marker would be a no-op. The permission must simply stay absent.
        assertTrue(
            "FOREGROUND_SERVICE_DATA_SYNC must not appear in the manifest",
            "FOREGROUND_SERVICE_DATA_SYNC" !in text,
        )
        assertTrue(!text.contains("android:foregroundServiceType"))
        assertTrue(!text.contains(".sync.TelegramBotForegroundService"))
    }
}

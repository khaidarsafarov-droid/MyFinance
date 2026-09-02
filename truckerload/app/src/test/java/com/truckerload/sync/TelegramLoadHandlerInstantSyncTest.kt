package com.truckerload.sync

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramLoadHandlerInstantSyncTest {

    @Test
    fun botPersist_batchesWritesAndAwaitsWidgetPaint() {
        val handler = readMainSource("com/truckerload/sync/TelegramLoadHandler.kt")
        assertTrue(handler.contains("loadProcessor.processLoads"))
        assertTrue(handler.contains("WidgetRefresh.refreshAndUpdate"))
        assertTrue(handler.contains("private suspend fun notifyIfChanged"))
        assertTrue(!handler.contains("WidgetDataUpdater.updateWidgetData"))
        assertTrue(!handler.contains("WidgetUpdateWorker.refreshNow"))
    }

    @Test
    fun pollLoop_doesNotForceAOneSecondWaitAfterASavedLoad() {
        val service = readMainSource("com/truckerload/sync/TelegramBotForegroundService.kt")
        assertTrue(service.contains("coerceIn(0, 60)"))
        assertTrue(service.contains("if (delaySec > 0L) delay(delaySec * 1000)"))
        assertTrue(!service.contains("coerceIn(1, 60)"))
    }

    private fun readMainSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/$relativePath"),
            File("app/src/main/java/$relativePath"),
            File("../app/src/main/java/$relativePath"),
        )
        return candidates.firstOrNull(File::isFile)?.readText()
            ?: error("Main source not found: $relativePath")
    }
}

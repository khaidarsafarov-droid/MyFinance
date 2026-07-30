package com.truckerload.sync.telegram

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramSyncSplitArchitectureTest {

    @Test
    fun engine_isThinFacadeDelegatingToTelegramPackage() {
        val engine = readMainSource("com/truckerload/sync/TelegramBotSyncEngine.kt")
        assertTrue(engine.contains("TelegramApiClient"))
        assertTrue(engine.contains("TelegramMessageParser"))
        assertTrue(engine.contains("TelegramSyncScheduler"))
        assertTrue(engine.contains("TelegramStateMachine"))
        assertTrue(engine.contains("TelegramUpdateDispatcher"))
        assertTrue(engine.contains("stoppedOnFailure"))
        assertTrue(!engine.contains("parseLoadsFromMessage"))
    }

    @Test
    fun offsetStore_livesInSyncScheduler() {
        val source = readMainSource("com/truckerload/sync/telegram/TelegramSyncScheduler.kt")
        assertTrue(source.contains("loadNextRequestOffset"))
        assertTrue(source.contains("persistNextRequestOffset"))
        assertTrue(source.contains("nextDelaySeconds"))
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

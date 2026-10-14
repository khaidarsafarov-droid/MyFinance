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

    @Test
    fun inboundMessage_isAcknowledgedBeforeAnyParseOrDownload() {
        val dispatcher = readMainSource("com/truckerload/sync/telegram/TelegramUpdateDispatcher.kt")
        val ack = dispatcher.indexOf("acknowledgeNow(update.chatId)")
        val persistAfterAck = dispatcher.indexOf("persistIncoming", ack)
        assertTrue(ack >= 0 && persistAfterAck > ack)
        assertTrue(dispatcher.contains("sendChatAction"))
        val media = readMainSource("com/truckerload/sync/telegram/TelegramFileIngestHandler.kt")
        assertTrue(!media.contains("sync_processing"))
        val poller = readMainSource("com/truckerload/sync/TelegramBotForegroundService.kt")
        assertTrue(!poller.contains("coerceIn(2, 60)"))
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

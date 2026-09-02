package com.truckerload.sync.telegram

import org.junit.Assert.assertEquals
import org.junit.Test

class TelegramSyncSchedulerTest {

    @Test
    fun delayAfterPoll_isZeroWhenALoadWasSaved() {
        assertEquals(0L, TelegramSyncScheduler.delayAfterPoll(processed = 1, updatesNonEmpty = true))
        assertEquals(0L, TelegramSyncScheduler.delayAfterPoll(processed = 3, updatesNonEmpty = false))
    }

    @Test
    fun delayAfterPoll_waitsWhenNothingWasSaved() {
        assertEquals(1L, TelegramSyncScheduler.delayAfterPoll(processed = 0, updatesNonEmpty = true))
        assertEquals(2L, TelegramSyncScheduler.delayAfterPoll(processed = 0, updatesNonEmpty = false))
    }
}

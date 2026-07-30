package com.truckerload.sync.telegram

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramSyncSchedulerTest {

    private val scheduler = TelegramSyncScheduler()

    @Test
    fun nextDelayAfterPoll_isShortWhenWorkDone() {
        assertEquals(1L, scheduler.nextDelayAfterPoll(processedUpdates = 3, receivedUpdates = 3))
        assertEquals(1L, scheduler.nextDelayAfterPoll(processedUpdates = 0, receivedUpdates = 2))
        assertEquals(2L, scheduler.nextDelayAfterPoll(processedUpdates = 0, receivedUpdates = 0))
    }

    @Test
    fun delayAfterGetUpdatesFailure_conflictUsesLongerBackoff() {
        assertEquals(45L, scheduler.delayAfterGetUpdatesFailure("getUpdates failed: 409 Conflict"))
        assertEquals(30L, scheduler.delayAfterGetUpdatesFailure("timeout"))
    }

    @Test
    fun stateMachine_transitionsIdlePollingSyncingIdle() {
        val machine = TelegramStateMachine()
        assertEquals(TelegramSyncState.Idle, machine.current())
        assertEquals(TelegramSyncState.Polling, machine.beginPoll())
        assertEquals(TelegramSyncState.Syncing, machine.beginSync())
        assertTrue(machine.fail("boom") is TelegramSyncState.Error)
        assertEquals(TelegramSyncState.Idle, machine.idle())
    }
}

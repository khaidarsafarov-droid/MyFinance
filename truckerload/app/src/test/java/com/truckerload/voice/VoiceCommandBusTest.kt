package com.truckerload.voice

import com.truckerload.presentation.navigation.Routes
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class VoiceCommandBusTest {

    @Before
    @After
    fun drainQueue() {
        VoiceCommandBus.drainForTests()
    }

    @Test
    fun offer_queuesCommandsInOrder() = runTest {
        val first = AppVoiceAction.OpenScreen(Routes.HOME)
        val second = AppVoiceAction.OpenScreen(Routes.SETTINGS)

        VoiceCommandBus.offer(first)
        VoiceCommandBus.offer(second)

        assertEquals(first, VoiceCommandBus.receive())
        assertEquals(second, VoiceCommandBus.receive())
    }

    @Test
    fun offer_doesNotOverwriteUnconsumedCommand() = runTest {
        val first = AppVoiceAction.OpenScreen(Routes.HOME)
        val second = AppVoiceAction.OpenScreen(Routes.ANALYTICS)

        VoiceCommandBus.offer(first)
        VoiceCommandBus.offer(second)

        val pending = async { VoiceCommandBus.receive() }
        testScheduler.runCurrent()
        assertEquals(first, pending.await())

        assertEquals(second, VoiceCommandBus.receive())
    }

    @Test
    fun tryReceive_returnsNullWhenEmpty() {
        assertNull(VoiceCommandBus.tryReceive())
    }
}

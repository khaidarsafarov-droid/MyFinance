package com.truckerload.data.local

import java.util.concurrent.atomic.AtomicBoolean
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JournalRecoveryGateTest {

    @After
    fun tearDown() {
        JournalRecoveryGate.resetForTests()
    }

    @Test
    fun backgroundOpenWaitsUntilRecoveryReleases() {
        JournalRecoveryGate.resetForTests()
        JournalRecoveryGate.arm()
        val entered = AtomicBoolean(false)
        val thread = Thread {
            JournalRecoveryGate.awaitOffMainForTests()
            entered.set(true)
        }
        thread.start()
        Thread.sleep(150)
        assertFalse(entered.get())
        JournalRecoveryGate.release()
        thread.join(2_000)
        assertTrue(entered.get())
    }

    @Test
    fun mainThreadDoesNotBlockWhenRecoveryIsStillRunning() {
        JournalRecoveryGate.resetForTests()
        JournalRecoveryGate.arm()
        val error = runCatching { JournalRecoveryGate.awaitBeforeOpenForTests() }.exceptionOrNull()
        assertTrue(error is IllegalStateException)
        JournalRecoveryGate.release()
        JournalRecoveryGate.awaitBeforeOpenForTests()
    }

    @Test
    fun unarmedGateDoesNotWait() {
        JournalRecoveryGate.resetForTests()
        JournalRecoveryGate.awaitBeforeOpenForTests()
        JournalRecoveryGate.awaitOffMainForTests()
    }
}

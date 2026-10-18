package com.truckerload.data.local

import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
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

    @Test
    fun databaseOpenWaitsUntilRecoveryFinishes() {
        val context = RuntimeEnvironment.getApplication()
        val order = Collections.synchronizedList(mutableListOf<String>())
        val failure = AtomicReference<Throwable>(null)
        JournalRecoveryGate.resetForTests()
        JournalRecoveryGate.enforceForTests = true
        JournalRecoveryGate.arm()
        val thread = Thread {
            try {
                AppDatabase.getInstance(context, "gate-before-open")
                order.add("db-open")
            } catch (t: Throwable) {
                failure.set(t)
            }
        }
        thread.start()
        try {
            Thread.sleep(200)
            assertFalse(order.contains("db-open"))
            assertNull(failure.get())
            order.add("recovery-finished")
            JournalRecoveryGate.release()
            thread.join(8_000)
            assertNull(failure.get()?.toString(), failure.get())
            assertEquals(listOf("recovery-finished", "db-open"), order)
        } finally {
            JournalRecoveryGate.release()
            thread.join(2_000)
            AppDatabase.closeCurrent()
            JournalRecoveryGate.resetForTests()
        }
    }
}

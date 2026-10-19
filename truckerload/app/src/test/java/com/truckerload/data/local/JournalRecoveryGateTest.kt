package com.truckerload.data.local

import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
    fun mainThreadDoesNotThrowOrBlockWhileRecoveryIsStillRunning() {
        JournalRecoveryGate.resetForTests()
        JournalRecoveryGate.enforceForTests = true
        JournalRecoveryGate.arm()
        val started = System.nanoTime()
        JournalRecoveryGate.awaitBeforeOpen()
        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        assertTrue("main thread blocked for ${elapsedMs}ms", elapsedMs < 400)
        JournalRecoveryGate.release()
        JournalRecoveryGate.awaitBeforeOpen()
    }

    @Test
    fun unarmedGateDoesNotWait() {
        JournalRecoveryGate.resetForTests()
        JournalRecoveryGate.awaitBeforeOpenForTests()
        JournalRecoveryGate.awaitOffMainForTests()
    }

    @Test
    fun getInstanceOnMainDoesNotOpenUntilRecoveryFinishes() {
        val context = RuntimeEnvironment.getApplication()
        val order = Collections.synchronizedList(mutableListOf<String>())
        val failure = AtomicReference<Throwable>(null)
        JournalRecoveryGate.resetForTests()
        JournalRecoveryGate.enforceForTests = true
        JournalRecoveryGate.arm()
        val started = System.nanoTime()
        val db = AppDatabase.getInstance(context, "gate-before-open")
        val buildMs = (System.nanoTime() - started) / 1_000_000
        assertTrue("getInstance blocked the main thread for ${buildMs}ms", buildMs < 500)
        val thread = Thread {
            try {
                db.openHelper.writableDatabase
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

    @Test
    fun mainThreadOpenDoesNotThrowWhenRecoveryIsStillRunning() {
        val context = RuntimeEnvironment.getApplication()
        JournalRecoveryGate.resetForTests()
        JournalRecoveryGate.enforceForTests = true
        JournalRecoveryGate.mainOpenWaitMillis = 100
        JournalRecoveryGate.arm()
        val db = AppDatabase.getInstance(context, "gate-main-open")
        val releaser = Thread {
            Thread.sleep(350)
            JournalRecoveryGate.release()
        }
        releaser.start()
        try {
            val opened = db.openHelper.writableDatabase
            assertNotNull(opened)
        } finally {
            JournalRecoveryGate.release()
            releaser.join(2_000)
            AppDatabase.closeCurrent()
            JournalRecoveryGate.resetForTests()
        }
    }
}

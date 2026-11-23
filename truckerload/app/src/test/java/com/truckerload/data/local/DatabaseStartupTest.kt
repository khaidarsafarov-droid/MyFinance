package com.truckerload.data.local

import android.os.Looper
import com.truckerload.data.preferences.AuthStore
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseStartupTest {

    @Before
    fun setUp() {
        DatabaseStartup.resetForTests()
        AuthStore.resetForTests()
    }

    @After
    fun tearDown() {
        DatabaseStartup.resetForTests()
        AuthStore.resetForTests()
        JournalRecoveryGate.resetForTests()
    }

    @Test
    fun startReturnsImmediatelyThenBecomesReadyOffTheMainThread() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val auth = AuthStore(context)
        val opener = AtomicReference<Thread>()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        DatabaseStartup.openForTests = {
            opener.set(Thread.currentThread())
            entered.complete(Unit)
            release.await()
        }
        try {
            val started = System.nanoTime()
            DatabaseStartup.start(context, auth)
            val elapsedMs = (System.nanoTime() - started) / 1_000_000
            assertTrue("start blocked the caller for ${elapsedMs}ms", elapsedMs < 400)
            assertEquals(DbState.Loading, DatabaseStartup.state.value)
            withTimeout(5_000) { entered.await() }
            assertNotEquals(Looper.getMainLooper().thread, opener.get())
            release.complete(Unit)
            val ready = withTimeout(5_000) {
                DatabaseStartup.state.first { it is DbState.Ready }
            }
            assertEquals(DbState.Ready, ready)
        } finally {
            if (!release.isCompleted) release.complete(Unit)
        }
    }

    @Test
    fun failedOpenStaysRecoverableUntilRetry() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val auth = AuthStore(context)
        val attempts = AtomicInteger(0)
        DatabaseStartup.openForTests = {
            if (attempts.incrementAndGet() == 1) error("journal open failed")
        }
        DatabaseStartup.start(context, auth)
        val failed = withTimeout(5_000) {
            DatabaseStartup.state.first { it is DbState.Failed }
        } as DbState.Failed
        assertTrue(failed.recoverable)
        DatabaseStartup.retry()
        withTimeout(5_000) {
            DatabaseStartup.state.first { it is DbState.Ready }
        }
        assertEquals(2, attempts.get())
    }

    @Test
    fun restoreRunsTheOpenAgainAfterFailure() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val auth = AuthStore(context)
        val attempts = AtomicInteger(0)
        DatabaseStartup.openForTests = {
            if (attempts.incrementAndGet() == 1) error("journal open failed")
        }
        DatabaseStartup.start(context, auth)
        withTimeout(5_000) {
            DatabaseStartup.state.first { it is DbState.Failed }
        }
        DatabaseStartup.restore()
        withTimeout(5_000) {
            DatabaseStartup.state.first { it is DbState.Ready }
        }
        assertEquals(2, attempts.get())
    }
}

package com.truckerload.data.preferences

import android.content.Context
import com.truckerload.data.local.JournalRecoveryGate
import java.util.ArrayDeque
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthStoreColdStartTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        AuthStore.resetForTests()
        SecurePreferences.resetFallbackForTests()
        JournalRecoveryGate.resetForTests()
    }

    @After
    fun tearDown() {
        AuthStore.ignoreBootstrapForTests = false
        AuthStore.resetForTests()
        SecurePreferences.resetFallbackForTests()
        JournalRecoveryGate.resetForTests()
    }

    @Test
    fun asyncColdStart_staysUnknownUntilPublish_thenRestoresTheDiskSession() {
        AuthStore(context, AuthStore.ColdStart.SYNC).login(
            userId = "google_deadbeef",
            email = "driver@example.com",
            rememberMe = true,
            provider = AuthProvider.GOOGLE,
        )
        AuthStore.ignoreBootstrapForTests = true
        AuthStore.resetForTests()
        JournalRecoveryGate.arm()
        val paused = QueueDispatcher()
        AuthStore.bootstrapDispatcherOverride = paused

        val async = AuthStore(context, AuthStore.ColdStart.ASYNC)
        assertFalse(
            "mode=${AuthStore.lastBootstrapModeForTests} peeked=${async.peekedUserIdOrNull()}",
            async.sessionKnown.value,
        )
        assertEquals(AuthSessionHealth.UNKNOWN, async.sessionHealth.value)
        // Main thread must not treat the gap as signed-out.
        assertNull(async.currentUserIdOrNull())
        assertTrue(paused.hasQueuedWork())
        paused.drain()

        assertEquals("google_deadbeef", async.peekedUserIdOrNull())
        assertFalse(async.sessionKnown.value)

        async.publishColdStart()
        assertTrue(async.sessionKnown.value)
        assertEquals("google_deadbeef", async.currentUserIdOrNull())
        assertEquals(true, async.isLoggedIn.value)
        assertEquals(AuthSessionHealth.VERIFIED, async.sessionHealth.value)
    }

    @Test
    fun loginDuringColdStart_isNotReplacedByTheDiskSnapshot() {
        AuthStore(context, AuthStore.ColdStart.SYNC).login(
            userId = "google_disk",
            email = "disk@example.com",
            rememberMe = true,
            provider = AuthProvider.GOOGLE,
        )
        AuthStore.ignoreBootstrapForTests = true
        AuthStore.resetForTests()
        JournalRecoveryGate.arm()
        val paused = QueueDispatcher()
        AuthStore.bootstrapDispatcherOverride = paused

        val async = AuthStore(context, AuthStore.ColdStart.ASYNC)
        async.login(
            userId = "google_fresh",
            email = "fresh@example.com",
            rememberMe = true,
            provider = AuthProvider.GOOGLE,
        )
        assertTrue(
            "queued=${paused.hasQueuedWork()} known=${async.sessionKnown.value} " +
                "peeked=${async.peekedUserIdOrNull()} mode=${AuthStore.lastBootstrapModeForTests}",
            paused.hasQueuedWork(),
        )
        paused.drain()
        runBlocking {
            withTimeout(5_000) { async.awaitDiskSnapshot() }
        }

        assertEquals("google_fresh", async.peekedUserIdOrNull())
        async.publishColdStart()
        assertEquals("google_fresh", async.currentUserIdOrNull())
        assertEquals(true, async.isLoggedIn.value)
    }

    @Test
    fun signedInSession_survivesRestart() {
        AuthStore(context, AuthStore.ColdStart.SYNC).login(
            userId = "google_restart",
            email = "driver@example.com",
            rememberMe = true,
            provider = AuthProvider.GOOGLE,
        )
        // Process death: in-memory session is gone, encrypted prefs stay on disk.
        AuthStore.resetForTests()
        val restored = AuthStore(context, AuthStore.ColdStart.SYNC)
        assertTrue(restored.sessionKnown.value)
        assertTrue(restored.isLoggedIn.value)
        assertEquals("google_restart", restored.currentUserIdOrNull())
        assertEquals("driver@example.com", restored.email.value)
        assertEquals(AuthProvider.GOOGLE, restored.authProvider())
    }

    @Test
    fun markSessionHealth_doesNotRequireACommitOnTheCaller() {
        val auth = AuthStore(context)
        auth.login(
            userId = "local_health",
            email = "local@device",
            rememberMe = true,
            provider = AuthProvider.EMAIL,
        )
        auth.markSessionHealth(AuthSessionHealth.OFFLINE_LOCAL)
        assertEquals(AuthSessionHealth.OFFLINE_LOCAL, auth.sessionHealth.value)
    }
}

/** Runs scheduled coroutines only when [drain] is called, on the caller thread. */
private class QueueDispatcher : CoroutineDispatcher() {
    private val queue = ArrayDeque<Runnable>()

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        queue.add(block)
    }

    fun hasQueuedWork(): Boolean = queue.isNotEmpty()

    fun drain() {
        while (queue.isNotEmpty()) {
            queue.removeFirst().run()
        }
    }
}

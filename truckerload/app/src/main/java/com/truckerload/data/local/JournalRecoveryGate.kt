package com.truckerload.data.local

import android.os.Looper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Holds Room / SQLCipher open until installed-journal recovery finishes.
 *
 * Unarmed (Robolectric, and any process that never cold-starts recovery) is a no-op.
 * Background callers wait. The main thread never blocks and never opens the database early.
 */
internal object JournalRecoveryGate {
    private val lock = Any()
    private var latch: CountDownLatch? = null
    private var done: CompletableDeferred<Unit>? = null
    private val running = AtomicBoolean(false)

    /** When set, Robolectric also waits. Production never sets this. */
    internal var enforceForTests: Boolean = false

    /**
     * How long a main-thread open waits before it logs and keeps waiting on IO.
     * The SQLCipher file still does not open until recovery releases the gate.
     */
    internal var mainOpenWaitMillis: Long = MAIN_OPEN_WAIT_MS

    fun arm() {
        synchronized(lock) {
            if (latch != null) return
            latch = CountDownLatch(1)
            done = CompletableDeferred()
        }
    }

    fun isArmed(): Boolean = synchronized(lock) { latch != null }

    fun start(scope: CoroutineScope, block: suspend () -> Unit) {
        if (!running.compareAndSet(false, true)) return
        synchronized(lock) {
            if (latch == null) {
                latch = CountDownLatch(1)
                done = CompletableDeferred()
            }
        }
        scope.launch(Dispatchers.IO) {
            try {
                block()
            } finally {
                release()
            }
        }
    }

    fun release() {
        val deferred: CompletableDeferred<Unit>
        val held: CountDownLatch
        synchronized(lock) {
            deferred = done ?: return
            held = latch ?: return
        }
        held.countDown()
        deferred.complete(Unit)
    }

    suspend fun await() {
        val deferred = synchronized(lock) { done } ?: return
        deferred.await()
    }

    /**
     * Blocks a background opener until recovery releases the gate.
     * A main-thread caller returns immediately and does not throw; the open helper
     * moves that wait onto [RecoveryOpenThreads].
     */
    fun awaitBeforeOpen() {
        if (skipWait()) return
        if (isMainThread()) return
        awaitLatch()
    }

    /** True when this thread is main and the journal file must not be opened yet. */
    fun deferCurrentThread(): Boolean = isMainThread() && !skipWait() && isAwaiting()

    internal fun callerIsMainThread(): Boolean = isMainThread()

    /** Test hook: on the main thread this returns without throwing or blocking. */
    internal fun awaitBeforeOpenForTests() {
        if (isMainThread()) return
        awaitLatch()
    }

    /** Test hook: wait on a background thread even under Robolectric. */
    internal fun awaitOffMainForTests() {
        awaitLatch()
    }

    private fun skipWait(): Boolean =
        DatabasePassphraseStore.isRobolectric() && !enforceForTests

    private fun isAwaiting(): Boolean {
        val held = synchronized(lock) { latch } ?: return false
        return held.count > 0L
    }

    private fun awaitLatch() {
        val held = synchronized(lock) { latch } ?: return
        if (held.count == 0L) return
        held.await()
    }

    internal fun resetForTests() {
        synchronized(lock) {
            latch?.countDown()
            done?.complete(Unit)
            latch = null
            done = null
            running.set(false)
            enforceForTests = false
            mainOpenWaitMillis = MAIN_OPEN_WAIT_MS
        }
    }

    private fun isMainThread(): Boolean {
        val main = Looper.getMainLooper() ?: return false
        return Looper.myLooper() == main
    }

    private const val MAIN_OPEN_WAIT_MS = 2_000L
}

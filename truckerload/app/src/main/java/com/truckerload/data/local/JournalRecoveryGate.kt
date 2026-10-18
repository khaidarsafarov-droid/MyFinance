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
     * Background Room open waits until recovery releases the gate.
     * A main-thread caller does not block; opening now would race a journal copy.
     */
    fun awaitBeforeOpen() {
        if (DatabasePassphraseStore.isRobolectric() && !enforceForTests) return
        awaitLatch(rejectMainThread = true)
    }

    /** Test hook: wait even under Robolectric, and fail if this is the main thread. */
    internal fun awaitBeforeOpenForTests() {
        awaitLatch(rejectMainThread = true)
    }

    /** Test hook: wait on a background thread even under Robolectric. */
    internal fun awaitOffMainForTests() {
        awaitLatch(rejectMainThread = false)
    }

    private fun awaitLatch(rejectMainThread: Boolean) {
        val held = synchronized(lock) { latch } ?: return
        if (held.count == 0L) return
        if (rejectMainThread) {
            check(!isMainThread()) {
                "Room opened on the main thread before journal recovery finished"
            }
        }
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
        }
    }

    private fun isMainThread(): Boolean {
        val main = Looper.getMainLooper() ?: return false
        return Looper.myLooper() == main
    }
}

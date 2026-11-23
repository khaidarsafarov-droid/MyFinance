package com.truckerload.data.local

import android.content.Context
import android.util.Log
import com.truckerload.BuildConfig
import com.truckerload.data.preferences.AccountIds
import com.truckerload.data.preferences.AuthStore
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Whether the process has finished opening the on-device journal.
 *
 * [Loading] is the cold start. [Ready] means a background open finished, or there is
 * no signed-in journal to open. [Failed] means that open threw; [Failed.recoverable]
 * is true when Retry can run it again.
 */
sealed interface DbState {
    data object Loading : DbState

    data object Ready : DbState

    data class Failed(val recoverable: Boolean) : DbState
}

/**
 * Opens the Room / SQLCipher journal on [Dispatchers.IO].
 *
 * [start] returns immediately. The main thread never waits on this object and never
 * opens the file. Journal recovery still runs first: [openInstalled] suspends on
 * [JournalRecoveryGate] before the first [androidx.sqlite.db.SupportSQLiteDatabase].
 */
object DatabaseStartup {
    private const val TAG = "DatabaseStartup"
    private const val AWAIT_TIMEOUT_MS = 30_000L

    private val _state = MutableStateFlow<DbState>(DbState.Loading)
    val state: StateFlow<DbState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val generation = AtomicInteger(0)
    private val started = AtomicBoolean(false)

    private var appContext: Context? = null
    private var authStore: AuthStore? = null

    /** Test seam. Production uses [openInstalled]. */
    internal var openForTests: (suspend () -> Unit)? = null

    fun start(context: Context, authStore: AuthStore) {
        appContext = context.applicationContext
        this.authStore = authStore
        if (!started.compareAndSet(false, true)) return
        if (skipRealOpen()) {
            _state.value = DbState.Ready
            return
        }
        launchOpen(recoverFirst = false)
    }

    /** Try the same open again. Does not copy journals a second time. */
    fun retry() {
        if (appContext == null || authStore == null) return
        launchOpen(recoverFirst = false)
    }

    /**
     * Run installed-journal recovery, then open. The error screen uses this when
     * the first open failed and the driver asks to recover the journal.
     */
    fun restore() {
        if (appContext == null || authStore == null) return
        launchOpen(recoverFirst = true)
    }

    /**
     * Suspend until startup leaves [DbState.Loading]. The wait runs on
     * [Dispatchers.IO] and is bounded. True only for [DbState.Ready].
     */
    suspend fun awaitReadyOrFalse(timeoutMs: Long = AWAIT_TIMEOUT_MS): Boolean {
        if (bypassAwait()) return true
        val result = withTimeoutOrNull(timeoutMs) {
            withContext(Dispatchers.IO) {
                state.first { it !is DbState.Loading }
            }
        }
        return result is DbState.Ready
    }

    /**
     * Background threads only. Bounded. The main thread gets false and does not wait.
     * Robolectric's test thread is allowed through so existing opens keep working.
     */
    fun awaitReadyBlockingOrFalse(timeoutMs: Long = AWAIT_TIMEOUT_MS): Boolean {
        if (JournalRecoveryGate.callerIsMainThread()) {
            if (allowsMainThreadInTests()) return true
            Log.w(TAG, "Refusing to wait for the database on the main thread")
            return false
        }
        if (bypassAwait()) return true
        val result = runBlocking {
            withTimeoutOrNull(timeoutMs) {
                state.first { it !is DbState.Loading }
            }
        }
        return result is DbState.Ready
    }

    internal fun resetForTests() {
        generation.incrementAndGet()
        started.set(false)
        openForTests = null
        appContext = null
        authStore = null
        _state.value = DbState.Loading
    }

    private fun launchOpen(recoverFirst: Boolean) {
        val ticket = generation.incrementAndGet()
        _state.value = DbState.Loading
        scope.launch {
            val next = openCatching(recoverFirst)
            if (ticket == generation.get()) {
                _state.value = next
            }
        }
    }

    private suspend fun openCatching(recoverFirst: Boolean): DbState {
        return try {
            if (recoverFirst && openForTests == null) {
                val context = appContext
                val auth = authStore
                if (context != null && auth != null) {
                    InstalledJournalRecovery.recover(context, auth)
                }
            }
            val custom = openForTests
            if (custom != null) custom() else openInstalled()
            DbState.Ready
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            Log.e(TAG, "Journal open failed", error)
            DbState.Failed(recoverable = error !is Error)
        }
    }

    private suspend fun openInstalled() {
        check(!JournalRecoveryGate.callerIsMainThread()) {
            "Database open must leave the main thread"
        }
        val context = appContext ?: error("DatabaseStartup.start was not called")
        val auth = authStore ?: error("DatabaseStartup.start was not called")
        JournalRecoveryGate.await()
        auth.awaitReady()
        val userId = auth.currentUserIdOrNull()
        if (userId == null && !BuildConfig.LOCAL_ONLY_MODE) return
        val id = userId ?: AccountIds.LOCAL_DEV
        val db = AppDatabase.getInstance(context, id)
        db.openHelper.writableDatabase
    }

    private fun skipRealOpen(): Boolean =
        openForTests == null && allowsMainThreadInTests()

    private fun bypassAwait(): Boolean =
        !started.get() && allowsMainThreadInTests() && openForTests == null

    private fun allowsMainThreadInTests(): Boolean =
        DatabasePassphraseStore.isRobolectric() && !JournalRecoveryGate.enforceForTests
}

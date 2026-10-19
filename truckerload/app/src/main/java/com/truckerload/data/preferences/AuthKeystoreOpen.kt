package com.truckerload.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Cold-start encrypted-prefs open. The first attempt runs on its own thread.
 * If Keystore does not return within [timeoutMsForTests], the caller (already
 * on IO) runs the historical synchronous [SecurePreferences.open] and keeps
 * that snapshot. A timed-out attempt must not replace a session already applied.
 */
internal object AuthKeystoreOpen {
    const val DEFAULT_TIMEOUT_MS = 8_000L

    @Volatile
    var timeoutMsForTests: Long = DEFAULT_TIMEOUT_MS

    /** Test hook: attempt 1 is the async open, attempt 2 is the synchronous fallback. */
    @Volatile
    var beforeOpenForTests: ((attempt: Int) -> Unit)? = null

    @Volatile
    var fallbackUsedForTests: Boolean = false

    private var worker: Thread? = null

    fun resetForTests() {
        timeoutMsForTests = DEFAULT_TIMEOUT_MS
        beforeOpenForTests = null
        fallbackUsedForTests = false
        val running = worker
        worker = null
        running?.join(2_000)
    }

    fun openOnce(context: Context): SharedPreferences {
        AuthStore.prefsRef?.let { return it }
        synchronized(AuthStore.prefsLock) {
            AuthStore.prefsRef?.let { return it }
            val opened = readSecurePrefs(context)
            if (AuthStore.prefsRef == null) {
                AuthStore.prefsRef = opened
            }
            flushPendingHealth(AuthStore.prefsRef ?: opened)
            return AuthStore.prefsRef ?: opened
        }
    }

    /**
     * Same read as [openOnce], without waiting on a Keystore call that already
     * holds [AuthStore.prefsLock].
     */
    fun openSynchronously(context: Context): SharedPreferences {
        AuthStore.prefsRef?.let { return it }
        val opened = readSecurePrefs(context)
        if (AuthStore.prefsRef == null) {
            AuthStore.prefsRef = opened
        }
        val chosen = AuthStore.prefsRef ?: opened
        flushPendingHealth(chosen)
        return chosen
    }

    fun openWithFallback(context: Context): SharedPreferences {
        val result = AtomicReference<SharedPreferences?>(null)
        val failure = AtomicReference<Throwable?>(null)
        val done = CountDownLatch(1)
        val worker = Thread({
            try {
                beforeOpenForTests?.invoke(ASYNC_ATTEMPT)
                result.set(openOnce(context))
            } catch (error: Throwable) {
                failure.set(error)
            } finally {
                done.countDown()
            }
        }, "auth-keystore-open")
        worker.isDaemon = true
        this.worker = worker
        worker.start()
        if (done.await(timeoutMsForTests, TimeUnit.MILLISECONDS)) {
            failure.get()?.let { throw it }
            return result.get() ?: openSynchronously(context)
        }
        Log.w(
            TAG,
            "Encrypted prefs open exceeded ${timeoutMsForTests}ms; " +
                "falling back to synchronous open on IO",
        )
        fallbackUsedForTests = true
        beforeOpenForTests?.invoke(SYNC_ATTEMPT)
        return openSynchronously(context)
    }

    private fun flushPendingHealth(opened: SharedPreferences) {
        if (!AuthStore.pendingHealthWrite) return
        AuthStore.pendingHealthWrite = false
        opened.edit {
            putLong(AuthStore.KEY_LAST_SESSION_CHECK_AT, System.currentTimeMillis())
        }
    }

    private fun readSecurePrefs(context: Context): SharedPreferences {
        val secure = SecurePreferences.open(context, AuthStore.PREFS_NAME)
        SecurePreferences.migratePlainToSecure(
            context = context,
            legacyName = AuthStore.LEGACY_PREFS_NAME,
            securePrefs = secure,
            migrationFlagKey = AuthStore.MIGRATION_FLAG,
        )
        if (secure.getBoolean(AuthStore.KEY_LOGGED_IN, false) &&
            secure.getString(AuthStore.KEY_USER_ID, null).isNullOrBlank()
        ) {
            secure.edit {
                putString(AuthStore.KEY_USER_ID, AccountIds.LOCAL_DEV)
                if (secure.getString(AuthStore.KEY_EMAIL, null).isNullOrBlank()) {
                    putString(AuthStore.KEY_EMAIL, "local@device")
                }
            }
        }
        return secure
    }

    private const val TAG = "AuthStore"
    private const val ASYNC_ATTEMPT = 1
    private const val SYNC_ATTEMPT = 2
}

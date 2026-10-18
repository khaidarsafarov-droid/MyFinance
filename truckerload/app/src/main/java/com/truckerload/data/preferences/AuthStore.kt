package com.truckerload.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.os.Looper
import android.util.Log
import androidx.core.content.edit
import com.truckerload.data.local.DatabasePassphraseStore
import com.truckerload.data.local.JournalRecoveryGate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val KEY_ACCESS_TOKEN = "access_token"
private const val KEY_REFRESH_TOKEN = "refresh_token"
private const val KEY_GOOGLE_SUB = "google_sub"
private const val KEY_GOOGLE_ID_TOKEN = "google_id_token"
private const val KEY_PROVIDER = "auth_provider"
private const val DEFAULT_LOGGED_IN = false

data class AuthSession(
    val userId: String,
    val email: String,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val googleSub: String? = null,
    val provider: AuthProvider = AuthProvider.LOCAL,
)

/**
 * Process-wide auth session. Every `AuthStore(context)` shares the same state so
 * Telegram workers / widgets see the active user even when `rememberMe=false`
 * (in-memory only, cleared on process death).
 */
class AuthStore internal constructor(
    context: Context,
    coldStart: ColdStart,
) {
    private val appContext = context.applicationContext

    init {
        when (coldStart) {
            ColdStart.SYNC -> bootstrapSync(appContext)
            ColdStart.ASYNC -> scheduleBootstrap(appContext)
            ColdStart.SKIP -> lastBootstrapModeForTests = "skip"
        }
    }

    constructor(context: Context) : this(
        context,
        when {
            ignoreBootstrapForTests -> ColdStart.SKIP
            DatabasePassphraseStore.isRobolectric() -> ColdStart.SYNC
            else -> ColdStart.ASYNC
        },
    )

    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    val userId: StateFlow<String?> = _userId.asStateFlow()
    val email: StateFlow<String> = _email.asStateFlow()
    val sessionHealth: StateFlow<AuthSessionHealth> = _sessionHealth.asStateFlow()

    /**
     * False until encrypted prefs have been read and journal recovery has published
     * the result. Callers must not treat a missing user id as signed-out while this is false.
     */
    val sessionKnown: StateFlow<Boolean> = _sessionKnown.asStateFlow()

    /**
     * Disk snapshot, including a login recovery already applied, without waiting for
     * [sessionKnown]. Recovery calls this so it does not deadlock on its own publish.
     */
    internal fun peekedUserIdOrNull(): String? = synchronized(lock) {
        liveUserId?.takeIf { liveLoggedIn && it.isNotBlank() }
    }

    fun currentUserIdOrNull(): String? {
        if (!isSessionKnown()) {
            // Main thread stays non-blocking. Unknown is not signed-out.
            if (isMainThread()) return null
            readyLatch().await()
        }
        return peekedUserIdOrNull()
    }

    /** Suspend until cold start has published. Does not block the main thread. */
    suspend fun awaitReady() {
        if (_sessionKnown.value) return
        _sessionKnown.first { it }
    }

    internal suspend fun awaitDiskSnapshot() {
        if (diskReady().isCompleted) return
        diskReady().await()
    }

    /** Bounded wait used by a main-thread database open. Does not change the session. */
    internal fun awaitReadyBlocking(timeoutMs: Long): Boolean {
        if (_sessionKnown.value) return true
        val latch = readyLatch()
        if (timeoutMs > 0L) {
            latch.await(timeoutMs, TimeUnit.MILLISECONDS)
        } else {
            latch.await()
        }
        return _sessionKnown.value
    }

    internal fun publishColdStart() {
        synchronized(lock) { publishColdStartLocked() }
    }

    fun requireUserId(): String =
        currentUserIdOrNull() ?: error("No active user session")

    fun sessionOrNull(): AuthSession? = synchronized(lock) {
        val id = liveUserId?.takeIf { it.isNotBlank() } ?: return null
        if (!liveLoggedIn) return null
        AuthSession(
            userId = id,
            email = liveEmail,
            accessToken = liveAccessToken,
            refreshToken = liveRefreshToken,
            googleSub = liveGoogleSub,
            provider = liveProvider,
        )
    }

    fun accessTokenOrNull(): String? = synchronized(lock) { liveAccessToken }

    /**
     * Google Sign-In ID token (RS256). Distinct from [accessTokenOrNull]
     * which may be a Supabase HS256 JWT.
     */
    fun googleIdTokenOrNull(): String? = synchronized(lock) { liveGoogleIdToken }

    fun authProvider(): AuthProvider = synchronized(lock) { liveProvider }

    fun googleSubOrNull(): String? = synchronized(lock) { liveGoogleSub }

    fun markSessionHealth(health: AuthSessionHealth) {
        synchronized(lock) {
            liveSessionHealth = health
            if (isSessionKnown()) _sessionHealth.value = health
            val target = prefsRef
            if (target == null) {
                pendingHealthWrite = true
                return
            }
            // apply(), not commit(): this runs from silent re-auth and must not block.
            target.edit {
                putLong(KEY_LAST_SESSION_CHECK_AT, System.currentTimeMillis())
            }
        }
    }

    fun setGoogleSub(sub: String?) {
        synchronized(lock) {
            liveGoogleSub = sub?.takeIf { it.isNotBlank() }
            if (!liveGoogleSub.isNullOrBlank()) {
                liveProvider = AuthProvider.GOOGLE
            }
            openPrefsOnce(appContext).edit(commit = true) {
                if (liveGoogleSub == null) remove(KEY_GOOGLE_SUB)
                else putString(KEY_GOOGLE_SUB, liveGoogleSub)
                putString(KEY_PROVIDER, liveProvider.name)
            }
        }
    }

    fun updateTokens(accessToken: String?, refreshToken: String?) {
        val writeSecrets = (!accessToken.isNullOrBlank() || !refreshToken.isNullOrBlank()) &&
            !SecurePreferences.plaintextFallbackUsed
        synchronized(lock) {
            liveAccessToken = accessToken?.takeIf { it.isNotBlank() }
            liveRefreshToken = refreshToken?.takeIf { it.isNotBlank() }
            if (!writeSecrets) {
                // Keep tokens in memory for this process; do not wipe a prior encrypted disk copy
                // when secure storage is temporarily unavailable.
                return
            }
            openPrefsOnce(appContext).edit(commit = true) {
                if (liveAccessToken == null) remove(KEY_ACCESS_TOKEN)
                else putString(KEY_ACCESS_TOKEN, liveAccessToken)
                if (liveRefreshToken == null) remove(KEY_REFRESH_TOKEN)
                else putString(KEY_REFRESH_TOKEN, liveRefreshToken)
            }
        }
    }

    /**
     * Opens the device profile. The app keeps exactly one profile per device: while a profile
     * is active, a login with another id keeps the active id, so the Room file and every
     * account-scoped preference namespace stay the same. Only [logout] (full data erase)
     * releases the id.
     *
     * Google and email sessions always survive app restarts (normal app behavior).
     * Only [AuthProvider.LOCAL] / ephemeral debug sessions can skip disk when [rememberMe] is false.
     */
    fun login(
        userId: String,
        email: String,
        rememberMe: Boolean = true,
        accessToken: String? = null,
        refreshToken: String? = null,
        googleSub: String? = null,
        provider: AuthProvider = AuthProvider.LOCAL,
        googleIdToken: String? = null,
    ) {
        val requestedId = if (!googleSub.isNullOrBlank()) {
            AccountIds.fromGoogleSub(googleSub)
        } else {
            userId.trim()
        }
        require(requestedId.isNotBlank()) { "userId required" }
        val id = peekedUserIdOrNull() ?: requestedId
        val mail = email.trim()
        val resolvedProvider = when {
            !googleSub.isNullOrBlank() -> AuthProvider.GOOGLE
            provider == AuthProvider.GOOGLE || provider == AuthProvider.EMAIL -> provider
            id == AccountIds.LOCAL_DEV -> AuthProvider.LOCAL
            id.startsWith("local_") -> AuthProvider.EMAIL
            id.startsWith("google_") -> AuthProvider.GOOGLE
            // Supabase UUID (or any non-guest id) is a real account, not guest LOCAL.
            else -> AuthProvider.EMAIL
        }
        // Real auth (Google / email) always persists identity across cold starts.
        val isPersistentAuth = resolvedProvider == AuthProvider.GOOGLE ||
            resolvedProvider == AuthProvider.EMAIL ||
            !googleSub.isNullOrBlank()
        val persistSession = rememberMe || isPersistentAuth
        val canPersistSecrets = persistSession &&
            (!accessToken.isNullOrBlank() || !refreshToken.isNullOrBlank() || !googleIdToken.isNullOrBlank()) &&
            !SecurePreferences.plaintextFallbackUsed
        synchronized(lock) {
            sessionMutated = true
            val nextGoogleIdToken = when {
                !googleIdToken.isNullOrBlank() -> googleIdToken.trim()
                googleIdToken != null -> null
                id != liveUserId -> null
                else -> liveGoogleIdToken
            }
            liveUserId = id
            liveEmail = mail
            liveAccessToken = accessToken?.takeIf { it.isNotBlank() }
            liveRefreshToken = refreshToken?.takeIf { it.isNotBlank() }
            liveGoogleSub = googleSub?.takeIf { it.isNotBlank() }
            liveGoogleIdToken = nextGoogleIdToken
            liveProvider = resolvedProvider
            liveLoggedIn = true
            liveSessionHealth = if (canPersistSecrets || accessToken.isNullOrBlank()) {
                AuthSessionHealth.VERIFIED
            } else {
                AuthSessionHealth.SESSION_UNCONFIRMED
            }
            _userId.value = id
            _email.value = mail
            _isLoggedIn.value = true
            _sessionHealth.value = liveSessionHealth
            if (persistSession) {
                openPrefsOnce(appContext).edit(commit = true) {
                    putBoolean(KEY_LOGGED_IN, true)
                    putString(KEY_USER_ID, id)
                    putString(KEY_EMAIL, mail)
                    putString(KEY_PROVIDER, resolvedProvider.name)
                    if (!canPersistSecrets || accessToken.isNullOrBlank()) remove(KEY_ACCESS_TOKEN)
                    else putString(KEY_ACCESS_TOKEN, accessToken)
                    if (!canPersistSecrets || refreshToken.isNullOrBlank()) remove(KEY_REFRESH_TOKEN)
                    else putString(KEY_REFRESH_TOKEN, refreshToken)
                    if (googleSub.isNullOrBlank()) remove(KEY_GOOGLE_SUB)
                    else putString(KEY_GOOGLE_SUB, googleSub)
                    when {
                        nextGoogleIdToken.isNullOrBlank() -> remove(KEY_GOOGLE_ID_TOKEN)
                        SecurePreferences.plaintextFallbackUsed -> { /* keep prior encrypted copy */ }
                        else -> putString(KEY_GOOGLE_ID_TOKEN, nextGoogleIdToken)
                    }
                }
            } else {
                // Ephemeral local-only session: memory for this process, cleared on next cold start.
                openPrefsOnce(appContext).edit(commit = true) {
                    remove(KEY_LOGGED_IN)
                    remove(KEY_USER_ID)
                    remove(KEY_EMAIL)
                    remove(KEY_ACCESS_TOKEN)
                    remove(KEY_REFRESH_TOKEN)
                    remove(KEY_GOOGLE_SUB)
                    remove(KEY_GOOGLE_ID_TOKEN)
                    remove(KEY_PROVIDER)
                }
            }
        }
    }

    /** @deprecated Use [login] with userId. */
    fun login(rememberMe: Boolean = true) {
        val existing = currentUserIdOrNull()
        if (existing != null) {
            login(existing, email.value, rememberMe)
            return
        }
        val prefs = openPrefsOnce(appContext)
        if (rememberMe && prefs.getBoolean(KEY_LOGGED_IN, false)) {
            login(
                AccountIds.LOCAL_DEV,
                prefs.getString(KEY_EMAIL, null).orEmpty().ifBlank { "local@device" },
                true,
            )
            return
        }
        error("login(userId, email) required to open the device profile")
    }

    fun logout() {
        synchronized(lock) {
            sessionMutated = true
            liveLoggedIn = false
            liveUserId = null
            liveEmail = ""
            liveAccessToken = null
            liveRefreshToken = null
            liveGoogleSub = null
            liveGoogleIdToken = null
            liveProvider = AuthProvider.LOCAL
            liveSessionHealth = AuthSessionHealth.VERIFIED
            openPrefsOnce(appContext).edit(commit = true) {
                remove(KEY_LOGGED_IN)
                remove(KEY_USER_ID)
                remove(KEY_EMAIL)
                remove(KEY_ACCESS_TOKEN)
                remove(KEY_REFRESH_TOKEN)
                remove(KEY_GOOGLE_SUB)
                remove(KEY_GOOGLE_ID_TOKEN)
                remove(KEY_PROVIDER)
            }
            _isLoggedIn.value = false
            _userId.value = null
            _email.value = ""
            _sessionHealth.value = AuthSessionHealth.VERIFIED
        }
    }

    fun setLoggedIn(value: Boolean) {
        if (!value) {
            logout()
            return
        }
        val id = currentUserIdOrNull() ?: return
        login(
            userId = id,
            email = email.value,
            rememberMe = true,
            accessToken = liveAccessToken,
            refreshToken = liveRefreshToken,
            googleSub = liveGoogleSub,
            provider = liveProvider,
            googleIdToken = liveGoogleIdToken,
        )
    }

    enum class ColdStart { SYNC, ASYNC, SKIP }

    companion object {
        private const val TAG = "AuthStore"
        internal const val PREFS_NAME = "truckerload_auth_enc"
        internal const val LEGACY_PREFS_NAME = "truckerload_auth"
        internal const val MIGRATION_FLAG = "migrated_from_plain"
        internal const val KEY_LOGGED_IN = "is_logged_in"
        internal const val KEY_USER_ID = "user_id"
        internal const val KEY_EMAIL = "email"
        internal const val KEY_LAST_SESSION_CHECK_AT = "last_session_check_at"
        private val lock = Any()
        internal val prefsLock = Any()
        private val bootstrapScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private var bootstrapJob: Job? = null
        private var bootstrapGeneration = 0
        private var bootstrapped = false
        private var sessionKnown = false
        private var sessionMutated = false
        @Volatile internal var pendingHealthWrite = false
        @Volatile internal var prefsRef: SharedPreferences? = null
        private var readyLatch = CountDownLatch(1)
        private var diskReadyDeferred = CompletableDeferred<Unit>()
        private var liveLoggedIn = false
        private var liveUserId: String? = null
        private var liveEmail: String = ""
        private var liveAccessToken: String? = null
        private var liveRefreshToken: String? = null
        private var liveGoogleSub: String? = null
        private var liveGoogleIdToken: String? = null
        private var liveProvider: AuthProvider = AuthProvider.LOCAL
        private var liveSessionHealth: AuthSessionHealth = AuthSessionHealth.VERIFIED

        private val _isLoggedIn = MutableStateFlow(DEFAULT_LOGGED_IN)
        private val _userId = MutableStateFlow<String?>(null)
        private val _email = MutableStateFlow("")
        private val _sessionHealth = MutableStateFlow(AuthSessionHealth.VERIFIED)
        private val _sessionKnown = MutableStateFlow(false)

        /** Pauses the async snapshot so tests can login before disk is applied. */
        internal var beforeApplyForTests: (suspend () -> Unit)? = null

        /** Test-only dispatcher for the cold-start read. Null uses [Dispatchers.IO]. */
        internal var bootstrapDispatcherOverride: kotlinx.coroutines.CoroutineDispatcher? = null

        internal var lastBootstrapModeForTests: String = ""

        /**
         * While true, the public constructor does not read prefs. Tests use this so a
         * background `AuthStore(context)` cannot publish over a cold-start case.
         */
        internal var ignoreBootstrapForTests: Boolean = false

        private fun readyLatch(): CountDownLatch = readyLatch

        private fun diskReady(): CompletableDeferred<Unit> = diskReadyDeferred

        private fun isSessionKnown(): Boolean = synchronized(lock) { sessionKnown }

        private fun isMainThread(): Boolean {
            val main = Looper.getMainLooper() ?: return false
            return Looper.myLooper() == main
        }

        private fun bootstrapSync(context: Context) {
            lastBootstrapModeForTests = "sync"
            val generation = synchronized(lock) {
                if (sessionKnown) return
                bootstrapGeneration
            }
            val prefs = openPrefsOnce(context)
            synchronized(lock) {
                if (generation != bootstrapGeneration || ignoreBootstrapForTests) return
                if (!sessionMutated) applyDiskLocked(prefs)
                publishColdStartLocked()
            }
        }

        private fun scheduleBootstrap(context: Context) {
            synchronized(lock) {
                if (sessionKnown || bootstrapJob?.isActive == true) {
                    lastBootstrapModeForTests = "async-skip"
                    return
                }
                liveSessionHealth = AuthSessionHealth.UNKNOWN
                _sessionHealth.value = AuthSessionHealth.UNKNOWN
                _sessionKnown.value = false
                sessionKnown = false
            }
            val generation = bootstrapGeneration
            lastBootstrapModeForTests = "async"
            bootstrapJob = bootstrapScope.launch(bootstrapDispatcher()) {
                runCatching { AuthKeystoreOpen.openWithFallback(context) }
                    .onSuccess { prefs ->
                        beforeApplyForTests?.invoke()
                        synchronized(lock) {
                            if (generation != bootstrapGeneration) return@onSuccess
                            if (!sessionMutated) applyDiskLocked(prefs)
                        }
                    }
                    .onFailure { error -> Log.e(TAG, "Session bootstrap failed", error) }
                synchronized(lock) {
                    if (generation != bootstrapGeneration) return@launch
                    if (!diskReadyDeferred.isCompleted) diskReadyDeferred.complete(Unit)
                }
                if (!JournalRecoveryGate.isArmed()) {
                    synchronized(lock) {
                        if (generation == bootstrapGeneration) publishColdStartLocked()
                    }
                }
            }
        }

        private fun bootstrapDispatcher(): kotlinx.coroutines.CoroutineDispatcher =
            bootstrapDispatcherOverride ?: Dispatchers.IO

        private fun applyDiskLocked(prefs: SharedPreferences) {
            val persistedId = prefs.getString(KEY_USER_ID, null)?.takeIf { it.isNotBlank() }
            if (prefs.getBoolean(KEY_LOGGED_IN, DEFAULT_LOGGED_IN) && persistedId != null) {
                liveUserId = persistedId
                liveEmail = prefs.getString(KEY_EMAIL, null).orEmpty()
                liveAccessToken = prefs.getString(KEY_ACCESS_TOKEN, null)?.takeIf { it.isNotBlank() }
                liveRefreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)?.takeIf { it.isNotBlank() }
                liveGoogleSub = prefs.getString(KEY_GOOGLE_SUB, null)?.takeIf { it.isNotBlank() }
                liveGoogleIdToken = prefs.getString(KEY_GOOGLE_ID_TOKEN, null)?.takeIf { it.isNotBlank() }
                liveProvider = prefs.getString(KEY_PROVIDER, null)?.let {
                    runCatching { AuthProvider.valueOf(it) }.getOrNull()
                } ?: when {
                    !liveGoogleSub.isNullOrBlank() -> AuthProvider.GOOGLE
                    persistedId.startsWith("google_") -> AuthProvider.GOOGLE
                    persistedId == AccountIds.LOCAL_DEV -> AuthProvider.LOCAL
                    // local_<hash> email accounts and cloud UUIDs are real logins —
                    // never treat a restored UUID as guest LOCAL (that force-logs out).
                    else -> AuthProvider.EMAIL
                }
                liveLoggedIn = true
                liveSessionHealth = AuthSessionHealth.VERIFIED
            } else {
                liveLoggedIn = false
                liveUserId = null
                liveEmail = ""
                liveAccessToken = null
                liveRefreshToken = null
                liveGoogleSub = null
                liveGoogleIdToken = null
                liveProvider = AuthProvider.LOCAL
                liveSessionHealth = AuthSessionHealth.VERIFIED
            }
        }

        private fun publishColdStartLocked() {
            if (sessionKnown) return
            sessionKnown = true
            bootstrapped = true
            _isLoggedIn.value = liveLoggedIn
            _userId.value = liveUserId
            _email.value = liveEmail
            _sessionHealth.value = liveSessionHealth
            _sessionKnown.value = true
            if (!diskReadyDeferred.isCompleted) diskReadyDeferred.complete(Unit)
            readyLatch.countDown()
        }

        /** Test-only: clear process-wide session so the next [AuthStore] re-reads disk. */
        internal fun resetForTests() {
            AuthKeystoreOpen.resetForTests()
            synchronized(lock) {
                bootstrapGeneration++
                bootstrapJob?.cancel()
                bootstrapJob = null
                beforeApplyForTests = null
                bootstrapDispatcherOverride = null
                bootstrapped = false
                sessionKnown = false
                sessionMutated = false
                pendingHealthWrite = false
                prefsRef = null
                liveLoggedIn = false
                liveUserId = null
                liveEmail = ""
                liveAccessToken = null
                liveRefreshToken = null
                liveGoogleSub = null
                liveGoogleIdToken = null
                liveProvider = AuthProvider.LOCAL
                liveSessionHealth = AuthSessionHealth.VERIFIED
                readyLatch.countDown()
                if (!diskReadyDeferred.isCompleted) diskReadyDeferred.complete(Unit)
                readyLatch = CountDownLatch(1)
                diskReadyDeferred = CompletableDeferred()
                _isLoggedIn.value = DEFAULT_LOGGED_IN
                _userId.value = null
                _email.value = ""
                _sessionHealth.value = AuthSessionHealth.VERIFIED
                _sessionKnown.value = false
            }
        }

        private fun openPrefsOnce(context: Context): SharedPreferences =
            AuthKeystoreOpen.openOnce(context)
    }
}

package com.truckerload.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.truckerload.data.local.GoogleAccountUnifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PREFS_NAME = "truckerload_auth_enc"
private const val LEGACY_PREFS_NAME = "truckerload_auth"
private const val MIGRATION_FLAG = "migrated_from_plain"
private const val KEY_LOGGED_IN = "is_logged_in"
private const val KEY_USER_ID = "user_id"
private const val KEY_EMAIL = "email"
private const val KEY_ACCESS_TOKEN = "access_token"
private const val KEY_REFRESH_TOKEN = "refresh_token"
private const val KEY_GOOGLE_SUB = "google_sub"
private const val KEY_GOOGLE_ID_TOKEN = "google_id_token"
private const val KEY_PROVIDER = "auth_provider"
private const val KEY_LAST_SESSION_CHECK_AT = "last_session_check_at"
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
class AuthStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = openPrefs(appContext)

    init {
        synchronized(lock) {
            if (!bootstrapped) {
                bootstrapped = true
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
                    val canonical = GoogleAccountUnifier.canonicalSessionUserId(
                        appContext,
                        persistedId,
                        liveGoogleSub,
                    )
                    if (canonical != persistedId) {
                        liveUserId = canonical
                        prefs.edit(commit = true) { putString(KEY_USER_ID, canonical) }
                    }
                }
                _isLoggedIn.value = liveLoggedIn
                _userId.value = liveUserId
                _email.value = liveEmail
                _sessionHealth.value = liveSessionHealth
            }
        }
    }

    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    val userId: StateFlow<String?> = _userId.asStateFlow()
    val email: StateFlow<String> = _email.asStateFlow()
    val sessionHealth: StateFlow<AuthSessionHealth> = _sessionHealth.asStateFlow()

    fun currentUserIdOrNull(): String? = synchronized(lock) {
        liveUserId?.takeIf { liveLoggedIn && it.isNotBlank() }
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

    /**
     * Copies a leftover Supabase-UUID journal/prefs into the canonical Google id
     * before [UserProfileStore] binds. No-op when [googleSub] is blank.
     */
    fun unifyGoogleJournal(googleSub: String?, aliasUserIds: Collection<String> = emptyList()) {
        val sub = googleSub?.trim().orEmpty()
        if (sub.isBlank()) return
        GoogleAccountUnifier.relocateAliases(
            appContext,
            AccountIds.fromGoogleSub(sub),
            aliasUserIds,
        )
    }

    fun markSessionHealth(health: AuthSessionHealth) {
        synchronized(lock) {
            liveSessionHealth = health
            _sessionHealth.value = health
            prefs.edit(commit = true) {
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
            prefs.edit(commit = true) {
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
            prefs.edit(commit = true) {
                if (liveAccessToken == null) remove(KEY_ACCESS_TOKEN)
                else putString(KEY_ACCESS_TOKEN, liveAccessToken)
                if (liveRefreshToken == null) remove(KEY_REFRESH_TOKEN)
                else putString(KEY_REFRESH_TOKEN, liveRefreshToken)
            }
        }
    }

    /**
     * Starts a multi-user session. Each [userId] gets its own Room DB and preference namespaces.
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
        aliasUserIds: List<String> = emptyList(),
    ) {
        val requestedId = userId.trim()
        val id = if (!googleSub.isNullOrBlank()) {
            AccountIds.fromGoogleSub(googleSub)
        } else {
            requestedId
        }
        require(id.isNotBlank()) { "userId required" }
        val mail = email.trim()
        val previousId: String?
        val previousSub: String?
        synchronized(lock) {
            previousId = liveUserId
            previousSub = liveGoogleSub
        }
        if (!googleSub.isNullOrBlank()) {
            val aliases = buildList {
                addAll(aliasUserIds)
                if (requestedId.isNotBlank()) add(requestedId)
                if (!previousId.isNullOrBlank() &&
                    previousId != id &&
                    previousSub == googleSub.trim()
                ) {
                    add(previousId)
                }
            }
            GoogleAccountUnifier.relocateAliases(appContext, id, aliases)
        }
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
                prefs.edit(commit = true) {
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
                prefs.edit(commit = true) {
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
        if (rememberMe && prefs.getBoolean(KEY_LOGGED_IN, false)) {
            login(
                AccountIds.LOCAL_DEV,
                prefs.getString(KEY_EMAIL, null).orEmpty().ifBlank { "local@device" },
                true,
            )
            return
        }
        error("login(userId, email) required for multi-user sessions")
    }

    fun logout() {
        synchronized(lock) {
            liveLoggedIn = false
            liveUserId = null
            liveEmail = ""
            liveAccessToken = null
            liveRefreshToken = null
            liveGoogleSub = null
            liveGoogleIdToken = null
            liveProvider = AuthProvider.LOCAL
            liveSessionHealth = AuthSessionHealth.VERIFIED
            prefs.edit(commit = true) {
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

    companion object {
        private val lock = Any()
        private var bootstrapped = false
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

        /** Test-only: clear process-wide session so the next [AuthStore] re-reads disk. */
        internal fun resetForTests() {
            synchronized(lock) {
                bootstrapped = false
                liveLoggedIn = false
                liveUserId = null
                liveEmail = ""
                liveAccessToken = null
                liveRefreshToken = null
                liveGoogleSub = null
                liveGoogleIdToken = null
                liveProvider = AuthProvider.LOCAL
                liveSessionHealth = AuthSessionHealth.VERIFIED
                _isLoggedIn.value = DEFAULT_LOGGED_IN
                _userId.value = null
                _email.value = ""
                _sessionHealth.value = AuthSessionHealth.VERIFIED
            }
        }

        private fun openPrefs(context: Context): SharedPreferences {
            val secure = SecurePreferences.open(context, PREFS_NAME)
            // Migrate even on Keystore fallback so older installs keep their session.
            SecurePreferences.migratePlainToSecure(
                context = context,
                legacyName = LEGACY_PREFS_NAME,
                securePrefs = secure,
                migrationFlagKey = MIGRATION_FLAG,
            )
            if (secure.getBoolean(KEY_LOGGED_IN, false) &&
                secure.getString(KEY_USER_ID, null).isNullOrBlank()
            ) {
                secure.edit {
                    putString(KEY_USER_ID, AccountIds.LOCAL_DEV)
                    if (secure.getString(KEY_EMAIL, null).isNullOrBlank()) {
                        putString(KEY_EMAIL, "local@device")
                    }
                }
            }
            return secure
        }
    }
}

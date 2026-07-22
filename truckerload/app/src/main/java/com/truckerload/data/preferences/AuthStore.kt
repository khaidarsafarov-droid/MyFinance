package com.truckerload.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
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
private const val DEFAULT_LOGGED_IN = false

data class AuthSession(
    val userId: String,
    val email: String,
    val accessToken: String? = null,
    val refreshToken: String? = null,
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
                    liveLoggedIn = true
                }
                _isLoggedIn.value = liveLoggedIn
                _userId.value = liveUserId
                _email.value = liveEmail
            }
        }
    }

    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    val userId: StateFlow<String?> = _userId.asStateFlow()
    val email: StateFlow<String> = _email.asStateFlow()

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
        )
    }

    fun accessTokenOrNull(): String? = synchronized(lock) { liveAccessToken }

    /**
     * Starts a multi-user session. Each [userId] gets its own Room DB and preference namespaces.
     */
    fun login(
        userId: String,
        email: String,
        rememberMe: Boolean = true,
        accessToken: String? = null,
        refreshToken: String? = null,
    ) {
        val id = userId.trim()
        require(id.isNotBlank()) { "userId required" }
        val mail = email.trim()
        synchronized(lock) {
            liveUserId = id
            liveEmail = mail
            liveAccessToken = accessToken?.takeIf { it.isNotBlank() }
            liveRefreshToken = refreshToken?.takeIf { it.isNotBlank() }
            liveLoggedIn = true
            _userId.value = id
            _email.value = mail
            _isLoggedIn.value = true
            if (rememberMe) {
                prefs.edit {
                    putBoolean(KEY_LOGGED_IN, true)
                    putString(KEY_USER_ID, id)
                    putString(KEY_EMAIL, mail)
                    if (accessToken.isNullOrBlank()) remove(KEY_ACCESS_TOKEN)
                    else putString(KEY_ACCESS_TOKEN, accessToken)
                    if (refreshToken.isNullOrBlank()) remove(KEY_REFRESH_TOKEN)
                    else putString(KEY_REFRESH_TOKEN, refreshToken)
                }
            } else {
                // Ephemeral session: keep process-wide memory, clear disk so next cold start logs out.
                prefs.edit {
                    remove(KEY_LOGGED_IN)
                    remove(KEY_USER_ID)
                    remove(KEY_EMAIL)
                    remove(KEY_ACCESS_TOKEN)
                    remove(KEY_REFRESH_TOKEN)
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
            prefs.edit {
                remove(KEY_LOGGED_IN)
                remove(KEY_USER_ID)
                remove(KEY_EMAIL)
                remove(KEY_ACCESS_TOKEN)
                remove(KEY_REFRESH_TOKEN)
            }
            _isLoggedIn.value = false
            _userId.value = null
            _email.value = ""
        }
    }

    fun setLoggedIn(value: Boolean) {
        if (!value) {
            logout()
            return
        }
        val id = currentUserIdOrNull() ?: return
        login(id, email.value, rememberMe = true)
    }

    companion object {
        private val lock = Any()
        private var bootstrapped = false
        private var liveLoggedIn = false
        private var liveUserId: String? = null
        private var liveEmail: String = ""
        private var liveAccessToken: String? = null
        private var liveRefreshToken: String? = null

        private val _isLoggedIn = MutableStateFlow(DEFAULT_LOGGED_IN)
        private val _userId = MutableStateFlow<String?>(null)
        private val _email = MutableStateFlow("")

        private fun openPrefs(context: Context): SharedPreferences {
            val secure = SecurePreferences.open(context, PREFS_NAME)
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

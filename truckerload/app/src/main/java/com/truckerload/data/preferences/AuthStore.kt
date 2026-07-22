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

class AuthStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = openPrefs(appContext)

    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean(KEY_LOGGED_IN, DEFAULT_LOGGED_IN))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userId = MutableStateFlow(prefs.getString(KEY_USER_ID, null)?.takeIf { it.isNotBlank() })
    val userId: StateFlow<String?> = _userId.asStateFlow()

    private val _email = MutableStateFlow(prefs.getString(KEY_EMAIL, null).orEmpty())
    val email: StateFlow<String> = _email.asStateFlow()

    fun currentUserIdOrNull(): String? = _userId.value?.takeIf { it.isNotBlank() }

    fun requireUserId(): String =
        currentUserIdOrNull() ?: error("No active user session")

    fun sessionOrNull(): AuthSession? {
        val id = currentUserIdOrNull() ?: return null
        if (!_isLoggedIn.value) return null
        return AuthSession(
            userId = id,
            email = _email.value,
            accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)?.takeIf { it.isNotBlank() },
            refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)?.takeIf { it.isNotBlank() },
        )
    }

    fun accessTokenOrNull(): String? =
        prefs.getString(KEY_ACCESS_TOKEN, null)?.takeIf { it.isNotBlank() }

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
        _userId.value = id
        _email.value = email.trim()
        _isLoggedIn.value = true
        if (rememberMe) {
            prefs.edit {
                putBoolean(KEY_LOGGED_IN, true)
                putString(KEY_USER_ID, id)
                putString(KEY_EMAIL, email.trim())
                if (accessToken.isNullOrBlank()) remove(KEY_ACCESS_TOKEN)
                else putString(KEY_ACCESS_TOKEN, accessToken)
                if (refreshToken.isNullOrBlank()) remove(KEY_REFRESH_TOKEN)
                else putString(KEY_REFRESH_TOKEN, refreshToken)
            }
        } else {
            // In-memory session only — clear persisted identity.
            prefs.edit {
                remove(KEY_LOGGED_IN)
                remove(KEY_USER_ID)
                remove(KEY_EMAIL)
                remove(KEY_ACCESS_TOKEN)
                remove(KEY_REFRESH_TOKEN)
            }
        }
    }

    /** @deprecated Use [login] with userId. Kept so accidental call sites fail loudly in debug. */
    fun login(rememberMe: Boolean = true) {
        val existing = currentUserIdOrNull()
        if (existing != null) {
            login(existing, _email.value, rememberMe)
            return
        }
        if (rememberMe && prefs.getBoolean(KEY_LOGGED_IN, false)) {
            // Legacy: logged-in flag without userId — upgrade to local_dev so existing DB migrates.
            login(AccountIds.LOCAL_DEV, prefs.getString(KEY_EMAIL, null).orEmpty().ifBlank { "local@device" }, true)
            return
        }
        error("login(userId, email) required for multi-user sessions")
    }

    fun logout() {
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

    fun setLoggedIn(value: Boolean) {
        if (!value) {
            logout()
            return
        }
        val id = currentUserIdOrNull() ?: return
        login(id, _email.value, rememberMe = true)
    }

    companion object {
        private fun openPrefs(context: Context): SharedPreferences {
            val secure = SecurePreferences.open(context, PREFS_NAME)
            SecurePreferences.migratePlainToSecure(
                context = context,
                legacyName = LEGACY_PREFS_NAME,
                securePrefs = secure,
                migrationFlagKey = MIGRATION_FLAG,
            )
            // Upgrade legacy "logged in with no userId" sessions.
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

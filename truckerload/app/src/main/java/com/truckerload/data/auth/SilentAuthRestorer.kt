package com.truckerload.data.auth

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.truckerload.BuildConfig
import com.truckerload.data.preferences.AuthProvider
import com.truckerload.data.preferences.AuthSessionHealth
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.remote.SupabaseAuthService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Cold-start silent session check (guide Part 2).
 * Never blocks the UI; only updates [AuthStore.sessionHealth].
 *
 * Important: Google cold-start must NOT call Credential Manager. Even
 * `silent=true` / auto-select can show the Google account sheet on every launch.
 * Prefer refreshing the stored Supabase session; keep local identity if refresh fails.
 */
object SilentAuthRestorer {

    private const val TAG = "SilentAuthRestorer"

    @Suppress("UNUSED_PARAMETER")
    suspend fun restore(
        context: Context,
        authStore: AuthStore,
        userProfileStore: UserProfileStore,
    ): AuthSessionHealth = withContext(Dispatchers.IO) {
        if (BuildConfig.LOCAL_ONLY_MODE) {
            authStore.markSessionHealth(AuthSessionHealth.VERIFIED)
            return@withContext AuthSessionHealth.VERIFIED
        }
        val session = authStore.sessionOrNull()
        if (session == null) {
            return@withContext AuthSessionHealth.VERIFIED
        }
        if (!hasNetwork(context)) {
            authStore.markSessionHealth(AuthSessionHealth.OFFLINE_LOCAL)
            return@withContext AuthSessionHealth.OFFLINE_LOCAL
        }
        when (authStore.authProvider()) {
            AuthProvider.GOOGLE -> restoreGoogle(context, authStore)
            AuthProvider.EMAIL -> restoreEmailTokens(context, authStore)
            AuthProvider.LOCAL -> {
                authStore.markSessionHealth(AuthSessionHealth.VERIFIED)
                AuthSessionHealth.VERIFIED
            }
        }
    }

    /**
     * Refresh Supabase JWTs from the stored refresh token.
     * Never launches Credential Manager / Google UI on cold start.
     */
    private suspend fun restoreGoogle(
        context: Context,
        authStore: AuthStore,
    ): AuthSessionHealth {
        val session = authStore.sessionOrNull() ?: return AuthSessionHealth.VERIFIED
        val refresh = session.refreshToken
        val access = session.accessToken
        val supabase = SupabaseAuthService(context.applicationContext)

        if (supabase.isConfigured() && !refresh.isNullOrBlank()) {
            val refreshed = supabase.refreshSession(refresh)
            val tokens = refreshed.getOrNull()
            if (tokens != null) {
                runCatching {
                    authStore.updateTokens(tokens.accessToken, tokens.refreshToken)
                }.onFailure { Log.w(TAG, "Failed to persist refreshed Google tokens", it) }
                authStore.markSessionHealth(AuthSessionHealth.VERIFIED)
                return AuthSessionHealth.VERIFIED
            }
            Log.w(TAG, "Google JWT refresh failed: ${refreshed.exceptionOrNull()?.message}")
            // Keep local Google identity; soft banner. Explicit reconnect uses Login UI.
            authStore.markSessionHealth(AuthSessionHealth.SESSION_UNCONFIRMED)
            return AuthSessionHealth.SESSION_UNCONFIRMED
        }

        if (supabase.isConfigured() && !access.isNullOrBlank() && refresh.isNullOrBlank()) {
            // Access-only session cannot be silently refreshed without Google UI.
            authStore.markSessionHealth(AuthSessionHealth.SESSION_UNCONFIRMED)
            return AuthSessionHealth.SESSION_UNCONFIRMED
        }

        // Local Google profile / no cloud tokens — disk identity is enough.
        authStore.markSessionHealth(AuthSessionHealth.VERIFIED)
        return AuthSessionHealth.VERIFIED
    }

    private suspend fun restoreEmailTokens(
        context: Context,
        authStore: AuthStore,
    ): AuthSessionHealth {
        val session = authStore.sessionOrNull() ?: return AuthSessionHealth.VERIFIED
        val refresh = session.refreshToken
        val access = session.accessToken
        if (access.isNullOrBlank() && refresh.isNullOrBlank()) {
            // Offline-local email account (no Supabase tokens) — treat as OK.
            authStore.markSessionHealth(AuthSessionHealth.VERIFIED)
            return AuthSessionHealth.VERIFIED
        }
        val supabase = SupabaseAuthService(context.applicationContext)
        if (!supabase.isConfigured()) {
            authStore.markSessionHealth(AuthSessionHealth.VERIFIED)
            return AuthSessionHealth.VERIFIED
        }
        if (!refresh.isNullOrBlank()) {
            val refreshed = supabase.refreshSession(refresh)
            val tokens = refreshed.getOrNull()
            if (tokens != null) {
                authStore.updateTokens(tokens.accessToken, tokens.refreshToken)
                authStore.markSessionHealth(AuthSessionHealth.VERIFIED)
                return AuthSessionHealth.VERIFIED
            }
            Log.w(TAG, "Email JWT refresh failed: ${refreshed.exceptionOrNull()?.message}")
            if (!access.isNullOrBlank()) {
                // Keep working with the existing access token; soft banner.
                authStore.markSessionHealth(AuthSessionHealth.SESSION_UNCONFIRMED)
                return AuthSessionHealth.SESSION_UNCONFIRMED
            }
            authStore.markSessionHealth(AuthSessionHealth.SESSION_UNCONFIRMED)
            return AuthSessionHealth.SESSION_UNCONFIRMED
        }
        // Access only — cannot refresh.
        authStore.markSessionHealth(AuthSessionHealth.SESSION_UNCONFIRMED)
        return AuthSessionHealth.SESSION_UNCONFIRMED
    }

    private fun hasNetwork(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

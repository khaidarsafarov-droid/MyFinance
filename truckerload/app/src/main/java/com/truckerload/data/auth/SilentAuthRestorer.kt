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
import com.truckerload.data.remote.CredentialManagerGoogleSignIn
import com.truckerload.data.remote.SupabaseAuthService
import com.truckerload.presentation.auth.decodeGoogleIdToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Cold-start silent session check (guide Part 2).
 * Never blocks the UI; only updates [AuthStore.sessionHealth].
 */
object SilentAuthRestorer {

    private const val TAG = "SilentAuthRestorer"

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
            AuthProvider.GOOGLE -> restoreGoogle(context, authStore, userProfileStore)
            AuthProvider.EMAIL -> restoreEmailTokens(context, authStore)
            AuthProvider.LOCAL -> {
                authStore.markSessionHealth(AuthSessionHealth.VERIFIED)
                AuthSessionHealth.VERIFIED
            }
        }
    }

    private suspend fun restoreGoogle(
        context: Context,
        authStore: AuthStore,
        userProfileStore: UserProfileStore,
    ): AuthSessionHealth {
        if (!CredentialManagerGoogleSignIn.isAvailable()) {
            // Tokens on disk are enough for offline-first identity.
            authStore.markSessionHealth(AuthSessionHealth.VERIFIED)
            return AuthSessionHealth.VERIFIED
        }
        val silent = CredentialManagerGoogleSignIn.getGoogleIdToken(context, silent = true)
        val idToken = silent.getOrNull()
        if (idToken.isNullOrBlank()) {
            Log.i(TAG, "Silent Google re-auth failed: ${silent.exceptionOrNull()?.message}")
            authStore.markSessionHealth(AuthSessionHealth.SESSION_UNCONFIRMED)
            return AuthSessionHealth.SESSION_UNCONFIRMED
        }
        val claims = decodeGoogleIdToken(idToken)
        val sub = claims?.optString("sub")?.takeIf { it.isNotBlank() }
        val email = claims?.optString("email").orEmpty()
        if (!sub.isNullOrBlank()) {
            authStore.setGoogleSub(sub)
            val bound = userProfileStore.boundUserIdOrNull
            if (!bound.isNullOrBlank()) {
                val profile = userProfileStore.profile.value
                if (profile != null && profile.googleId.isNullOrBlank()) {
                    userProfileStore.saveProfile(profile.copy(googleId = sub, email = profile.email.ifBlank { email }))
                }
            }
        }
        val supabase = SupabaseAuthService(context.applicationContext)
        if (supabase.isConfigured()) {
            val refresh = authStore.sessionOrNull()?.refreshToken
            if (!refresh.isNullOrBlank()) {
                // Best-effort: exchange ID token again to refresh access when possible.
                runCatching {
                    val result = supabase.signInWithIdToken(idToken)
                    val signIn = result.getOrNull()
                    if (signIn != null) {
                        authStore.updateTokens(signIn.accessToken, signIn.refreshToken)
                    }
                }.onFailure { Log.w(TAG, "Supabase silent refresh failed", it) }
            }
        }
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

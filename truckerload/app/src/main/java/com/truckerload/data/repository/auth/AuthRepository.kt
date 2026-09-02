package com.truckerload.data.repository.auth

import android.content.Context
import kotlinx.coroutines.flow.Flow

/** Google credential payload (Credential Manager ID token and/or legacy account fields). */
data class GoogleAuthCredential(
    val idToken: String? = null,
    val email: String = "",
    val givenName: String = "",
    val familyName: String = "",
    val photoUrl: String? = null,
    val googleId: String? = null,
)

data class AuthUser(
    val userId: String,
    val email: String,
    val displayName: String = "",
)

sealed interface AuthState {
    data object SignedOut : AuthState
    data class SignedIn(val user: AuthUser) : AuthState
}

data class AuthSignInResult(
    val user: AuthUser,
    val toastMessages: List<String> = emptyList(),
    val biometricEnabled: Boolean = false,
)

sealed interface GoogleTokenRequestResult {
    data class Token(val idToken: String) : GoogleTokenRequestResult
    data object Cancelled : GoogleTokenRequestResult
    data object FallBackToLegacy : GoogleTokenRequestResult
}

interface AuthRepository {
    /**
     * Completes Google sign-in with an ID token and/or legacy account fields.
     * Identity is stored on this device (Room + encrypted prefs). Drive backup is optional.
     */
    suspend fun signInWithGoogle(
        credential: GoogleAuthCredential,
    ): Result<AuthSignInResult>

    suspend fun signInWithEmail(
        email: String,
        password: String,
    ): Result<AuthSignInResult>

    /** Offline / LOCAL_ONLY local_dev session. */
    suspend fun signInAnonymously(): Result<AuthSignInResult>

    fun observeAuthState(): Flow<AuthState>

    suspend fun signOut()

    /** Credential Manager Google ID token request (needs Activity context). */
    suspend fun requestGoogleIdToken(activityContext: Context): GoogleTokenRequestResult

    fun isGoogleCredentialManagerAvailable(): Boolean
}

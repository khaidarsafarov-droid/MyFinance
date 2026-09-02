package com.truckerload.data.repository.auth

import android.content.Context
import android.util.Base64
import android.util.Log
import com.truckerload.R
import com.truckerload.data.preferences.AccountIds
import com.truckerload.data.preferences.AuthCredentialsStore
import com.truckerload.data.preferences.AuthLogin
import com.truckerload.data.preferences.AuthProvider
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.UserProfile
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.di.UserComponentManager
import com.truckerload.presentation.auth.shouldOfferBiometricUnlock
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val authStore: AuthStore,
    private val userProfileStore: UserProfileStore,
    private val credentialsStore: AuthCredentialsStore,
    private val userComponentManager: UserComponentManager,
) : AuthRepository {

    override fun isGoogleCredentialManagerAvailable(): Boolean = false

    override fun observeAuthState(): Flow<AuthState> =
        combine(authStore.isLoggedIn, authStore.userId, authStore.email) { loggedIn, userId, email ->
            if (loggedIn && !userId.isNullOrBlank()) {
                AuthState.SignedIn(AuthUser(userId = userId, email = email))
            } else {
                AuthState.SignedOut
            }
        }

    override suspend fun signOut() {
        // Stage3: single logout path — FGS/presence teardown then session + auth wipe
        withContext(Dispatchers.Main.immediate) {
            com.truckerload.sync.SessionTeardown.signOut(
                context = appContext,
                authStore = authStore,
                endSession = { userComponentManager.endSession() },
            )
        }
    }

    override suspend fun requestGoogleIdToken(activityContext: Context): GoogleTokenRequestResult {
        // Credential Manager often returns "GetCredentialResponse error from framework"
        // and then sits on a 20s timeout. Login uses the legacy account picker instead.
        Log.i(
            "TL",
            "Google Sign-In: skip Credential Manager, use legacy picker " +
                "(ctx=${activityContext.javaClass.simpleName})",
        )
        return GoogleTokenRequestResult.FallBackToLegacy
    }

    override suspend fun signInWithGoogle(
        credential: GoogleAuthCredential,
    ): Result<AuthSignInResult> =
        withContext(Dispatchers.IO) {
            completeLocalGoogle(credential, mutableListOf())
        }

    private suspend fun completeLocalGoogle(
        credential: GoogleAuthCredential,
        toasts: MutableList<String>,
    ): Result<AuthSignInResult> {
        val claims = credential.idToken?.let { decodeGoogleIdToken(it) }
        val profile = UserProfile(
            email = credential.email.ifBlank { claims?.optString("email").orEmpty() },
            givenName = credential.givenName.ifBlank { claims?.optString("given_name").orEmpty() },
            familyName = credential.familyName.ifBlank { claims?.optString("family_name").orEmpty() },
            photoUrl = resolveGooglePhotoUrl(null, credential.idToken, credential.photoUrl),
            googleId = credential.googleId
                ?: claims?.optString("sub")
                ?: decodeGoogleIdToken(credential.idToken.orEmpty())?.optString("sub"),
        )
        return completeLoginResult(
            profile = profile,
            accessToken = credential.idToken,
            googleIdToken = credential.idToken,
            toasts = toasts,
        )
    }

    override suspend fun signInWithEmail(
        email: String,
        password: String,
    ): Result<AuthSignInResult> =
        withContext(Dispatchers.IO) {
            val emailTrimmed = email.trim()
            when {
                emailTrimmed.isBlank() ->
                    Result.failure(IllegalArgumentException(appContext.getString(R.string.auth_error_email_required)))
                password.isBlank() ->
                    Result.failure(IllegalArgumentException(appContext.getString(R.string.auth_error_password_required)))
                else -> signInEmailLocal(emailTrimmed, password)
            }
        }

    private suspend fun signInEmailLocal(
        email: String,
        password: String,
    ): Result<AuthSignInResult> {
        // FIX: never auto-register on failed login — that created accounts with weak/typo passwords
        if (!credentialsStore.hasCredentialsFor(email)) {
            return Result.failure(
                IllegalArgumentException(appContext.getString(R.string.auth_error_invalid_credentials)),
            )
        }
        if (!credentialsStore.validateCredentials(email, password)) {
            return Result.failure(
                IllegalArgumentException(appContext.getString(R.string.auth_error_invalid_credentials)),
            )
        }
        val toasts = mutableListOf<String>()
        val biometric = shouldOfferBiometricUnlock(appContext)
        return completeLoginResult(
            profile = UserProfile(email = email, givenName = "", familyName = "", photoUrl = null),
            supabaseUserId = credentialsStore.boundUserIdFor(email),
            toasts = toasts,
            biometricEnabled = biometric,
        )
    }

    override suspend fun signInAnonymously(): Result<AuthSignInResult> = withContext(Dispatchers.IO) {
        val profile = UserProfile(
            email = "",
            givenName = "Driver",
            familyName = "",
            photoUrl = null,
        )
        val ok = AuthLogin.tryCompleteLogin(
            authStore = authStore,
            userProfileStore = userProfileStore,
            supabaseUserId = AccountIds.LOCAL_DEV,
            profile = profile.copy(email = "local@truckerload.local"),
            rememberMe = true,
        )
        if (!ok) {
            // Force local_dev via completeLogin when tryComplete fails on email rules.
            runCatching {
                AuthLogin.completeLogin(
                    authStore = authStore,
                    userProfileStore = userProfileStore,
                    userId = AccountIds.LOCAL_DEV,
                    profile = profile.copy(email = "local@truckerload.local"),
                    rememberMe = true,
                    provider = AuthProvider.LOCAL,
                )
            }.onFailure {
                return@withContext Result.failure(it)
            }
        }
        delay(400)
        Result.success(
            AuthSignInResult(
                user = AuthUser(
                    userId = AccountIds.LOCAL_DEV,
                    email = "local@truckerload.local",
                    displayName = "Driver",
                ),
            ),
        )
    }

    private suspend fun completeLoginResult(
        profile: UserProfile,
        supabaseUserId: String? = null,
        accessToken: String? = null,
        refreshToken: String? = null,
        googleIdToken: String? = null,
        toasts: List<String> = emptyList(),
        biometricEnabled: Boolean = false,
    ): Result<AuthSignInResult> {
        delay(400)
        val ok = AuthLogin.tryCompleteLogin(
            authStore = authStore,
            userProfileStore = userProfileStore,
            supabaseUserId = supabaseUserId,
            profile = profile,
            rememberMe = true,
            accessToken = accessToken,
            refreshToken = refreshToken,
            googleIdToken = googleIdToken,
        )
        if (!ok) {
            return Result.failure(
                IllegalStateException(appContext.getString(R.string.auth_error_email_required)),
            )
        }
        val userId = authStore.currentUserIdOrNull().orEmpty()
        if (profile.googleId.isNullOrBlank() && profile.email.isNotBlank() && userId.isNotBlank()) {
            runCatching { credentialsStore.saveBoundUserId(profile.email, userId) }
        }
        return Result.success(
            AuthSignInResult(
                user = AuthUser(
                    userId = userId,
                    email = profile.email,
                    displayName = listOf(profile.givenName, profile.familyName)
                        .filter { it.isNotBlank() }
                        .joinToString(" "),
                ),
                toastMessages = toasts,
                biometricEnabled = biometricEnabled,
            ),
        )
    }

    companion object {
        private fun decodeGoogleIdToken(idToken: String): JSONObject? {
            return try {
                val parts = idToken.split(".")
                if (parts.size != 3) return null
                val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP))
                JSONObject(payload)
            } catch (e: Exception) {
                Log.w("TL", "swallowed", e)
                null
            }
        }

        private fun resolveGooglePhotoUrl(
            primary: String?,
            idToken: String? = null,
            accountPhotoUrl: String? = null,
        ): String? {
            primary?.takeIf { it.isNotBlank() }?.let { return it }
            idToken?.let { token ->
                decodeGoogleIdToken(token)?.optString("picture")?.takeIf { it.isNotBlank() }?.let { return it }
            }
            return accountPhotoUrl?.takeIf { it.isNotBlank() }
        }
    }
}

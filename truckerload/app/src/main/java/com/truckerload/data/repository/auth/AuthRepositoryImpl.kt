package com.truckerload.data.repository.auth

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.truckerload.R
import com.truckerload.data.preferences.AccountIds
import com.truckerload.data.preferences.AuthCredentialsStore
import com.truckerload.data.preferences.AuthLogin
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.UserProfile
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.remote.CredentialManagerGoogleSignIn
import com.truckerload.data.remote.SupabaseAuthService
import com.truckerload.di.UserComponentManager
import com.truckerload.presentation.auth.shouldOfferBiometricUnlock
import com.truckerload.utils.findActivity
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

    private val supabaseAuth = SupabaseAuthService(appContext)

    override fun isGoogleCredentialManagerAvailable(): Boolean =
        CredentialManagerGoogleSignIn.isAvailable()

    override fun isSupabaseConfigured(): Boolean = supabaseAuth.isConfigured()

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
        if (!CredentialManagerGoogleSignIn.isAvailable()) {
            return GoogleTokenRequestResult.FallBackToLegacy
        }
        // Credential Manager requires an Activity context — tablets often wrap Compose
        // LocalContext; resolve the host Activity before calling Play Services.
        val host = activityContext.findActivity() ?: activityContext
        val tokenResult = CredentialManagerGoogleSignIn.getGoogleIdToken(host)
        val idToken = tokenResult.getOrNull()
        if (idToken != null) return GoogleTokenRequestResult.Token(idToken)
        return when (tokenResult.exceptionOrNull()) {
            is GetCredentialCancellationException -> GoogleTokenRequestResult.Cancelled
            else -> GoogleTokenRequestResult.FallBackToLegacy
        }
    }

    override suspend fun signInWithGoogle(credential: GoogleAuthCredential): Result<AuthSignInResult> =
        withContext(Dispatchers.IO) {
            val idToken = credential.idToken
            val toasts = mutableListOf<String>()
            if (supabaseAuth.isConfigured() && !idToken.isNullOrBlank()) {
                runCatching { supabaseAuth.signInWithIdToken(idToken) }
                    .getOrElse { e ->
                        toasts += appContext.getString(R.string.login_google_fallback, e.message ?: "")
                        return@withContext completeLocalGoogle(credential, toasts)
                    }
                    .fold(
                        onSuccess = { signInResult ->
                            val u = signInResult.user
                            val cloud = supabaseAuth.getProfile(signInResult.accessToken, u.id).getOrNull()
                            val fullName = cloud?.fullName ?: u.fullName
                            val parts = (fullName ?: "${u.email?.take(10) ?: "User"}").trim().split(" ")
                            val profile = UserProfile(
                                email = u.email ?: cloud?.email ?: credential.email,
                                givenName = parts.firstOrNull() ?: credential.givenName,
                                familyName = parts.drop(1).joinToString(" ")
                                    .ifBlank { credential.familyName },
                                photoUrl = resolveGooglePhotoUrl(u.avatarUrl, idToken, credential.photoUrl),
                                googleId = credential.googleId
                                    ?: decodeGoogleIdToken(idToken)?.optString("sub"),
                                phoneNumber = cloud?.phoneNumber,
                                nickname = cloud?.nickname,
                            )
                            return@withContext completeLoginResult(
                                profile = profile,
                                supabaseUserId = u.id,
                                accessToken = signInResult.accessToken,
                                refreshToken = signInResult.refreshToken,
                                toasts = toasts,
                            )
                        },
                        onFailure = { err ->
                            toasts += appContext.getString(
                                R.string.login_google_fallback,
                                err.message ?: "",
                            )
                            return@withContext completeLocalGoogle(credential, toasts)
                        },
                    )
            }
            completeLocalGoogle(credential, toasts)
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
        return completeLoginResult(profile = profile, toasts = toasts)
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<AuthSignInResult> =
        withContext(Dispatchers.IO) {
            val emailTrimmed = email.trim()
            when {
                emailTrimmed.isBlank() ->
                    Result.failure(IllegalArgumentException(appContext.getString(R.string.auth_error_email_required)))
                password.isBlank() ->
                    Result.failure(IllegalArgumentException(appContext.getString(R.string.auth_error_password_required)))
                !supabaseAuth.isConfigured() -> signInEmailLocal(emailTrimmed, password)
                else -> signInEmailSupabase(emailTrimmed, password)
            }
        }

    private suspend fun signInEmailLocal(email: String, password: String): Result<AuthSignInResult> {
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
        val toasts = mutableListOf(appContext.getString(R.string.supabase_not_configured_local))
        val biometric = shouldOfferBiometricUnlock(appContext)
        return completeLoginResult(
            profile = UserProfile(email = email, givenName = "", familyName = "", photoUrl = null),
            toasts = toasts,
            biometricEnabled = biometric,
        )
    }

    private suspend fun signInEmailSupabase(email: String, password: String): Result<AuthSignInResult> {
        val result = supabaseAuth.signInWithPassword(email, password)
        return result.fold(
            onSuccess = { r ->
                runCatching { credentialsStore.saveCredentials(email, password) }
                    .onFailure { Log.w(TAG, "Failed to cache email credentials after cloud login", it) }
                val biometric = shouldOfferBiometricUnlock(appContext)
                val profileResult = supabaseAuth.getProfile(r.accessToken, r.user.id)
                profileResult.fold(
                    onSuccess = { profile ->
                        val fullName = profile.fullName ?: r.user.fullName
                        val parts = (fullName ?: "").trim().split(" ", limit = 2)
                        completeLoginResult(
                            profile = UserProfile(
                                email = r.user.email ?: profile.email ?: email,
                                givenName = parts.firstOrNull() ?: "",
                                familyName = parts.getOrNull(1) ?: "",
                                photoUrl = null,
                                phoneNumber = profile.phoneNumber,
                                nickname = profile.nickname,
                            ),
                            supabaseUserId = r.user.id,
                            accessToken = r.accessToken,
                            refreshToken = r.refreshToken,
                            biometricEnabled = biometric,
                        )
                    },
                    onFailure = {
                        val parts = (r.user.fullName ?: "").trim().split(" ", limit = 2)
                        completeLoginResult(
                            profile = UserProfile(
                                email = r.user.email ?: email,
                                givenName = parts.firstOrNull() ?: "",
                                familyName = parts.getOrNull(1) ?: "",
                                photoUrl = null,
                            ),
                            supabaseUserId = r.user.id,
                            accessToken = r.accessToken,
                            refreshToken = r.refreshToken,
                            biometricEnabled = biometric,
                        )
                    },
                )
            },
            onFailure = { err ->
                if (credentialsStore.validateCredentials(email, password)) {
                    val toasts = mutableListOf(appContext.getString(R.string.auth_local_login_fallback))
                    val biometric = shouldOfferBiometricUnlock(appContext)
                    completeLoginResult(
                        profile = UserProfile(email = email, givenName = "", familyName = "", photoUrl = null),
                        toasts = toasts,
                        biometricEnabled = biometric,
                    )
                } else {
                    Result.failure(
                        Exception(err.message ?: appContext.getString(R.string.auth_error_login_invalid)),
                    )
                }
            },
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
        )
        if (!ok) {
            return Result.failure(
                IllegalStateException(appContext.getString(R.string.auth_error_email_required)),
            )
        }
        val userId = authStore.currentUserIdOrNull().orEmpty()
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
        private const val TAG = "AuthRepository"

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

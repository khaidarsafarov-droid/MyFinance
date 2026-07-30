package com.truckerload.data.repository

import android.content.Context
import android.util.Base64
import android.util.Log
import com.truckerload.R
import com.truckerload.data.preferences.AuthCredentialsStore
import com.truckerload.data.preferences.AuthLogin
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.UserProfile
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.remote.SupabaseAuthService
import kotlinx.coroutines.delay
import org.json.JSONObject

/**
 * Unified auth: Supabase email/Google, Credential Manager id-token completion, and local fallback.
 */
class AuthRepository(
    private val appContext: Context,
    private val authStore: AuthStore,
    private val userProfileStore: UserProfileStore,
    private val credentialsStore: AuthCredentialsStore,
    private val supabaseAuth: SupabaseAuthService = SupabaseAuthService(appContext),
) {
    fun isSupabaseConfigured(): Boolean = supabaseAuth.isConfigured()

    /**
     * Email/password sign-in. On success, [offerBiometric] is true so the UI can enable biometrics.
     */
    suspend fun signInWithEmail(email: String, password: String): AuthActionResult {
        val emailTrimmed = email.trim()
        when {
            emailTrimmed.isBlank() -> {
                return AuthActionResult.fieldError(appContext.getString(R.string.auth_error_email_required))
            }
            password.isBlank() -> {
                return AuthActionResult.fieldError(appContext.getString(R.string.auth_error_password_required))
            }
            !supabaseAuth.isConfigured() -> {
                if (!credentialsStore.validateCredentials(emailTrimmed, password) &&
                    !credentialsStore.hasCredentialsFor(emailTrimmed)
                ) {
                    credentialsStore.saveCredentials(emailTrimmed, password)
                }
                if (!credentialsStore.validateCredentials(emailTrimmed, password)) {
                    return AuthActionResult.fieldError(
                        appContext.getString(R.string.auth_error_invalid_credentials),
                    )
                }
                val ok = completeLogin(
                    email = emailTrimmed,
                    givenName = "",
                    familyName = "",
                    photoUrl = null,
                )
                return if (ok) {
                    AuthActionResult.success(
                        toastMessage = appContext.getString(R.string.supabase_not_configured_local),
                        offerBiometric = true,
                    )
                } else {
                    AuthActionResult.fieldError(appContext.getString(R.string.auth_error_email_required))
                }
            }
            else -> {
                val result = supabaseAuth.signInWithPassword(emailTrimmed, password)
                return result.fold(
                    onSuccess = { r ->
                        credentialsStore.saveCredentials(emailTrimmed, password)
                        val profileResult = supabaseAuth.getProfile(r.accessToken, r.user.id)
                        val ok = profileResult.fold(
                            onSuccess = { profile ->
                                val fullName = profile.fullName ?: r.user.fullName
                                val parts = (fullName ?: "").trim().split(" ", limit = 2)
                                completeLogin(
                                    email = r.user.email ?: profile.email ?: emailTrimmed,
                                    givenName = parts.firstOrNull().orEmpty(),
                                    familyName = parts.getOrNull(1).orEmpty(),
                                    photoUrl = null,
                                    phoneNumber = profile.phoneNumber,
                                    supabaseUserId = r.user.id,
                                    accessToken = r.accessToken,
                                    refreshToken = r.refreshToken,
                                )
                            },
                            onFailure = {
                                val parts = (r.user.fullName ?: "").trim().split(" ", limit = 2)
                                completeLogin(
                                    email = r.user.email ?: emailTrimmed,
                                    givenName = parts.firstOrNull().orEmpty(),
                                    familyName = parts.getOrNull(1).orEmpty(),
                                    photoUrl = null,
                                    supabaseUserId = r.user.id,
                                    accessToken = r.accessToken,
                                    refreshToken = r.refreshToken,
                                )
                            },
                        )
                        if (ok) {
                            AuthActionResult.success(offerBiometric = true)
                        } else {
                            AuthActionResult.fieldError(
                                appContext.getString(R.string.auth_error_email_required),
                            )
                        }
                    },
                    onFailure = { err ->
                        if (credentialsStore.validateCredentials(emailTrimmed, password)) {
                            val ok = completeLogin(
                                email = emailTrimmed,
                                givenName = "",
                                familyName = "",
                                photoUrl = null,
                            )
                            if (ok) {
                                AuthActionResult.success(
                                    toastMessage = appContext.getString(R.string.auth_local_login_fallback),
                                    offerBiometric = true,
                                )
                            } else {
                                AuthActionResult.fieldError(
                                    appContext.getString(R.string.auth_error_email_required),
                                )
                            }
                        } else {
                            AuthActionResult.fieldError(
                                err.message ?: appContext.getString(R.string.auth_error_login_invalid),
                            )
                        }
                    },
                )
            }
        }
    }

    /** Complete Google sign-in from a Credential Manager / Sign-In id token. */
    suspend fun signInWithGoogleIdToken(idToken: String): AuthActionResult {
        val claims = decodeGoogleIdToken(idToken)
        if (supabaseAuth.isConfigured()) {
            return try {
                val authResult = supabaseAuth.signInWithIdToken(idToken)
                val signIn = authResult.getOrNull()
                val user = signIn?.user
                if (user != null) {
                    val parts = (user.fullName ?: user.email?.take(10) ?: "User").trim().split(" ")
                    val ok = completeLogin(
                        email = user.email ?: claims?.optString("email").orEmpty(),
                        givenName = parts.firstOrNull() ?: claims?.optString("given_name").orEmpty(),
                        familyName = parts.drop(1).joinToString(" ").ifBlank {
                            claims?.optString("family_name").orEmpty()
                        },
                        photoUrl = resolveGooglePhotoUrl(user.avatarUrl, idToken),
                        supabaseUserId = user.id,
                        accessToken = signIn.accessToken,
                        refreshToken = signIn.refreshToken,
                        googleId = claims?.optString("sub"),
                    )
                    if (ok) AuthActionResult.success()
                    else AuthActionResult.toastOnly(appContext.getString(R.string.auth_error_email_required))
                } else {
                    val localOk = completeGoogleLocal(claims, idToken)
                    AuthActionResult(
                        succeeded = localOk,
                        toastMessage = appContext.getString(
                            R.string.login_google_fallback,
                            authResult.exceptionOrNull()?.message.orEmpty(),
                        ),
                    )
                }
            } catch (e: Exception) {
                val localOk = completeGoogleLocal(claims, idToken)
                AuthActionResult(
                    succeeded = localOk,
                    toastMessage = appContext.getString(
                        R.string.login_google_fallback,
                        e.message.orEmpty(),
                    ),
                )
            }
        }
        val ok = completeGoogleLocal(claims, idToken)
        return if (ok) AuthActionResult.success()
        else AuthActionResult.toastOnly(appContext.getString(R.string.auth_error_email_required))
    }

    /** Complete Google sign-in from the legacy GoogleSignIn account picker. */
    suspend fun signInWithGoogleAccount(account: GoogleAccountInfo): AuthActionResult {
        val idToken = account.idToken
        suspend fun signInLocally(): Boolean = completeLogin(
            email = account.email.orEmpty(),
            givenName = account.givenName.orEmpty(),
            familyName = account.familyName.orEmpty(),
            photoUrl = resolveGooglePhotoUrl(null, idToken, account.photoUrl),
            googleId = account.id
                ?: idToken?.let { decodeGoogleIdToken(it)?.optString("sub") },
        )

        if (supabaseAuth.isConfigured() && !idToken.isNullOrBlank()) {
            return try {
                val authResult = supabaseAuth.signInWithIdToken(idToken)
                val signIn = authResult.getOrNull()
                val user = signIn?.user
                if (user != null) {
                    val parts = (user.fullName ?: account.email?.take(10) ?: "User").trim().split(" ")
                    val ok = completeLogin(
                        email = user.email ?: account.email.orEmpty(),
                        givenName = parts.firstOrNull() ?: account.givenName.orEmpty(),
                        familyName = parts.drop(1).joinToString(" ").ifBlank {
                            account.familyName.orEmpty()
                        },
                        photoUrl = resolveGooglePhotoUrl(
                            user.avatarUrl,
                            idToken,
                            account.photoUrl,
                        ),
                        supabaseUserId = user.id,
                        accessToken = signIn.accessToken,
                        refreshToken = signIn.refreshToken,
                        googleId = account.id ?: decodeGoogleIdToken(idToken)?.optString("sub"),
                    )
                    if (ok) AuthActionResult.success()
                    else AuthActionResult.toastOnly(appContext.getString(R.string.auth_error_email_required))
                } else {
                    val localOk = signInLocally()
                    AuthActionResult(
                        succeeded = localOk,
                        toastMessage = appContext.getString(
                            R.string.login_google_fallback,
                            authResult.exceptionOrNull()?.message.orEmpty(),
                        ),
                    )
                }
            } catch (e: Exception) {
                val localOk = signInLocally()
                AuthActionResult(
                    succeeded = localOk,
                    toastMessage = appContext.getString(
                        R.string.login_google_fallback,
                        e.message.orEmpty(),
                    ),
                )
            }
        }
        val ok = signInLocally()
        return if (ok) AuthActionResult.success()
        else AuthActionResult.toastOnly(appContext.getString(R.string.auth_error_email_required))
    }

    private suspend fun completeGoogleLocal(claims: JSONObject?, idToken: String): Boolean =
        completeLogin(
            email = claims?.optString("email").orEmpty(),
            givenName = claims?.optString("given_name").orEmpty(),
            familyName = claims?.optString("family_name").orEmpty(),
            photoUrl = resolveGooglePhotoUrl(null, idToken),
            googleId = claims?.optString("sub"),
        )

    /**
     * Persists profile and opens the session after a short delay (matches prior LoginScreen UX).
     * @return false when identity is incomplete.
     */
    suspend fun completeLogin(
        email: String,
        givenName: String,
        familyName: String,
        photoUrl: String?,
        rememberMe: Boolean = true,
        phoneNumber: String? = null,
        supabaseUserId: String? = null,
        accessToken: String? = null,
        refreshToken: String? = null,
        googleId: String? = null,
    ): Boolean {
        val profile = UserProfile(
            email = email,
            givenName = givenName,
            familyName = familyName,
            photoUrl = photoUrl,
            phoneNumber = phoneNumber?.takeIf { it.isNotBlank() },
            googleId = googleId?.takeIf { it.isNotBlank() },
        )
        delay(LOGIN_FINISH_DELAY_MS)
        return AuthLogin.tryCompleteLogin(
            authStore = authStore,
            userProfileStore = userProfileStore,
            supabaseUserId = supabaseUserId,
            profile = profile,
            rememberMe = rememberMe,
            accessToken = accessToken,
            refreshToken = refreshToken,
        )
    }

    companion object {
        private const val LOGIN_FINISH_DELAY_MS = 400L

        fun decodeGoogleIdToken(idToken: String): JSONObject? {
            return try {
                val parts = idToken.split(".")
                if (parts.size != 3) return null
                val payload = String(
                    Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP),
                )
                JSONObject(payload)
            } catch (e: Exception) {
                Log.w("TL", "swallowed", e)
                null
            }
        }

        fun resolveGooglePhotoUrl(
            primary: String?,
            idToken: String? = null,
            accountPhotoUrl: String? = null,
        ): String? {
            primary?.takeIf { it.isNotBlank() }?.let { return it }
            idToken?.let { token ->
                decodeGoogleIdToken(token)?.optString("picture")
                    ?.takeIf { it.isNotBlank() }
                    ?.let { return it }
            }
            return accountPhotoUrl?.takeIf { it.isNotBlank() }
        }
    }
}

/** Platform-agnostic Google account fields (from Credential Manager or legacy GoogleSignIn). */
data class GoogleAccountInfo(
    val idToken: String?,
    val email: String?,
    val givenName: String?,
    val familyName: String?,
    val photoUrl: String?,
    val id: String?,
)

data class AuthActionResult(
    val succeeded: Boolean,
    val fieldError: String? = null,
    val toastMessage: String? = null,
    val offerBiometric: Boolean = false,
) {
    companion object {
        fun success(
            toastMessage: String? = null,
            offerBiometric: Boolean = false,
        ) = AuthActionResult(
            succeeded = true,
            toastMessage = toastMessage,
            offerBiometric = offerBiometric,
        )

        fun fieldError(message: String) = AuthActionResult(
            succeeded = false,
            fieldError = message,
        )

        fun toastOnly(message: String) = AuthActionResult(
            succeeded = false,
            toastMessage = message,
        )
    }
}

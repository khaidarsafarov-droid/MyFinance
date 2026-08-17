package com.truckerload.data.remote

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.truckerload.BuildConfig
import java.security.SecureRandom
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Credential Manager (One Tap replacement) — современный Sign-in with Google.
 * Показывает bottom sheet вместо отдельного Activity.
 *
 * Use only for **explicit** user sign-in / reconnect (Login screen).
 * Do **not** call from cold-start [com.truckerload.data.auth.SilentAuthRestorer]:
 * even `silent=true` can show the Google account UI on every launch.
 */
object CredentialManagerGoogleSignIn {

    private val executor = Executors.newSingleThreadExecutor()
    private const val REQUEST_TIMEOUT_MS = 20_000L

    /**
     * Credential Manager account picker is flaky on many tablets / large-screen OEMs.
     * Prefer legacy Google Sign-In Activity there (sw ≥ 600dp).
     */
    fun isAvailable(context: Context? = null): Boolean {
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) return false
        val sw = context?.resources?.configuration?.smallestScreenWidthDp ?: 0
        if (sw >= 600) return false
        return resolveActivity(context) != null
    }

    /**
     * Запрашивает Google ID token через Credential Manager.
     * Tries previously-authorized accounts first, then the full account picker.
     * Times out so a hung Play Services sheet cannot leave Login stuck on spinner.
     */
    suspend fun getGoogleIdToken(
        context: Context,
        silent: Boolean = false,
    ): Result<String> = withContext(Dispatchers.Main.immediate) {
        if (!isAvailable(context)) {
            return@withContext Result.failure(
                Exception(
                    if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) {
                        "GOOGLE_WEB_CLIENT_ID is not configured"
                    } else {
                        "Credential Manager skipped; use legacy Google Sign-In"
                    },
                ),
            )
        }
        val activity = resolveActivity(context)
            ?: return@withContext Result.failure(Exception("Activity required for Google Sign-In"))

        val timed = withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
            if (silent) {
                requestToken(activity, filterAuthorized = true)
            } else {
                val authorized = requestToken(activity, filterAuthorized = true)
                val authedToken = authorized.getOrNull()
                if (authedToken != null) {
                    Result.success(authedToken)
                } else when (authorized.exceptionOrNull()) {
                    is GetCredentialCancellationException -> authorized
                    else -> requestToken(activity, filterAuthorized = false)
                }
            }
        }
        timed ?: Result.failure(Exception("Google Sign-In timed out"))
    }

    private suspend fun requestToken(
        activity: Activity,
        filterAuthorized: Boolean,
    ): Result<String> = suspendCoroutine { cont ->
        val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        val credentialManager = CredentialManager.create(activity)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filterAuthorized)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(filterAuthorized)
            .setNonce(newNonce())
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val callback = object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
            override fun onResult(result: GetCredentialResponse) {
                try {
                    val idToken = extractIdToken(result.credential)
                    if (!idToken.isNullOrBlank()) {
                        cont.resume(Result.success(idToken))
                    } else {
                        cont.resume(Result.failure(Exception("Failed to obtain ID token")))
                    }
                } catch (e: Exception) {
                    cont.resume(Result.failure(e))
                }
            }

            override fun onError(e: GetCredentialException) {
                if (e !is NoCredentialException && e !is GetCredentialCancellationException) {
                    Log.w("TL", "Credential Manager Google Sign-In failed", e)
                }
                cont.resume(Result.failure(e))
            }
        }

        credentialManager.getCredentialAsync(activity, request, null, executor, callback)
    }

    private fun extractIdToken(credential: androidx.credentials.Credential): String? =
        when (credential) {
            is GoogleIdTokenCredential -> credential.idToken
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL ||
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL
                ) {
                    GoogleIdTokenCredential.createFrom(credential.data).idToken
                } else {
                    null
                }
            }
            else -> null
        }

    private fun newNonce(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { b -> "%02x".format(b) }
    }

    private fun resolveActivity(context: Context?): Activity? {
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return current as? Activity
    }
}

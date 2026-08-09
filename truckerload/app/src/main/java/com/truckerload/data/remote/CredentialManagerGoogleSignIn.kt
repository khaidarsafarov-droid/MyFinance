package com.truckerload.data.remote

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.truckerload.BuildConfig
import com.truckerload.utils.findActivity
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import java.security.SecureRandom
import java.util.concurrent.Executors

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

    fun isAvailable(): Boolean = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    /**
     * Запрашивает Google ID token через Credential Manager.
     * @param silent when true, only previously authorized accounts with auto-select.
     *   Still may show UI — never use for automatic cold-start restore.
     */
    suspend fun getGoogleIdToken(
        context: Context,
        silent: Boolean = false,
    ): Result<String> = suspendCoroutine { cont ->
        val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (webClientId.isBlank()) {
            cont.resume(Result.failure(Exception("GOOGLE_WEB_CLIENT_ID is not configured")))
            return@suspendCoroutine
        }
        // Activity context is required on many tablets; ApplicationContext silently fails.
        val uiContext = context.findActivity() ?: context
        val credentialManager = CredentialManager.create(uiContext)
        val nonce = ByteArray(16).also { SecureRandom().nextBytes(it) }
            .joinToString("") { b -> "%02x".format(b) }
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(silent)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(silent)
            .setNonce(nonce)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val callback = object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
            override fun onResult(result: GetCredentialResponse) {
                try {
                    val credential = result.credential
                    val idToken = when (credential) {
                        is GoogleIdTokenCredential -> credential.idToken
                        is CustomCredential -> {
                            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL ||
                                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL
                            ) {
                                GoogleIdTokenCredential.createFrom(credential.data).idToken
                            } else null
                        }
                        else -> null
                    }
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
                cont.resume(Result.failure(e))
            }
        }

        credentialManager.getCredentialAsync(uiContext, request, null, executor, callback)
    }
}

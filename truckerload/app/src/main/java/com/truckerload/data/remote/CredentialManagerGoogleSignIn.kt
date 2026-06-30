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
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import java.util.concurrent.Executors

/**
 * Credential Manager (One Tap replacement) — современный Sign-in with Google.
 * Показывает bottom sheet вместо отдельного Activity.
 */
object CredentialManagerGoogleSignIn {

    private val executor = Executors.newSingleThreadExecutor()

    fun isAvailable(): Boolean = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    /**
     * Запрашивает Google ID token через Credential Manager.
     */
    suspend fun getGoogleIdToken(context: Context): Result<String> = suspendCoroutine { cont ->
        val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (webClientId.isBlank()) {
            cont.resume(Result.failure(Exception("GOOGLE_WEB_CLIENT_ID не настроен")))
            return@suspendCoroutine
        }
        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
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
                        cont.resume(Result.failure(Exception("Не удалось получить ID token")))
                    }
                } catch (e: Exception) {
                    cont.resume(Result.failure(e))
                }
            }

            override fun onError(e: GetCredentialException) {
                cont.resume(Result.failure(e))
            }
        }

        credentialManager.getCredentialAsync(context, request, null, executor, callback)
    }
}

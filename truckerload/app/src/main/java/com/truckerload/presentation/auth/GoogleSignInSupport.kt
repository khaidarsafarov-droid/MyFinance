package com.truckerload.presentation.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.truckerload.BuildConfig
import com.truckerload.R

/** Shared Google Sign-In helpers for Login + SignUp paths. */
object GoogleSignInSupport {

    fun resolveActivity(context: Context): Activity? {
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return current as? Activity
    }

    fun buildSignInOptions(): GoogleSignInOptions {
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()) {
            builder.requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
        }
        return builder.build()
    }

    fun client(context: Context): GoogleSignInClient =
        GoogleSignIn.getClient(context, buildSignInOptions())

    /**
     * Clear a stale Google session, then run [onReady] so the account picker always
     * appears (avoids silent failures with a previously selected account).
     */
    fun signOutThen(context: Context, onReady: () -> Unit) {
        runCatching {
            client(context).signOut().addOnCompleteListener { onReady() }
        }.onFailure { onReady() }
    }

    fun formatError(context: Context, error: Throwable?): String {
        val api = error as? ApiException
        val status = api?.statusCode
        val detail = when (status) {
            10 -> context.getString(R.string.login_google_developer_error)
            12500 -> context.getString(R.string.login_google_play_services_error)
            12501, 16 -> context.getString(R.string.login_google_cancelled)
            else -> api?.message?.takeIf { it.isNotBlank() }
                ?: error?.message?.takeIf { it.isNotBlank() }
                ?: status?.toString()
                ?: error?.toString().orEmpty()
        }
        return if (status == 12501 || status == 16) {
            detail
        } else {
            context.getString(R.string.login_google_error, detail)
        }
    }
}

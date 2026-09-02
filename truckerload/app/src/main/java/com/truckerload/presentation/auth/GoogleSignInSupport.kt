package com.truckerload.presentation.auth

import android.content.Context
import com.google.android.gms.common.api.ApiException
import com.truckerload.R
import com.truckerload.utils.InstalledSigningSha1

/** User-facing Google Play Services errors for Drive OAuth. */
object GoogleSignInSupport {

    fun formatError(context: Context, error: Throwable?): String {
        val api = error as? ApiException
        val status = api?.statusCode
        val detail = when (status) {
            10 -> {
                val sha = InstalledSigningSha1.fingerprint(context) ?: "—"
                context.getString(R.string.login_google_developer_error, sha)
            }
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

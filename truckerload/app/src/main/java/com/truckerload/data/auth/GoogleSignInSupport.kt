package com.truckerload.data.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

/**
 * Shared guards for Google Sign-In across Credential Manager and legacy GMS flows.
 */
object GoogleSignInSupport {

    fun isPlayServicesAvailable(context: Context): Boolean {
        return GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context.applicationContext) == ConnectionResult.SUCCESS
    }

    /**
     * Credential Manager requires an [Activity] to show the account picker.
     * On tablets / multi-window, [Context] from Compose may not be an Activity.
     */
    fun findActivity(context: Context): Activity? {
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }

    fun requireActivity(context: Context): Activity =
        findActivity(context) ?: error("Activity context required for Google Sign-In")
}

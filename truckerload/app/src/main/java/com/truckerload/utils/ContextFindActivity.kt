package com.truckerload.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Unwraps Compose / theme wrappers to the host [Activity].
 *
 * Google Sign-In and Credential Manager require an Activity; `LocalContext` is
 * often a [ContextWrapper], and `applicationContext` cannot start the consent UI.
 */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> {
        val base = baseContext
        if (base === this) null else base.findActivity()
    }
    else -> null
}

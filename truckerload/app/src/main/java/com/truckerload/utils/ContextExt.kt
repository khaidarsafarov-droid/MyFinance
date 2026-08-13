package com.truckerload.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** Walks [ContextWrapper] chain to find a hosting [Activity] (needed for Credential Manager / Google). */
fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}

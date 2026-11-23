package com.truckerload.utils

import android.util.Log

/**
 * On-device log only. Debug and release builds do not link a crash-reporting SDK,
 * including when a google-services.json file is present on the build machine.
 */
object CrashReporting {
    private const val TAG = "CrashReporting"

    fun setCustomKey(key: String, value: String) {
        note(key, value)
    }

    fun setCustomKey(key: String, value: Boolean) {
        note(key, value.toString())
    }

    fun setCustomKey(key: String, value: Long) {
        note(key, value.toString())
    }

    fun recordException(throwable: Throwable) {
        val safe = LogRedactor.redactThrowable(throwable)
        Log.e(TAG, safe.message ?: safe.javaClass.simpleName, safe)
    }

    private fun note(key: String, value: String) {
        if (key.isEmpty() && value.isEmpty()) return
    }
}

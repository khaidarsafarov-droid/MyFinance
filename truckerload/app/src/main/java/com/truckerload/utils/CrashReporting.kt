package com.truckerload.utils

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.truckerload.BuildConfig

/**
 * Best-effort Crashlytics wrapper. No-ops when Firebase is not configured.
 */
object CrashReporting {
    private const val TAG = "CrashReporting"

    fun setCustomKey(key: String, value: String) {
        if (!BuildConfig.FIREBASE_CONFIGURED) return
        runCatching {
            FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        }.onFailure { e ->
            Log.w(TAG, "setCustomKey($key) failed: ${e.javaClass.simpleName}")
        }
    }

    fun setCustomKey(key: String, value: Boolean) {
        if (!BuildConfig.FIREBASE_CONFIGURED) return
        runCatching {
            FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        }.onFailure { e ->
            Log.w(TAG, "setCustomKey($key) failed: ${e.javaClass.simpleName}")
        }
    }

    fun setCustomKey(key: String, value: Long) {
        if (!BuildConfig.FIREBASE_CONFIGURED) return
        runCatching {
            FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        }.onFailure { e ->
            Log.w(TAG, "setCustomKey($key) failed: ${e.javaClass.simpleName}")
        }
    }

    fun recordException(throwable: Throwable) {
        val safe = LogRedactor.redactThrowable(throwable)
        Log.e(TAG, safe.message ?: safe.javaClass.simpleName, safe)
        if (!BuildConfig.FIREBASE_CONFIGURED) return
        runCatching {
            FirebaseCrashlytics.getInstance().recordException(safe)
        }.onFailure { e ->
            Log.w(TAG, "recordException failed: ${e.javaClass.simpleName}")
        }
    }
}

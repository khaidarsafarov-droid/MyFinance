package com.truckerload.presentation.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** System "Remove animations" / animator duration scale == 0. */
fun Context.isSystemReduceMotionEnabled(): Boolean {
    return runCatching {
        Settings.Global.getFloat(
            contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }.getOrDefault(false)
}

/** Effective reduce-motion: user preference OR system animator scale off. */
fun Context.effectiveReduceMotion(userPrefersReduce: Boolean): Boolean {
    return userPrefersReduce || isSystemReduceMotionEnabled()
}

@Composable
fun rememberEffectiveReduceMotion(userPrefersReduce: Boolean): Boolean {
    val context = LocalContext.current
    return remember(userPrefersReduce) {
        context.effectiveReduceMotion(userPrefersReduce)
    }
}

package com.truckerload.presentation.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * When true, decorative and non-essential motion should be skipped or
 * collapsed to an instant transition (calm / accessibility baseline).
 */
val LocalReduceMotion = compositionLocalOf { false }

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

/**
 * Effective reduce-motion: user preference OR system animator scale off.
 */
fun Context.effectiveReduceMotion(userPrefersReduce: Boolean): Boolean {
    return userPrefersReduce || isSystemReduceMotionEnabled()
}

/** Duration helper — returns 0 when reduce-motion is active. */
@Composable
fun motionDurationMs(preferredMs: Int): Int {
    return if (LocalReduceMotion.current) 0 else preferredMs.coerceIn(0, 500)
}

@Composable
fun rememberEffectiveReduceMotion(userPrefersReduce: Boolean): Boolean {
    val context = LocalContext.current
    return remember(userPrefersReduce) {
        context.effectiveReduceMotion(userPrefersReduce)
    }
}

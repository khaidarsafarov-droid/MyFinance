package com.truckerload.presentation.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable

/**
 * Respects system animator scale and the in-app "reduce motion" preference.
 * The in-app flag is applied by [TruckerLoadTheme] via [LocalReduceMotion].
 */
object MotionPolicy {

    fun isSystemReducedMotion(context: Context): Boolean = runCatching {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }.getOrDefault(false)
}

@Composable
fun rememberReduceMotion(): Boolean = LocalReduceMotion.current

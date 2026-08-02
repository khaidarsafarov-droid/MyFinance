package com.truckerload.presentation.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.presentation.di.LocalSettingsDataStore

/**
 * Respects system animator scale and the in-app "reduce motion" preference.
 */
object MotionPolicy {

    fun isSystemReducedMotion(context: Context): Boolean {
        return runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
}

@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    val settingsDataStore = LocalSettingsDataStore.current
    val userPref by settingsDataStore.reduceMotion.collectAsStateWithLifecycle(initialValue = false)
    val systemReduced = remember(context) { MotionPolicy.isSystemReducedMotion(context) }
    return userPref || systemReduced
}

package com.truckerload.presentation.motion

import androidx.compose.runtime.Composable
import com.truckerload.presentation.theme.LocalReduceMotion
import com.truckerload.presentation.theme.rememberSystemReduceMotion

/**
 * True when neither the in-app preference nor the system animator scale
 * has reduced motion. Both read [rememberSystemReduceMotion] on resume.
 */
@Composable
fun rememberMotionEnabled(): Boolean =
    !LocalReduceMotion.current && !rememberSystemReduceMotion()

internal const val SAVE_CONFIRM_HOLD_MS = 380L

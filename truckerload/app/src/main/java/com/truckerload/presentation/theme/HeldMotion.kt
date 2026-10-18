package com.truckerload.presentation.theme

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.truckerload.presentation.ScreenPhase
import kotlinx.coroutines.delay

/** Keep a loading skeleton on screen for at least [minMs], then reveal [phase]. */
@Composable
fun rememberHeldScreenPhase(
    phase: ScreenPhase,
    minMs: Int = 300,
): ScreenPhase {
    val reduceMotion = LocalReduceMotion.current
    var shown by remember { mutableStateOf(phase) }
    var sawLoading by remember { mutableStateOf(phase == ScreenPhase.Loading) }
    var loadingSince by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(phase, reduceMotion) {
        if (phase == ScreenPhase.Loading) {
            sawLoading = true
            loadingSince = SystemClock.elapsedRealtime()
            shown = ScreenPhase.Loading
            return@LaunchedEffect
        }
        if (sawLoading && shown == ScreenPhase.Loading && !reduceMotion) {
            val wait = (minMs - (SystemClock.elapsedRealtime() - loadingSince)).coerceAtLeast(0L)
            if (wait > 0L) delay(wait)
            sawLoading = false
        }
        shown = phase
    }
    return shown
}

/**
 * Once [visible] becomes true, keep the result true until [minMs] has elapsed
 * after it turns false. Reduce-motion hides immediately.
 */
@Composable
fun rememberMinVisible(
    visible: Boolean,
    minMs: Int = 300,
): Boolean {
    val reduceMotion = LocalReduceMotion.current
    var shown by remember { mutableStateOf(visible) }
    var since by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(visible, reduceMotion) {
        if (visible) {
            since = SystemClock.elapsedRealtime()
            shown = true
        } else if (shown && !reduceMotion) {
            val wait = (minMs - (SystemClock.elapsedRealtime() - since)).coerceAtLeast(0L)
            if (wait > 0L) delay(wait)
            shown = false
        } else {
            shown = false
        }
    }
    return shown
}

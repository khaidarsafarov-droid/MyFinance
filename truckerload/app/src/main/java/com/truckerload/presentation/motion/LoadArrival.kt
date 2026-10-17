package com.truckerload.presentation.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/** A load card slides in only when it was just saved. Older rows stay still. */
internal object LoadArrival {
    const val WINDOW_MS = 1_600L

    fun shouldPlay(parsedAt: Long, now: Long): Boolean {
        if (parsedAt <= 0L) return false
        val age = now - parsedAt
        return age in 0..WINDOW_MS
    }
}

@Composable
internal fun Modifier.arriveIfRecent(
    parsedAt: Long,
    now: Long = System.currentTimeMillis(),
): Modifier {
    val play = LoadArrival.shouldPlay(parsedAt, now)
    val motion = rememberMotionEnabled()
    val progress = remember(parsedAt) { Animatable(if (play && motion) 0f else 1f) }
    LaunchedEffect(parsedAt, play, motion) {
        if (play && motion) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(durationMillis = 280))
        } else {
            progress.snapTo(1f)
        }
    }
    return this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 28.dp.toPx()
    }
}

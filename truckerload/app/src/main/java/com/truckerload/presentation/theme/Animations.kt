package com.truckerload.presentation.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Process-wide reduce-motion flag mirrored from [LocalReduceMotion] so
 * non-composable NavHost transition factories can respect it.
 */
object MotionPreferences {
    @Volatile
    var reduceMotion: Boolean = false
}

private fun durationMs(preferredMs: Int): Int =
    if (MotionPreferences.reduceMotion) 0 else preferredMs.coerceIn(0, 500)

fun tabEnterTransition(): EnterTransition {
    val ms = durationMs(120)
    return if (ms == 0) EnterTransition.None else fadeIn(animationSpec = tween(ms, easing = EaseOutCubic))
}

fun tabExitTransition(): ExitTransition {
    val ms = durationMs(90)
    return if (ms == 0) ExitTransition.None else fadeOut(animationSpec = tween(ms, easing = EaseOutCubic))
}

fun screenEnterAnimation(): EnterTransition {
    val ms = durationMs(160)
    return if (ms == 0) EnterTransition.None else fadeIn(animationSpec = tween(ms, easing = EaseOutCubic))
}

fun staggeredListEnter(index: Int): EnterTransition {
    val ms = durationMs(180)
    if (ms == 0) return EnterTransition.None
    val delay = (index * 20).coerceAtMost(120)
    return fadeIn(animationSpec = tween(ms, delayMillis = delay, easing = EaseOutCubic)) +
        scaleIn(
            animationSpec = tween(ms, delayMillis = delay, easing = EaseOutCubic),
            initialScale = 0.98f,
        )
}

@Composable
fun StaggeredAnimatedItem(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = true,
        enter = staggeredListEnter(index),
        modifier = modifier,
    ) {
        content()
    }
}

fun Modifier.neoGlassPressScale(): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val duration = durationMs(120)
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = tween(duration),
        label = "neoGlassPress",
    )
    scale(scale).pointerInput(Unit) {
        detectTapGestures(
            onPress = {
                pressed = true
                try {
                    awaitRelease()
                } finally {
                    pressed = false
                }
            },
        )
    }
}

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

private val NoneEnter: EnterTransition = fadeIn(animationSpec = tween(0))
private val NoneExit: ExitTransition = fadeOut(animationSpec = tween(0))

fun tabEnterTransition(reduceMotion: Boolean): EnterTransition =
    if (reduceMotion) NoneEnter else fadeIn(animationSpec = tween(120, easing = EaseOutCubic))

fun tabExitTransition(reduceMotion: Boolean): ExitTransition =
    if (reduceMotion) NoneExit else fadeOut(animationSpec = tween(90, easing = EaseOutCubic))

fun screenEnterAnimation(reduceMotion: Boolean): EnterTransition =
    if (reduceMotion) NoneEnter else fadeIn(animationSpec = tween(160, easing = EaseOutCubic))

fun staggeredListEnter(index: Int, reduceMotion: Boolean): EnterTransition {
    if (reduceMotion) return NoneEnter
    val delay = (index * 20).coerceAtMost(120)
    return fadeIn(animationSpec = tween(180, delayMillis = delay, easing = EaseOutCubic)) +
        scaleIn(
            animationSpec = tween(180, delayMillis = delay, easing = EaseOutCubic),
            initialScale = 0.98f,
        )
}

@Composable
fun StaggeredAnimatedItem(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val reduceMotion = rememberReduceMotion()
    AnimatedVisibility(
        visible = true,
        enter = staggeredListEnter(index, reduceMotion),
        modifier = modifier,
    ) {
        content()
    }
}

fun Modifier.neoGlassPressScale(): Modifier = composed {
    val reduceMotion = rememberReduceMotion()
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (!reduceMotion && pressed) 0.95f else 1f,
        animationSpec = tween(if (reduceMotion) 0 else 120),
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
            }
        )
    }
}

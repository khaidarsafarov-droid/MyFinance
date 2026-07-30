package com.truckerload.presentation.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
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
 * Bottom-tab switches use no transition so Compose does not keep two heavy
 * destinations on screen at once (that felt like freezes between pages).
 */
fun tabEnterTransition(): EnterTransition = EnterTransition.None

fun tabExitTransition(): ExitTransition = ExitTransition.None

fun screenEnterAnimation() =
    fadeIn(animationSpec = tween(220, easing = EaseOutCubic)) +
        slideInVertically(
            animationSpec = tween(220, easing = EaseOutCubic),
            initialOffsetY = { it / 16 }
        )

fun staggeredListEnter(index: Int) =
    fadeIn(animationSpec = tween(220, delayMillis = (index * 30).coerceAtMost(150), easing = EaseOutCubic)) +
        slideInHorizontally(
            animationSpec = tween(220, delayMillis = (index * 30).coerceAtMost(150), easing = EaseOutCubic),
            initialOffsetX = { it / 16 }
        ) +
        scaleIn(
            animationSpec = tween(220, delayMillis = (index * 30).coerceAtMost(150), easing = EaseOutCubic),
            initialScale = 0.98f
        )

@Composable
fun StaggeredAnimatedItem(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = true,
        enter = staggeredListEnter(index),
        modifier = modifier
    ) {
        content()
    }
}

fun Modifier.neoGlassPressScale(): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = tween(120),
        label = "neoGlassPress"
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

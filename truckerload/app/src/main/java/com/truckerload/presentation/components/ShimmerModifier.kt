package com.truckerload.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import com.truckerload.presentation.theme.rememberReduceMotion

private val LocalShimmerAlpha = staticCompositionLocalOf<State<Float>?> { null }

/** True once an ancestor already applied the shared pulse, so children do not stack alpha. */
private val LocalShimmerConsumed = staticCompositionLocalOf { false }

@Composable
private fun rememberShimmerAlpha(): State<Float> {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    return infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )
}

/**
 * One infinite transition for every [shimmerPulse] inside [content].
 * Nested pulses reuse that clock instead of starting another.
 */
@Composable
fun ProvideShimmerPulse(content: @Composable () -> Unit) {
    val reduceMotion = rememberReduceMotion()
    if (reduceMotion) {
        content()
        return
    }
    val alpha = rememberShimmerAlpha()
    CompositionLocalProvider(LocalShimmerAlpha provides alpha, content = content)
}

@Composable
fun Modifier.shimmerPulse(): Modifier {
    val reduceMotion = rememberReduceMotion()
    if (reduceMotion) {
        return alpha(0.45f)
    }
    if (LocalShimmerConsumed.current) return this
    val alphaState = LocalShimmerAlpha.current ?: rememberShimmerAlpha()
    return graphicsLayer { alpha = alphaState.value }
}

/** Marks descendants so a second [shimmerPulse] does not multiply the same alpha. */
@Composable
fun ShimmerAlreadyApplied(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalShimmerConsumed provides true, content = content)
}

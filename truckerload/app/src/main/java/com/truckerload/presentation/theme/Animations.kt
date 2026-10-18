package com.truckerload.presentation.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/** True when the user opted into reduce-motion or the system animator scale is off. */
val LocalReduceMotion = compositionLocalOf { false }

/** Emphasized decelerate — enter / expand (motion spec). */
val MotionEasingEmphasizedDecelerate = CubicBezierEasing(0.3f, 0f, 0.1f, 1f)

/** Emphasized accelerate — exit / collapse (motion spec). */
val MotionEasingEmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

/**
 * System animator scale, re-read whenever the host resumes so a change in
 * system settings applies without recreating the activity. This is the single
 * source for [MotionPolicy.rememberReduceMotion] and [TruckerLoadTheme].
 */
@Composable
fun rememberSystemReduceMotion(): Boolean {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var reduced by remember { mutableStateOf(MotionPolicy.isSystemReducedMotion(context)) }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                reduced = MotionPolicy.isSystemReducedMotion(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        reduced = MotionPolicy.isSystemReducedMotion(context)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return reduced
}

/** FAB → add-load container transform. Instant when reduce-motion is on. */
fun navAddLoadEnter(reduceMotion: Boolean = false): EnterTransition {
    if (reduceMotion) return fadeIn(animationSpec = tween(0))
    return fadeIn(animationSpec = tween(220, easing = MotionEasingEmphasizedDecelerate)) +
        scaleIn(
            animationSpec = tween(450, easing = MotionEasingEmphasizedDecelerate),
            initialScale = 0.92f,
            transformOrigin = TransformOrigin(0.9f, 1f),
        )
}

fun navAddLoadExit(reduceMotion: Boolean = false): ExitTransition {
    if (reduceMotion) return fadeOut(animationSpec = tween(0))
    return fadeOut(animationSpec = tween(180, easing = MotionEasingEmphasizedAccelerate)) +
        scaleOut(
            animationSpec = tween(450, easing = MotionEasingEmphasizedAccelerate),
            targetScale = 0.92f,
            transformOrigin = TransformOrigin(0.9f, 1f),
        )
}

fun motionDurationMs(reduceMotion: Boolean, normalMs: Int): Int =
    if (reduceMotion) 0 else normalMs

fun tabEnterTransition(reduceMotion: Boolean = false) =
    fadeIn(
        animationSpec = tween(
            motionDurationMs(reduceMotion, 120),
            easing = EaseOutCubic,
        ),
    )

fun tabExitTransition(reduceMotion: Boolean = false) =
    fadeOut(
        animationSpec = tween(
            motionDurationMs(reduceMotion, 90),
            easing = EaseOutCubic,
        ),
    )

/**
 * Short horizontal slide when moving between Loads, Goal, and Profile.
 * [slideSign] is 1 toward the right tab, -1 toward the left. Nested screens
 * keep the plain fade.
 */
fun mainTabEnter(reduceMotion: Boolean, slideSign: Int): EnterTransition {
    if (reduceMotion || slideSign == 0) return tabEnterTransition(reduceMotion)
    return slideInHorizontally(
        animationSpec = tween(200, easing = EaseOutCubic),
        initialOffsetX = { fullWidth -> slideSign * fullWidth / 5 },
    ) + fadeIn(animationSpec = tween(200, easing = EaseOutCubic))
}

fun mainTabExit(reduceMotion: Boolean, slideSign: Int): ExitTransition {
    if (reduceMotion || slideSign == 0) return tabExitTransition(reduceMotion)
    return slideOutHorizontally(
        animationSpec = tween(200, easing = EaseOutCubic),
        targetOffsetX = { fullWidth -> -slideSign * fullWidth / 5 },
    ) + fadeOut(animationSpec = tween(160, easing = EaseOutCubic))
}

/**
 * Material shared-axis X (forward): list → detail, home → add, drawer push screens.
 * When [reduceMotion] is on, falls back to an instant fade (no slide).
 */
fun navForwardEnter(reduceMotion: Boolean = false): EnterTransition {
    if (reduceMotion) return fadeIn(animationSpec = tween(0))
    return slideInHorizontally(
        animationSpec = tween(280, easing = EaseOutCubic),
        initialOffsetX = { fullWidth -> fullWidth / 4 },
    ) + fadeIn(animationSpec = tween(220, easing = EaseOutCubic))
}

fun navForwardExit(reduceMotion: Boolean = false): ExitTransition {
    if (reduceMotion) return fadeOut(animationSpec = tween(0))
    return slideOutHorizontally(
        animationSpec = tween(280, easing = EaseOutCubic),
        targetOffsetX = { fullWidth -> -fullWidth / 10 },
    ) + fadeOut(animationSpec = tween(180, easing = EaseOutCubic))
}

/**
 * Predictive-back-friendly pop enter: the destination under the top screen
 * subtly scales up as the gesture seeks (Navigation Compose seeks these specs).
 */
fun navPopEnter(reduceMotion: Boolean = false): EnterTransition {
    if (reduceMotion) return fadeIn(animationSpec = tween(0))
    return fadeIn(animationSpec = tween(220, easing = EaseOutCubic)) +
        scaleIn(
            animationSpec = tween(280, easing = EaseOutCubic),
            initialScale = 0.92f,
            transformOrigin = TransformOrigin.Center,
        )
}

/**
 * Predictive-back-friendly pop exit: top screen scales down + fades so the
 * system back gesture can scrub a Material-style peek of the previous route.
 */
fun navPopExit(reduceMotion: Boolean = false): ExitTransition {
    if (reduceMotion) return fadeOut(animationSpec = tween(0))
    return fadeOut(animationSpec = tween(180, easing = EaseOutCubic)) +
        scaleOut(
            animationSpec = tween(280, easing = EaseOutCubic),
            targetScale = 0.90f,
            transformOrigin = TransformOrigin.Center,
        )
}

/** Modal / immersive: camera, scanner, galleries, attach picker. */
fun navModalEnter(reduceMotion: Boolean = false): EnterTransition {
    if (reduceMotion) return fadeIn(animationSpec = tween(0))
    return slideInVertically(
        animationSpec = tween(280, easing = EaseOutCubic),
        initialOffsetY = { fullHeight -> fullHeight / 5 },
    ) + fadeIn(animationSpec = tween(220, easing = EaseOutCubic))
}

fun navModalExit(reduceMotion: Boolean = false): ExitTransition {
    if (reduceMotion) return fadeOut(animationSpec = tween(0))
    return fadeOut(animationSpec = tween(160, easing = EaseOutCubic))
}

fun navModalPopEnter(reduceMotion: Boolean = false): EnterTransition =
    fadeIn(animationSpec = tween(motionDurationMs(reduceMotion, 160), easing = EaseOutCubic))

fun navModalPopExit(reduceMotion: Boolean = false): ExitTransition {
    if (reduceMotion) return fadeOut(animationSpec = tween(0))
    return slideOutVertically(
        animationSpec = tween(240, easing = EaseOutCubic),
        targetOffsetY = { fullHeight -> fullHeight / 5 },
    ) + fadeOut(animationSpec = tween(160, easing = EaseOutCubic)) +
        scaleOut(
            animationSpec = tween(240, easing = EaseOutCubic),
            targetScale = 0.94f,
            transformOrigin = TransformOrigin.Center,
        )
}

/**
 * Fade-through for shared-element destinations (list ↔ load detail).
 * Shared bounds morph the card; non-shared chrome fades instead of sliding.
 */
fun navSharedElementEnter(reduceMotion: Boolean = false): EnterTransition {
    if (reduceMotion) return fadeIn(animationSpec = tween(0))
    return fadeIn(animationSpec = tween(220, easing = EaseOutCubic)) +
        scaleIn(
            animationSpec = tween(280, easing = EaseOutCubic),
            initialScale = 0.96f,
            transformOrigin = TransformOrigin.Center,
        )
}

/** Shared-bounds destinations: light scale so predictive back can seek the peek. */
fun navSharedElementExit(reduceMotion: Boolean = false): ExitTransition {
    if (reduceMotion) return fadeOut(animationSpec = tween(0))
    return fadeOut(animationSpec = tween(180, easing = EaseOutCubic)) +
        scaleOut(
            animationSpec = tween(280, easing = EaseOutCubic),
            targetScale = 0.94f,
            transformOrigin = TransformOrigin.Center,
        )
}

fun screenEnterAnimation(reduceMotion: Boolean = false) =
    fadeIn(
        animationSpec = tween(
            motionDurationMs(reduceMotion, 160),
            easing = EaseOutCubic,
        ),
    )

fun staggeredListEnter(index: Int, reduceMotion: Boolean = false) =
    if (reduceMotion) {
        fadeIn(animationSpec = tween(0))
    } else {
        fadeIn(
            animationSpec = tween(
                180,
                delayMillis = (index * 20).coerceAtMost(120),
                easing = EaseOutCubic,
            ),
        ) + scaleIn(
            animationSpec = tween(
                180,
                delayMillis = (index * 20).coerceAtMost(120),
                easing = EaseOutCubic,
            ),
            initialScale = 0.98f,
        )
    }

@Composable
fun StaggeredAnimatedItem(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val reduceMotion = LocalReduceMotion.current
    AnimatedVisibility(
        visible = true,
        enter = staggeredListEnter(index, reduceMotion),
        modifier = modifier,
    ) {
        content()
    }
}

fun Modifier.neoGlassPressScale(): Modifier = pressScale()

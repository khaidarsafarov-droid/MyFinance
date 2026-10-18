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

/** Shared-axis push / pop. Material 3 emphasized duration band is 300–350ms. */
const val MotionPushDurationMs = 320

/** Fade-through for top-level tabs. */
const val MotionTabDurationMs = 250

private const val TAB_FADE_OUT_MS = 90
private const val TAB_FADE_DELAY_MS = 50
private const val TAB_FADE_IN_MS = MotionTabDurationMs - TAB_FADE_DELAY_MS

/** ~30dp on a 400dp-wide window, without needing density inside a transition lambda. */
private fun sharedAxisOffsetPx(fullSizePx: Int): Int =
    (fullSizePx * 30 / 400).coerceAtLeast(1)

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
            animationSpec = tween(MotionPushDurationMs, easing = MotionEasingEmphasizedDecelerate),
            initialScale = 0.92f,
            transformOrigin = TransformOrigin(0.9f, 1f),
        )
}

fun navAddLoadExit(reduceMotion: Boolean = false): ExitTransition {
    if (reduceMotion) return fadeOut(animationSpec = tween(0))
    return fadeOut(animationSpec = tween(180, easing = MotionEasingEmphasizedAccelerate)) +
        scaleOut(
            animationSpec = tween(MotionPushDurationMs, easing = MotionEasingEmphasizedAccelerate),
            targetScale = 0.92f,
            transformOrigin = TransformOrigin(0.9f, 1f),
        )
}

fun motionDurationMs(reduceMotion: Boolean, normalMs: Int): Int =
    if (reduceMotion) 0 else normalMs

/**
 * Material fade-through. Outgoing content fades first; incoming content
 * fades (and settles) after a short gap so two heavy screens are not
 * both fully opaque while sliding.
 */
fun navFadeThroughEnter(reduceMotion: Boolean = false): EnterTransition {
    if (reduceMotion) return fadeIn(animationSpec = tween(0))
    return fadeIn(
        animationSpec = tween(
            durationMillis = TAB_FADE_IN_MS,
            delayMillis = TAB_FADE_DELAY_MS,
            easing = MotionEasingEmphasizedDecelerate,
        ),
    )
}

fun navFadeThroughExit(reduceMotion: Boolean = false): ExitTransition {
    if (reduceMotion) return fadeOut(animationSpec = tween(0))
    return fadeOut(
        animationSpec = tween(
            durationMillis = TAB_FADE_OUT_MS,
            easing = MotionEasingEmphasizedAccelerate,
        ),
    )
}

fun tabEnterTransition(reduceMotion: Boolean = false): EnterTransition =
    navFadeThroughEnter(reduceMotion)

fun tabExitTransition(reduceMotion: Boolean = false): ExitTransition =
    navFadeThroughExit(reduceMotion)

/**
 * Top-level tabs (Home, Goal, Profile). Fade-through only.
 * A horizontal slide of Home was transforming the whole journal — shared-element
 * cards included — on the same frames Weekly Goal first composed.
 * [slideSign] stays in the signature so call sites can still describe direction.
 */
@Suppress("UNUSED_PARAMETER")
fun mainTabEnter(reduceMotion: Boolean, slideSign: Int): EnterTransition =
    navFadeThroughEnter(reduceMotion)

@Suppress("UNUSED_PARAMETER")
fun mainTabExit(reduceMotion: Boolean, slideSign: Int): ExitTransition =
    navFadeThroughExit(reduceMotion)

/**
 * Material shared-axis X, forward. About 30dp of travel, emphasized easing.
 * Reduce-motion is an instant fade.
 */
fun navForwardEnter(reduceMotion: Boolean = false): EnterTransition {
    if (reduceMotion) return fadeIn(animationSpec = tween(0))
    return slideInHorizontally(
        animationSpec = tween(MotionPushDurationMs, easing = MotionEasingEmphasizedDecelerate),
        initialOffsetX = { fullWidth -> sharedAxisOffsetPx(fullWidth) },
    ) + fadeIn(
        animationSpec = tween(MotionPushDurationMs, easing = MotionEasingEmphasizedDecelerate),
    )
}

fun navForwardExit(reduceMotion: Boolean = false): ExitTransition {
    if (reduceMotion) return fadeOut(animationSpec = tween(0))
    return slideOutHorizontally(
        animationSpec = tween(MotionPushDurationMs, easing = MotionEasingEmphasizedAccelerate),
        targetOffsetX = { fullWidth -> -sharedAxisOffsetPx(fullWidth) },
    ) + fadeOut(
        animationSpec = tween(200, easing = MotionEasingEmphasizedAccelerate),
    )
}

/**
 * Shared-axis X, back. Navigation seeks this spec for predictive back,
 * so the outgoing screen travels the opposite way from a forward push.
 */
fun navPopEnter(reduceMotion: Boolean = false): EnterTransition {
    if (reduceMotion) return fadeIn(animationSpec = tween(0))
    return slideInHorizontally(
        animationSpec = tween(MotionPushDurationMs, easing = MotionEasingEmphasizedDecelerate),
        initialOffsetX = { fullWidth -> -sharedAxisOffsetPx(fullWidth) },
    ) + fadeIn(
        animationSpec = tween(MotionPushDurationMs, easing = MotionEasingEmphasizedDecelerate),
    )
}

fun navPopExit(reduceMotion: Boolean = false): ExitTransition {
    if (reduceMotion) return fadeOut(animationSpec = tween(0))
    return slideOutHorizontally(
        animationSpec = tween(MotionPushDurationMs, easing = MotionEasingEmphasizedAccelerate),
        targetOffsetX = { fullWidth -> sharedAxisOffsetPx(fullWidth) },
    ) + fadeOut(
        animationSpec = tween(200, easing = MotionEasingEmphasizedAccelerate),
    )
}

/** Modal / immersive: camera, scanner, galleries, sheets that are routes. Shared-axis Y. */
fun navModalEnter(reduceMotion: Boolean = false): EnterTransition {
    if (reduceMotion) return fadeIn(animationSpec = tween(0))
    return slideInVertically(
        animationSpec = tween(MotionPushDurationMs, easing = MotionEasingEmphasizedDecelerate),
        initialOffsetY = { fullHeight -> sharedAxisOffsetPx(fullHeight) },
    ) + fadeIn(
        animationSpec = tween(MotionPushDurationMs, easing = MotionEasingEmphasizedDecelerate),
    )
}

fun navModalExit(reduceMotion: Boolean = false): ExitTransition {
    if (reduceMotion) return fadeOut(animationSpec = tween(0))
    return fadeOut(
        animationSpec = tween(TAB_FADE_OUT_MS, easing = MotionEasingEmphasizedAccelerate),
    )
}

fun navModalPopEnter(reduceMotion: Boolean = false): EnterTransition =
    fadeIn(
        animationSpec = tween(
            motionDurationMs(reduceMotion, TAB_FADE_IN_MS),
            easing = MotionEasingEmphasizedDecelerate,
        ),
    )

fun navModalPopExit(reduceMotion: Boolean = false): ExitTransition {
    if (reduceMotion) return fadeOut(animationSpec = tween(0))
    return slideOutVertically(
        animationSpec = tween(MotionPushDurationMs, easing = MotionEasingEmphasizedAccelerate),
        targetOffsetY = { fullHeight -> sharedAxisOffsetPx(fullHeight) },
    ) + fadeOut(
        animationSpec = tween(200, easing = MotionEasingEmphasizedAccelerate),
    )
}

/**
 * Container transform chrome for routes that already morph a shared element
 * (load card → detail). Fade only — a slide or scale here fights the shared bounds.
 */
fun navSharedElementEnter(reduceMotion: Boolean = false): EnterTransition =
    navFadeThroughEnter(reduceMotion)

fun navSharedElementExit(reduceMotion: Boolean = false): ExitTransition =
    navFadeThroughExit(reduceMotion)

/** Dialog / bottom-sheet motion. Same durations as a modal route; instant when reduced. */
fun navDialogEnter(reduceMotion: Boolean = false): EnterTransition = navModalEnter(reduceMotion)

fun navDialogExit(reduceMotion: Boolean = false): ExitTransition = navModalPopExit(reduceMotion)

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

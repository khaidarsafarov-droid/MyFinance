package com.truckerload.presentation.theme

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.ContentScale

/** Shared-bounds key for load list card ↔ detail route header. */
fun loadSharedBoundsKey(loadId: String): String = "load-bounds-$loadId"

/** Shared-bounds key for the home add-load FAB ↔ add form container transform. */
const val ADD_LOAD_SHARED_KEY = "add-load-container"

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

val LocalNavAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Provides navigation scopes so list/detail children can attach [loadSharedBounds]
 * without threading SharedTransitionScope through every screen.
 */
@Composable
fun ProvideLoadSharedElementScopes(
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalSharedTransitionScope provides sharedTransitionScope,
        LocalNavAnimatedVisibilityScope provides animatedVisibilityScope,
        content = content,
    )
}

/**
 * Container-transform style shared bounds for a load card / detail header.
 * No-op when scopes are missing, [loadId] is blank, or reduce-motion is on.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.loadSharedBounds(loadId: String): Modifier =
    sharedBoundsModifier(loadSharedBoundsKey(loadId), enabled = loadId.isNotBlank())

fun Modifier.addLoadSharedBounds(): Modifier =
    sharedBoundsModifier(ADD_LOAD_SHARED_KEY, enabled = true)

@OptIn(ExperimentalSharedTransitionApi::class)
private fun Modifier.sharedBoundsModifier(key: String, enabled: Boolean): Modifier = composed {
    val sharedScope = LocalSharedTransitionScope.current
    val animatedScope = LocalNavAnimatedVisibilityScope.current
    val reduceMotion = LocalReduceMotion.current
    if (sharedScope == null || animatedScope == null || reduceMotion || !enabled) {
        this
    } else {
        with(sharedScope) {
            sharedBounds(
                sharedContentState = rememberSharedContentState(key = key),
                animatedVisibilityScope = animatedScope,
                boundsTransform = { _, _ ->
                    tween(durationMillis = 450, easing = MotionEasingEmphasizedDecelerate)
                },
                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                    contentScale = ContentScale.FillWidth,
                    alignment = Alignment.TopCenter,
                ),
                clipInOverlayDuringTransition = OverlayClip(
                    RoundedCornerShape(BentoGlassTheme.CardRadius),
                ),
            )
        }
    }
}

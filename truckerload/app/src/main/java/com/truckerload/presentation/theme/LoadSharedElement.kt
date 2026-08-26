package com.truckerload.presentation.theme

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.EaseOutCubic
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
fun Modifier.loadSharedBounds(loadId: String): Modifier = composed {
    val sharedScope = LocalSharedTransitionScope.current
    val animatedScope = LocalNavAnimatedVisibilityScope.current
    val reduceMotion = LocalReduceMotion.current
    if (sharedScope == null || animatedScope == null || reduceMotion || loadId.isBlank()) {
        this
    } else {
        with(sharedScope) {
            sharedBounds(
                sharedContentState = rememberSharedContentState(key = loadSharedBoundsKey(loadId)),
                animatedVisibilityScope = animatedScope,
                boundsTransform = { _, _ ->
                    tween(durationMillis = 320, easing = EaseOutCubic)
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

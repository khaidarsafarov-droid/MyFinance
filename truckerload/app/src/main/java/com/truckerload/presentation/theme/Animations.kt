package com.truckerload.presentation.theme

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext

/** True when the user opted into reduce-motion or the system animator scale is off. */
val LocalReduceMotion = compositionLocalOf { false }

@Composable
fun rememberSystemReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
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

fun Modifier.neoGlassPressScale(): Modifier = composed {
    val reduceMotion = LocalReduceMotion.current
    var pressed by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(
        targetValue = if (pressed && !reduceMotion) 0.95f else 1f,
        animationSpec = tween(motionDurationMs(reduceMotion, 120)),
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

package com.truckerload.presentation.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Material 3 layout tokens. Cards stay 12–16 dp, buttons 16 dp.
 * The name is historical; these are no longer One UI radii.
 */
object OneUiTokens {
    val CornerChip = 8.dp
    val CornerButton = 16.dp
    val CornerCard = 12.dp
    val CornerCardLarge = 16.dp
    val CornerBottomBar = 0.dp

    val ScreenHorizontal = 24.dp
    val TitleTopPadding = 8.dp
    val TitleBottomPadding = 16.dp
    val BottomActionPadding = 16.dp
    val BottomBarHorizontalInset = 16.dp
    val BottomBarBottomInset = 8.dp
    val CardGap = 12.dp

    val LargeTitleSize = 32.sp
    val LargeTitleLineHeight = 38.sp
    val SubtitleSize = 14.sp

    val ChipShape = RoundedCornerShape(CornerChip)
    val ButtonShape = RoundedCornerShape(CornerButton)
    val CardShape = RoundedCornerShape(CornerCard)
    val CardLargeShape = RoundedCornerShape(CornerCardLarge)
    val BottomBarShape = RoundedCornerShape(CornerBottomBar)

    /** Material emphasized easing. */
    val MotionEasing: Easing = CubicBezierEasing(0.33f, 0.0f, 0.10f, 1.0f)
    const val MotionShortMs = 200
    const val MotionMediumMs = 350

    fun <T> motionSpec(reduced: Boolean, durationMs: Int = MotionMediumMs): FiniteAnimationSpec<T> {
        val duration = if (reduced) 0 else durationMs
        val easing = if (reduced) FastOutSlowInEasing else MotionEasing
        return tween(durationMillis = duration, easing = easing)
    }

    fun isCornerInOneUiRange(dp: Float): Boolean = dp in 8f..16f
}

/** True-black OLED override that still keeps Material You accents. */
fun overlayOledIfNeeded(scheme: ColorScheme, oled: Boolean): ColorScheme {
    if (!oled) return scheme
    return scheme.copy(
        background = SoftUiColors.BackgroundOled,
        surface = SoftUiColors.SurfaceOled,
        surfaceVariant = SoftUiColors.SurfaceMutedOled,
        primaryContainer = SoftUiColors.SurfaceMutedOled,
        secondaryContainer = SoftUiColors.SurfaceMutedOled,
    )
}

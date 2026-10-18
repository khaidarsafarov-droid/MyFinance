package com.truckerload.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.truckerload.domain.goal.PaceStatus

/**
 * Cabin-readable semantic tokens for the kit palette.
 *
 * Success / Warning / Danger / Neutral are the only status hues used in the UI.
 * Values are chosen so **text on the matching surface** meets WCAG AA, and
 * [hero] on [SoftUiColors.SurfaceLight] / dark background meets AAA.
 * Kit fills (teal / orange / pink) stay on charts; these are the text hues.
 */
data class SemanticPalette(
    val success: Color,
    val warning: Color,
    val danger: Color,
    val neutral: Color,
    val successContainer: Color,
    val warningContainer: Color,
    val dangerContainer: Color,
    val neutralContainer: Color,
    val hero: Color,
    /** Full-screen photo stage. Dark in both themes so the picture stays readable. */
    val mediaScrim: Color,
    val onMedia: Color,
)

object SemanticColors {
    /** Light cabin / daylight. */
    val Light = SemanticPalette(
        success = Color(0xFF0B7A3E),
        warning = Color(0xFF8F4C00),
        danger = Color(0xFFC4314B),
        neutral = SoftUiColors.TextSecondaryLight,
        successContainer = Color(0xFFDDF5E8),
        warningContainer = Color(0xFFFFEFD6),
        dangerContainer = Color(0xFFFDE7EB),
        neutralContainer = Color(0xFFEEEDFF),
        hero = SoftUiColors.TextPrimaryLight,
        mediaScrim = Color(0xFF000000),
        onMedia = SoftUiColors.TextPrimaryDark,
    )

    /** Dedicated dark tokens — not an invert of Light. */
    val Dark = SemanticPalette(
        success = Color(0xFF5FD69B),
        warning = Color(0xFFF5C26B),
        danger = Color(0xFFFF8A9B),
        neutral = SoftUiColors.TextSecondaryDark,
        successContainer = Color(0xFF163A2A),
        warningContainer = Color(0xFF3D2F12),
        dangerContainer = Color(0xFF42182A),
        neutralContainer = Color(0xFF2E2A66),
        hero = SoftUiColors.TextPrimaryDark,
        mediaScrim = Color(0xFF000000),
        onMedia = SoftUiColors.TextPrimaryDark,
    )

    fun forDark(dark: Boolean): SemanticPalette = if (dark) Dark else Light
}

fun SemanticPalette.pace(status: PaceStatus): Color = when (status) {
    PaceStatus.GOAL_MET, PaceStatus.AHEAD -> success
    PaceStatus.ON_TRACK -> warning
    PaceStatus.BEHIND -> danger
}

fun SemanticPalette.dispute(active: Boolean, resolved: Boolean): Color = when {
    resolved -> success
    active -> danger
    else -> neutral
}

enum class SyncUiStatus { SUCCESS, WARNING, DANGER, NEUTRAL, IDLE }

fun SemanticPalette.sync(status: SyncUiStatus): Color = when (status) {
    SyncUiStatus.SUCCESS -> success
    SyncUiStatus.WARNING -> warning
    SyncUiStatus.DANGER -> danger
    SyncUiStatus.NEUTRAL, SyncUiStatus.IDLE -> neutral
}

val LocalSemanticColors = staticCompositionLocalOf { SemanticColors.Light }

object LocalSemantics {
    val current: SemanticPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalSemanticColors.current
}

/** WCAG contrast helpers for theme tests and audits. */
object ContrastRatios {
    const val AA_NORMAL = 4.5
    const val AA_LARGE = 3.0
    const val AAA_NORMAL = 7.0
    const val AAA_LARGE = 4.5

    fun contrast(foreground: Color, background: Color): Double {
        val l1 = foreground.luminance().toDouble()
        val l2 = background.luminance().toDouble()
        val light = maxOf(l1, l2)
        val dark = minOf(l1, l2)
        return (light + 0.05) / (dark + 0.05)
    }

    fun passesAa(foreground: Color, background: Color, largeText: Boolean = false): Boolean =
        contrast(foreground, background) >= if (largeText) AA_LARGE else AA_NORMAL

    fun passesAaa(foreground: Color, background: Color, largeText: Boolean = false): Boolean =
        contrast(foreground, background) >= if (largeText) AAA_LARGE else AAA_NORMAL
}

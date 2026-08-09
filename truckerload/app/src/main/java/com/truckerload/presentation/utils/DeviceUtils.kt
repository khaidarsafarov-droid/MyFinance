package com.truckerload.presentation.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Window width buckets aligned with Material 3:
 * - [COMPACT] &lt; 600dp — phones
 * - [MEDIUM] 600–839dp — portrait tablets, foldables
 * - [EXPANDED] ≥ 840dp — landscape tablets / desktop
 */
enum class WindowSizeClass {
    COMPACT,
    MEDIUM,
    EXPANDED,
}

object WindowBreakpoints {
    const val COMPACT_MAX = 599
    const val MEDIUM_MIN = 600
    const val EXPANDED_MIN = 840
    const val XL_MIN = 1280
}

/** Pure width classifier for unit tests and non-Compose callers. */
fun windowSizeClassForWidth(widthDp: Int): WindowSizeClass = when {
    widthDp >= WindowBreakpoints.EXPANDED_MIN -> WindowSizeClass.EXPANDED
    widthDp >= WindowBreakpoints.MEDIUM_MIN -> WindowSizeClass.MEDIUM
    else -> WindowSizeClass.COMPACT
}

/** Side-by-side list/detail when the content pane is wide enough. */
fun useTwoPaneForWidth(widthDp: Int): Boolean =
    windowSizeClassForWidth(widthDp) == WindowSizeClass.EXPANDED

@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val width = LocalConfiguration.current.screenWidthDp
    return remember(width) { windowSizeClassForWidth(width) }
}

@Composable
fun rememberScreenWidthDp(): Int = LocalConfiguration.current.screenWidthDp

@Composable
fun isLandscape(): Boolean {
    val config = LocalConfiguration.current
    return remember(config.screenWidthDp, config.screenHeightDp) {
        config.screenWidthDp > config.screenHeightDp
    }
}

/** Width ≥ 600dp — tablet-class device (portrait 7"+ or wide phone). */
@Composable
fun isTabletClassDevice(): Boolean = rememberWindowSizeClass() != WindowSizeClass.COMPACT

/**
 * Fixed start sidebar: expanded width, or medium width in landscape.
 * Portrait tablets keep bottom navigation + swipe drawer.
 */
@Composable
fun useNavigationRail(): Boolean {
    val sizeClass = rememberWindowSizeClass()
    val landscape = isLandscape()
    return sizeClass == WindowSizeClass.EXPANDED ||
        (sizeClass == WindowSizeClass.MEDIUM && landscape)
}

/**
 * Legacy name used across the app. Prefer [useNavigationRail] / [isTabletClassDevice].
 * True when the fixed start sidebar should be shown.
 */
@Composable
fun isTablet(): Boolean = useNavigationRail()

@Composable
fun isFoldable(): Boolean = rememberWindowSizeClass() == WindowSizeClass.MEDIUM

/** List | detail panes (Expanded / landscape tablets). */
@Composable
fun useTwoPaneLayout(): Boolean = rememberWindowSizeClass() == WindowSizeClass.EXPANDED

/** Card / metric grid columns: 1 phone, 2 portrait tablet, 3 landscape tablet. */
@Composable
fun adaptiveCardColumns(): Int {
    val sizeClass = rememberWindowSizeClass()
    val landscape = isLandscape()
    return when {
        sizeClass == WindowSizeClass.EXPANDED && landscape -> 3
        sizeClass == WindowSizeClass.EXPANDED || sizeClass == WindowSizeClass.MEDIUM -> 2
        else -> 1
    }
}

/**
 * Journal load cards stay at most 2 columns — content is too dense for 3 columns.
 */
@Composable
fun adaptiveLoadColumns(): Int = adaptiveCardColumns().coerceAtMost(2)

@Composable
fun adaptiveHorizontalPadding(): Dp = when (rememberWindowSizeClass()) {
    WindowSizeClass.EXPANDED -> 24.dp
    WindowSizeClass.MEDIUM -> 20.dp
    WindowSizeClass.COMPACT -> 16.dp
}

@Composable
fun adaptiveVerticalPadding(): Dp = when (rememberWindowSizeClass()) {
    WindowSizeClass.EXPANDED -> 16.dp
    WindowSizeClass.MEDIUM -> 12.dp
    WindowSizeClass.COMPACT -> 8.dp
}

@Composable
fun adaptiveTitleFontSize() = when (rememberWindowSizeClass()) {
    WindowSizeClass.EXPANDED -> 32.sp
    WindowSizeClass.MEDIUM -> 28.sp
    WindowSizeClass.COMPACT -> 24.sp
}

@Composable
fun Modifier.adaptiveContentWidth(): Modifier {
    val sizeClass = rememberWindowSizeClass()
    val rail = useNavigationRail()
    return then(
        when {
            // Content already sits beside the sidebar — use the full pane.
            rail -> fillMaxWidth()
            sizeClass == WindowSizeClass.EXPANDED -> fillMaxWidth().widthIn(max = 1366.dp)
            sizeClass == WindowSizeClass.MEDIUM -> fillMaxWidth().widthIn(max = 840.dp)
            else -> fillMaxWidth()
        },
    )
}

@Composable
fun Modifier.adaptiveScreenPadding(): Modifier =
    padding(horizontal = adaptiveHorizontalPadding())

/**
 * Centers content on wide phone-layout screens; fills the pane when a sidebar is present.
 */
@Composable
fun AdaptiveScreenContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val sizeClass = rememberWindowSizeClass()
    val rail = useNavigationRail()
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = if (!rail && sizeClass != WindowSizeClass.COMPACT) {
            Alignment.TopCenter
        } else {
            Alignment.TopStart
        },
    ) {
        Box(
            modifier = Modifier
                .adaptiveContentWidth()
                .fillMaxSize(),
            content = content,
        )
    }
}

@Composable
fun adaptiveContentPadding(base: PaddingValues): PaddingValues {
    val horizontal = adaptiveHorizontalPadding()
    return PaddingValues(
        start = base.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr) + horizontal,
        top = base.calculateTopPadding(),
        end = base.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr) + horizontal,
        bottom = base.calculateBottomPadding(),
    )
}

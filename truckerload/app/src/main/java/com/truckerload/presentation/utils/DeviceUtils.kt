package com.truckerload.presentation.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
 * Window width buckets aligned with Material 3 and common tablet ranges:
 * - [COMPACT] &lt; 600dp — phones
 * - [MEDIUM] 600–839dp — portrait tablets (~7–10"), foldables, large phones in landscape
 * - [EXPANDED] ≥ 840dp — landscape tablets (~10–13"), iPad Pro, split-screen halves on wide devices
 */
enum class WindowSizeClass {
    COMPACT,
    MEDIUM,
    EXPANDED,
}

/** Pure width classifier for unit tests and non-Compose callers. */
fun windowSizeClassForWidth(widthDp: Int): WindowSizeClass = when {
    widthDp >= WindowBreakpoints.EXPANDED_MIN -> WindowSizeClass.EXPANDED
    widthDp >= WindowBreakpoints.MEDIUM_MIN -> WindowSizeClass.MEDIUM
    else -> WindowSizeClass.COMPACT
}

object WindowBreakpoints {
    /** ~ Tailwind `sm` / phone landscape threshold. */
    const val COMPACT_MAX = 599

    /** ~ Tailwind `md` / portrait tablet start (768px class devices ≈ 600–840dp). */
    const val MEDIUM_MIN = 600

    /** ~ Tailwind `lg` / landscape tablet start (1024px class). */
    const val EXPANDED_MIN = 840

    /** ~ Tailwind `xl` — used for max content width caps. */
    const val XL_MIN = 1280
}

@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val width = LocalConfiguration.current.screenWidthDp
    return remember(width) { windowSizeClassForWidth(width) }
}

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
 * Fixed navigation rail: expanded width or medium width in landscape.
 * Portrait tablets use bottom navigation + swipe drawer instead.
 */
@Composable
fun useNavigationRail(): Boolean {
    val sizeClass = rememberWindowSizeClass()
    val landscape = isLandscape()
    return sizeClass == WindowSizeClass.EXPANDED ||
        (sizeClass == WindowSizeClass.MEDIUM && landscape)
}

/** @deprecated Prefer [useNavigationRail] or [isTabletClassDevice] for clarity. */
@Composable
fun isTablet(): Boolean = useNavigationRail()

@Composable
fun isFoldable(): Boolean = rememberWindowSizeClass() == WindowSizeClass.MEDIUM

@Composable
fun adaptiveHorizontalPadding(): Dp = when (rememberWindowSizeClass()) {
    WindowSizeClass.EXPANDED -> 32.dp
    WindowSizeClass.MEDIUM -> 24.dp
    WindowSizeClass.COMPACT -> 16.dp
}

@Composable
fun adaptiveVerticalPadding(): Dp = when (rememberWindowSizeClass()) {
    WindowSizeClass.EXPANDED -> 24.dp
    WindowSizeClass.MEDIUM -> 16.dp
    WindowSizeClass.COMPACT -> 8.dp
}

@Composable
fun adaptiveTitleFontSize() = when (rememberWindowSizeClass()) {
    WindowSizeClass.EXPANDED -> 32.sp
    WindowSizeClass.MEDIUM -> 28.sp
    WindowSizeClass.COMPACT -> 24.sp
}

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

/** Min cell size for adaptive photo grids on tablets. */
@Composable
fun adaptiveGalleryMinCellSize(): Dp {
    val columns = adaptiveCardColumns()
    return when (columns) {
        3 -> 160.dp
        2 -> 140.dp
        else -> 120.dp
    }
}

@Composable
fun Modifier.adaptiveContentWidth(): Modifier {
    val sizeClass = rememberWindowSizeClass()
    return then(
        when (sizeClass) {
            WindowSizeClass.EXPANDED -> fillMaxWidth(0.95f).widthIn(max = 1200.dp)
            WindowSizeClass.MEDIUM -> fillMaxWidth(0.98f).widthIn(max = 840.dp)
            WindowSizeClass.COMPACT -> fillMaxWidth()
        },
    )
}

@Composable
fun Modifier.adaptiveScreenPadding(): Modifier =
    padding(horizontal = adaptiveHorizontalPadding())

/**
 * Centers content on wide screens; keeps full width on phones.
 */
@Composable
fun AdaptiveScreenContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val sizeClass = rememberWindowSizeClass()
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = if (sizeClass == WindowSizeClass.EXPANDED) {
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

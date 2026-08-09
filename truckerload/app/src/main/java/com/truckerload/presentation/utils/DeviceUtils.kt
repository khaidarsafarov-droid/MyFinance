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
 * Material-aligned width buckets:
 * - Compact  <600dp  — phones
 * - Medium   600–839 — portrait tablets, foldables, phones in landscape
 * - Expanded ≥840    — landscape tablets, wide split-screen panes
 */
enum class WindowSizeClass {
    COMPACT,
    MEDIUM,
    EXPANDED,
}

/** Pure width → size-class mapping (unit-testable, no Compose). */
fun windowSizeClassForWidth(widthDp: Int): WindowSizeClass = when {
    widthDp >= 840 -> WindowSizeClass.EXPANDED
    widthDp >= 600 -> WindowSizeClass.MEDIUM
    else -> WindowSizeClass.COMPACT
}

/** Tablet-class window (portrait tablet or wide pane). */
fun isTabletClassWidth(widthDp: Int): Boolean =
    windowSizeClassForWidth(widthDp) != WindowSizeClass.COMPACT

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

/** Width ≥ 600dp — tablet route chrome, wider padding, etc. */
@Composable
fun isTablet(): Boolean = rememberWindowSizeClass() != WindowSizeClass.COMPACT

/**
 * Fixed navigation rail: landscape tablets and any Expanded width.
 * Portrait tablets keep the bottom bar so content uses the full width.
 */
@Composable
fun useNavigationRail(): Boolean {
    val sizeClass = rememberWindowSizeClass()
    return sizeClass == WindowSizeClass.EXPANDED ||
        (sizeClass == WindowSizeClass.MEDIUM && isLandscape())
}

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

@Composable
fun Modifier.adaptiveContentWidth(): Modifier {
    val sizeClass = rememberWindowSizeClass()
    return then(
        when (sizeClass) {
            WindowSizeClass.EXPANDED -> fillMaxWidth().widthIn(max = 1366.dp)
            WindowSizeClass.MEDIUM -> fillMaxWidth().widthIn(max = 840.dp)
            WindowSizeClass.COMPACT -> fillMaxWidth()
        },
    )
}

@Composable
fun Modifier.adaptiveScreenPadding(): Modifier =
    padding(horizontal = adaptiveHorizontalPadding())

/**
 * Centers and caps content on phones; on tablets with a navigation rail the shell
 * already provides horizontal structure, so callers pass [useFullWidth] = true.
 */
@Composable
fun AdaptiveScreenContainer(
    modifier: Modifier = Modifier,
    useFullWidth: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    if (useFullWidth) {
        Box(modifier = modifier.fillMaxSize(), content = content)
        return
    }
    val sizeClass = rememberWindowSizeClass()
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = if (sizeClass == WindowSizeClass.COMPACT) {
            Alignment.TopStart
        } else {
            Alignment.TopCenter
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

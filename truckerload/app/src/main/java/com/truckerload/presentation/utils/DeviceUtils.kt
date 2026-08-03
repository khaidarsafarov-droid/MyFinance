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
 * Material-aligned width buckets for phone / tablet portrait / tablet landscape.
 *
 * Mapping from common web breakpoints:
 * - Compact  &lt;600dp  ≈ mobile (&lt;768 CSS px at 1x density)
 * - Medium   600–839  ≈ tablet portrait (≈768–1023)
 * - Expanded ≥840     ≈ tablet landscape / desktop (≈1024+)
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

/** Navigation rail / non-phone chrome when width is at least a small tablet. */
fun isTabletWidth(widthDp: Int): Boolean =
    windowSizeClassForWidth(widthDp) != WindowSizeClass.COMPACT

/** Side-by-side list/detail when the content pane is wide enough. */
fun useTwoPaneForWidth(widthDp: Int): Boolean =
    windowSizeClassForWidth(widthDp) == WindowSizeClass.EXPANDED

/** Card / metric grid columns: 1 phone, 2 tablet portrait, 3 tablet landscape. */
fun adaptiveGridColumnsForWidth(
    widthDp: Int,
    compact: Int = 1,
    medium: Int = 2,
    expanded: Int = 3,
): Int = when (windowSizeClassForWidth(widthDp)) {
    WindowSizeClass.EXPANDED -> expanded
    WindowSizeClass.MEDIUM -> medium
    WindowSizeClass.COMPACT -> compact
}

@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val width = LocalConfiguration.current.screenWidthDp
    return remember(width) { windowSizeClassForWidth(width) }
}

@Composable
fun rememberScreenWidthDp(): Int = LocalConfiguration.current.screenWidthDp

/** True for Medium and Expanded (rail navigation, tablet padding). */
@Composable
fun isTablet(): Boolean = rememberWindowSizeClass() != WindowSizeClass.COMPACT

/** Narrow tablet / foldable unfolded width (600–839dp). */
@Composable
fun isFoldable(): Boolean = rememberWindowSizeClass() == WindowSizeClass.MEDIUM

/** List | detail panes (Expanded / landscape tablets). */
@Composable
fun useTwoPaneLayout(): Boolean = rememberWindowSizeClass() == WindowSizeClass.EXPANDED

@Composable
fun adaptiveGridColumns(
    compact: Int = 1,
    medium: Int = 2,
    expanded: Int = 3,
): Int {
    val width = rememberScreenWidthDp()
    return remember(width, compact, medium, expanded) {
        adaptiveGridColumnsForWidth(width, compact, medium, expanded)
    }
}

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
 * Centers content on tablet widths; keeps full width on phones.
 */
@Composable
fun AdaptiveScreenContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
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

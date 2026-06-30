package com.truckerload.presentation.utils

import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize

/** Compact phone, foldable / narrow tablet, full tablet. */
enum class WindowSizeClass {
    COMPACT,
    MEDIUM,
    EXPANDED,
}

@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val width = LocalConfiguration.current.screenWidthDp
    return remember(width) {
        when {
            width >= 600 -> WindowSizeClass.EXPANDED
            width in 400..599 -> WindowSizeClass.MEDIUM
            else -> WindowSizeClass.COMPACT
        }
    }
}

@Composable
fun isTablet(): Boolean = rememberWindowSizeClass() == WindowSizeClass.EXPANDED

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
            WindowSizeClass.EXPANDED -> fillMaxWidth(0.95f).widthIn(max = 1200.dp)
            WindowSizeClass.MEDIUM -> fillMaxWidth(0.98f).widthIn(max = 840.dp)
            WindowSizeClass.COMPACT -> fillMaxWidth()
        }
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

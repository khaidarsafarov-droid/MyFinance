package com.truckerload.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Typography mapped to [MaterialTheme] for accessibility / system font scaling. */
object AppTypography {
    val DisplayOnDark: TextStyle
        @Composable get() = MaterialTheme.typography.displaySmall.copy(
            color = MaterialTheme.colorScheme.onPrimary,
            fontFeatureSettings = "tnum",
        )

    val DisplayMedium: TextStyle
        @Composable get() = MaterialTheme.typography.displaySmall.copy(
            color = MaterialTheme.colorScheme.onBackground,
            fontFeatureSettings = "tnum",
        )

    val ScreenTitle: TextStyle
        @Composable get() = MaterialTheme.typography.headlineSmall.copy(
            color = SoftUiColors.ForestPrimary,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.4.sp,
        )

    val Headline: TextStyle
        @Composable get() = MaterialTheme.typography.titleLarge.copy(
            color = MaterialTheme.colorScheme.onBackground,
        )

    val CardTitle: TextStyle
        @Composable get() = MaterialTheme.typography.titleMedium.copy(
            color = SoftUiColors.ForestPrimary,
            fontWeight = FontWeight.ExtraBold,
        )

    val CardRoute: TextStyle
        @Composable get() = MaterialTheme.typography.titleSmall.copy(
            color = SoftUiColors.ForestPrimary,
            fontWeight = FontWeight.Bold,
        )

    val Body: TextStyle
        @Composable get() = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
        )

    val Subtitle: TextStyle
        @Composable get() = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

    val Caption: TextStyle
        @Composable get() = MaterialTheme.typography.labelMedium.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

    val CaptionMuted: TextStyle
        @Composable get() = MaterialTheme.typography.labelSmall.copy(
            color = MaterialTheme.colorScheme.outline,
        )

    val SectionTitle: TextStyle
        @Composable get() = MaterialTheme.typography.labelLarge.copy(
            color = SoftUiColors.ForestMuted,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.6.sp,
        )

    val NumbersLarge: TextStyle
        @Composable get() = MaterialTheme.typography.headlineMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontFeatureSettings = "tnum",
        )

    val NumbersMetric: TextStyle
        @Composable get() = MaterialTheme.typography.titleLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontFeatureSettings = "tnum",
        )

    val NumbersSmall: TextStyle
        @Composable get() = MaterialTheme.typography.titleMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontFeatureSettings = "tnum",
        )

    val NumbersOnDark: TextStyle
        @Composable get() = MaterialTheme.typography.headlineMedium.copy(
            color = MaterialTheme.colorScheme.onPrimary,
            fontFeatureSettings = "tnum",
        )

    val AccentNumber: TextStyle
        @Composable get() = MaterialTheme.typography.titleLarge.copy(
            color = MaterialTheme.colorScheme.primary,
            fontFeatureSettings = "tnum",
        )

    val AccentNumberSmall: TextStyle
        @Composable get() = MaterialTheme.typography.titleMedium.copy(
            color = MaterialTheme.colorScheme.primary,
            fontFeatureSettings = "tnum",
        )

    val Numbers: TextStyle
        @Composable get() = NumbersMetric
}

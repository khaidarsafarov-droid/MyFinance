package com.truckerload.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Semantic typography — always theme-aware for light/dark. */
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
        @Composable get() = MaterialTheme.typography.headlineMedium.copy(
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
        )

    val Headline: TextStyle
        @Composable get() = MaterialTheme.typography.titleLarge.copy(
            color = MaterialTheme.colorScheme.onBackground,
        )

    val CardTitle: TextStyle
        @Composable get() = MaterialTheme.typography.titleMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold,
        )

    val CardRoute: TextStyle
        @Composable get() = MaterialTheme.typography.titleSmall.copy(
            color = MaterialTheme.colorScheme.onSurface,
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
        @Composable get() = MaterialTheme.typography.bodySmall.copy(
            // AA-safe muted label — never ForestSoft / outlineVariant on light cards.
            color = LocalTruckColors.current.TextLabel,
            fontWeight = FontWeight.Normal,
        )

    /** Week gross, RPM, rate, paycheck total — glanceable from the cab. */
    val HeroNumber: TextStyle
        @Composable get() = MaterialTheme.typography.displayLarge.copy(
            fontWeight = FontWeight.Bold,
            fontFeatureSettings = "tnum",
            color = LocalTruckColors.current.TextNumbers,
        )

    /** Compact hero (load cards, leaderboard rows, widget-sized slots). */
    val HeroNumberCompact: TextStyle
        @Composable get() = MaterialTheme.typography.displayMedium.copy(
            fontWeight = FontWeight.Bold,
            fontFeatureSettings = "tnum",
            color = LocalTruckColors.current.TextNumbers,
        )

    val HeroNumberOnDark: TextStyle
        @Composable get() = HeroNumber.copy(color = MaterialTheme.colorScheme.onPrimary)

    val SectionTitle: TextStyle
        @Composable get() = MaterialTheme.typography.labelLarge.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        @Composable get() = HeroNumberOnDark

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

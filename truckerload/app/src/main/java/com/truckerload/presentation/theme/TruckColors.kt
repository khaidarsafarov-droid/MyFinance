package com.truckerload.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/** Black & Gold premium palette. */
data class TruckColorPalette(
    val Background: Color,
    val CardBackground: Color,
    val SurfaceSecondary: Color,
    val Divider: Color,
    val AccentPrimary: Color,
    val AccentSecondary: Color,
    val AccentExpense: Color,
    val AccentInfo: Color,
    val AccentWarning: Color,
    val AccentProfit: Color,
    val TextPrimary: Color,
    val TextSecondary: Color,
    val TextLabel: Color,
    val TextGold: Color,
    val OnAccent: Color,
    val GlassBorder: Color,
    val ProgressTrack: Color,
)

val TruckLightColors = TruckColorPalette(
    Background = Color(0xFFF7F7F7),
    CardBackground = Color(0xFFFFFFFF),
    SurfaceSecondary = Color(0xFFF0F0F0),
    Divider = Color(0x33C9A84C),
    AccentPrimary = Color(0xFFC9A84C),
    AccentSecondary = Color(0xFFF5D76E),
    AccentExpense = Color(0xFFEF4444),
    AccentInfo = Color(0xFF6B7280),
    AccentWarning = Color(0xFFFF9500),
    AccentProfit = Color(0xFF34C759),
    TextPrimary = Color(0xFF0A0A0A),
    TextSecondary = Color(0xFF6B6B6B),
    TextLabel = Color(0xFF9CA3AF),
    TextGold = Color(0xFFC9A84C),
    OnAccent = Color(0xFF0A0A0A),
    GlassBorder = Color(0x26C9A84C),
    ProgressTrack = Color(0xFFE8E8E8),
)

val TruckDarkColors = TruckColorPalette(
    Background = Color(0xFF0A0A0A),
    CardBackground = Color(0xFF121212),
    SurfaceSecondary = Color(0xFF1A1A1A),
    Divider = Color(0x33C9A84C),
    AccentPrimary = Color(0xFFC9A84C),
    AccentSecondary = Color(0xFFF5D76E),
    AccentExpense = Color(0xFFF87171),
    AccentInfo = Color(0xFF9CA3AF),
    AccentWarning = Color(0xFFFFB020),
    AccentProfit = Color(0xFF34D399),
    TextPrimary = Color(0xFFF5F5F5),
    TextSecondary = Color(0xFF9CA3AF),
    TextLabel = Color(0xFF6B7280),
    TextGold = Color(0xFFF5D76E),
    OnAccent = Color(0xFF0A0A0A),
    GlassBorder = Color(0x33C9A84C),
    ProgressTrack = Color(0xFF1F1F1F),
)

val LocalTruckColors = compositionLocalOf { TruckLightColors }

object FinanceCockpitColors {
    val Background: Color @Composable get() = LocalTruckColors.current.Background
    val GlassCard: Color @Composable get() = LocalTruckColors.current.CardBackground
    val GlassBorder: Color @Composable get() = LocalTruckColors.current.GlassBorder
    val TextPrimary: Color @Composable get() = LocalTruckColors.current.TextPrimary
    val TextSecondary: Color @Composable get() = LocalTruckColors.current.TextSecondary
    val TextMuted: Color @Composable get() = LocalTruckColors.current.TextLabel
    val TextGold: Color @Composable get() = LocalTruckColors.current.TextGold
    val NetProfitStart: Color @Composable get() = LocalTruckColors.current.AccentProfit
    val NetProfitEnd: Color @Composable get() = LocalTruckColors.current.AccentProfit
    val DieselAccent: Color @Composable get() = LocalTruckColors.current.AccentExpense
    val SalaryAccent: Color @Composable get() = LocalTruckColors.current.AccentPrimary
    val ActiveDateBackground: Color @Composable get() = LocalTruckColors.current.AccentPrimary
    val ActiveHighlight: Color @Composable get() = LocalTruckColors.current.OnAccent
    val InactiveDate: Color @Composable get() = LocalTruckColors.current.TextLabel
    val GlowAccent: Color @Composable get() = LocalTruckColors.current.AccentPrimary.copy(alpha = 0.35f)
    val GlowEmerald: Color @Composable get() = LocalTruckColors.current.AccentProfit.copy(alpha = 0.28f)
    @Deprecated("Use GlowAccent", ReplaceWith("GlowAccent"))
    val GlowGold: Color @Composable get() = GlowAccent
    @Deprecated("Use GlowAccent", ReplaceWith("GlowAccent"))
    val GlowIndigo: Color @Composable get() = GlowAccent
}

object GoldGradients {
    val screen: androidx.compose.ui.graphics.Brush
        @Composable get() = androidx.compose.ui.graphics.Brush.verticalGradient(
            listOf(Color(0xFF0A0A0A), Color(0xFF121212)),
        )

    val screenDark: androidx.compose.ui.graphics.Brush
        @Composable get() {
            val tc = LocalTruckColors.current
            return androidx.compose.ui.graphics.Brush.verticalGradient(
                listOf(tc.Background, tc.SurfaceSecondary),
            )
        }

    val horizontal: androidx.compose.ui.graphics.Brush
        @Composable get() {
            val tc = LocalTruckColors.current
            return androidx.compose.ui.graphics.Brush.horizontalGradient(
                listOf(tc.AccentPrimary, tc.AccentSecondary),
            )
        }

    val vertical: androidx.compose.ui.graphics.Brush
        @Composable get() {
            val tc = LocalTruckColors.current
            return androidx.compose.ui.graphics.Brush.verticalGradient(
                listOf(tc.AccentPrimary, tc.AccentSecondary),
            )
        }

    val cardShine: androidx.compose.ui.graphics.Brush
        @Composable get() = androidx.compose.ui.graphics.Brush.verticalGradient(
            listOf(Color(0xFF1A1A1A), Color(0xFF121212)),
        )
}

/** @deprecated Use [GoldGradients] */
object SoftGradients {
    val screen: androidx.compose.ui.graphics.Brush @Composable get() = GoldGradients.screen
    val screenDark: androidx.compose.ui.graphics.Brush @Composable get() = GoldGradients.screenDark
    val horizontal: androidx.compose.ui.graphics.Brush @Composable get() = GoldGradients.horizontal
    val vertical: androidx.compose.ui.graphics.Brush @Composable get() = GoldGradients.vertical
    val cardShine: androidx.compose.ui.graphics.Brush @Composable get() = GoldGradients.cardShine
}

/** @deprecated Use [GoldGradients] */
object BronzeGradients {
    val horizontal: androidx.compose.ui.graphics.Brush @Composable get() = GoldGradients.horizontal
    val vertical: androidx.compose.ui.graphics.Brush @Composable get() = GoldGradients.vertical
}

val StatsCockpitPalette: TruckColorPalette
    @Composable get() = LocalTruckColors.current

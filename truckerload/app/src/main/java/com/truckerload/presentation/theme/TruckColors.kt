package com.truckerload.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.truckerload.domain.goal.PaceStatus

data class TruckColorPalette(
    val Background: Color,
    val BackgroundBottom: Color,
    val CardBackground: Color,
    val SurfaceSecondary: Color,
    val Divider: Color,
    val AccentPrimary: Color,
    val AccentSecondary: Color,
    val AccentExpense: Color,
    val AccentInfo: Color,
    val AccentWarning: Color,
    val AccentProfit: Color,
    val Success: Color,
    val Warning: Color,
    val Danger: Color,
    val Neutral: Color,
    val TextPrimary: Color,
    val TextSecondary: Color,
    val TextLabel: Color,
    val TextNumbers: Color,
    val TextGold: Color,
    val OnAccent: Color,
    val GlassBorder: Color,
    val ProgressTrack: Color,
    val HeroBackground: Color,
    val CreamBackground: Color,
) {
    fun pace(status: PaceStatus): Color = when (status) {
        PaceStatus.GOAL_MET, PaceStatus.AHEAD -> Success
        PaceStatus.ON_TRACK -> Warning
        PaceStatus.BEHIND -> Danger
    }
}

/** Maps [ColorScheme] to the app's semantic palette (RPM, hero, cards, etc.). */
fun truckPaletteFrom(
    colorScheme: ColorScheme,
    semantic: SemanticPalette = SemanticColors.Light,
): TruckColorPalette = TruckColorPalette(
    Background = colorScheme.background,
    BackgroundBottom = colorScheme.background,
    CardBackground = colorScheme.surface,
    SurfaceSecondary = colorScheme.surfaceVariant,
    Divider = colorScheme.outline,
    AccentPrimary = colorScheme.primary,
    AccentSecondary = colorScheme.tertiary,
    AccentExpense = semantic.danger,
    AccentInfo = semantic.neutral,
    AccentWarning = semantic.warning,
    AccentProfit = semantic.success,
    Success = semantic.success,
    Warning = semantic.warning,
    Danger = semantic.danger,
    Neutral = semantic.neutral,
    TextPrimary = colorScheme.onBackground,
    TextSecondary = semantic.neutral,
    // Labels stay AA on white cards — never outlineVariant (border mint).
    TextLabel = semantic.neutral,
    TextNumbers = semantic.hero,
    TextGold = colorScheme.primary,
    OnAccent = colorScheme.onPrimary,
    GlassBorder = colorScheme.outline,
    ProgressTrack = colorScheme.surfaceVariant.copy(alpha = 0.65f),
    HeroBackground = colorScheme.primary,
    CreamBackground = colorScheme.secondaryContainer,
)

private val DefaultTruckPalette = truckPaletteFrom(
    androidx.compose.material3.lightColorScheme()
)

val LocalTruckColors = compositionLocalOf { DefaultTruckPalette }

object FinanceCockpitColors {
    val Background: Color @Composable get() = LocalTruckColors.current.Background
    val GlassCard: Color @Composable get() = LocalTruckColors.current.CardBackground
    val GlassBorder: Color @Composable get() = LocalTruckColors.current.GlassBorder
    val TextPrimary: Color @Composable get() = LocalTruckColors.current.TextPrimary
    val TextSecondary: Color @Composable get() = LocalTruckColors.current.TextSecondary
    val TextMuted: Color @Composable get() = LocalTruckColors.current.TextLabel
    val TextNumbers: Color @Composable get() = LocalTruckColors.current.TextNumbers
    val TextGold: Color @Composable get() = LocalTruckColors.current.TextGold
    val NetProfitStart: Color @Composable get() = LocalTruckColors.current.AccentProfit
    val NetProfitEnd: Color @Composable get() = LocalTruckColors.current.AccentProfit
    val DieselAccent: Color @Composable get() = LocalTruckColors.current.AccentExpense
    val SalaryAccent: Color @Composable get() = LocalTruckColors.current.AccentPrimary
    val ActiveDateBackground: Color @Composable get() = LocalTruckColors.current.AccentPrimary
    val ActiveHighlight: Color @Composable get() = LocalTruckColors.current.OnAccent
    val InactiveDate: Color @Composable get() = LocalTruckColors.current.TextLabel
    val GlowAccent: Color @Composable get() = LocalTruckColors.current.AccentPrimary.copy(alpha = 0.40f)
    val GlowEmerald: Color @Composable get() = LocalTruckColors.current.AccentProfit.copy(alpha = 0.32f)
    @Deprecated("Use GlowAccent", ReplaceWith("GlowAccent"))
    val GlowGold: Color @Composable get() = GlowAccent
    @Deprecated("Use GlowAccent", ReplaceWith("GlowAccent"))
    val GlowIndigo: Color @Composable get() = GlowAccent
}

object DarkGlassGradients {
    val screen: Brush
        @Composable get() {
            val cs = MaterialTheme.colorScheme
            return Brush.verticalGradient(listOf(cs.background, cs.background))
        }

    val screenDark: Brush @Composable get() = screen

        val cta: Brush
        @Composable get() {
            val cs = MaterialTheme.colorScheme
            return Brush.horizontalGradient(listOf(cs.primary, cs.primary))
        }

    val button: Brush @Composable get() = cta
    val ctaGold: Brush @Composable get() = cta
    val horizontal: Brush @Composable get() = cta
        val vertical: Brush
        @Composable get() {
            val cs = MaterialTheme.colorScheme
            return Brush.verticalGradient(listOf(cs.primary, cs.tertiary))
        }

        val cardShine: Brush
        @Composable get() {
            val cs = MaterialTheme.colorScheme
            return Brush.linearGradient(listOf(cs.primary, cs.tertiary))
        }

    val chartFill: Brush
        @Composable get() = Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                Color.Transparent,
            ),
        )

    val progressRing: Brush
        @Composable get() {
            val cs = MaterialTheme.colorScheme
            return Brush.sweepGradient(
                listOf(cs.primary, cs.tertiary, cs.primary),
            )
        }
}

object SoftGradients {
    val screen: Brush @Composable get() = DarkGlassGradients.screen
    val screenDark: Brush @Composable get() = DarkGlassGradients.screenDark
    val horizontal: Brush @Composable get() = DarkGlassGradients.horizontal
    val vertical: Brush @Composable get() = DarkGlassGradients.vertical
    val cardShine: Brush @Composable get() = DarkGlassGradients.cardShine
}

object BronzeGradients {
    val horizontal: Brush @Composable get() = DarkGlassGradients.horizontal
    val vertical: Brush @Composable get() = DarkGlassGradients.vertical
}

val StatsCockpitPalette: TruckColorPalette
    @Composable get() = LocalTruckColors.current

package com.truckerload.presentation.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/** Палитра цветов приложения. Светлая и тёмная — подстраиваются под систему. Синий акцент. */
data class TruckColorPalette(
    val Background: Color,
    val CardBackground: Color,
    val SurfaceSecondary: Color,
    val Divider: Color,
    val AccentPrimary: Color,
    val AccentExpense: Color,
    val AccentInfo: Color,
    val AccentWarning: Color,
    val AccentProfit: Color,
    val TextPrimary: Color,
    val TextSecondary: Color,
    val TextLabel: Color
)

// Единая premium light палитра для всего приложения
val TruckLightColors = TruckColorPalette(
    Background = Color(0xFFF4F7FC),
    CardBackground = Color(0xF2FFFFFF),
    SurfaceSecondary = Color(0xFFE9EEF7),
    Divider = Color(0xFFD7DFEC),
    AccentPrimary = Color(0xFF4F46E5),   // Indigo-600
    AccentExpense = Color(0xFFF43F5E),   // Rose-500
    AccentInfo = Color(0xFF0EA5E9),
    AccentWarning = Color(0xFFF59E0B),
    AccentProfit = Color(0xFF10B981),    // Emerald-500
    TextPrimary = Color(0xFF0F172A),     // Slate-900
    TextSecondary = Color(0xFF475569),   // Slate-600
    TextLabel = Color(0xFF64748B)        // Slate-500
)

// Для консистентного дизайна используем ту же палитру и в dark-моде
val TruckDarkColors = TruckColorPalette(
    Background = TruckLightColors.Background,
    CardBackground = TruckLightColors.CardBackground,
    SurfaceSecondary = TruckLightColors.SurfaceSecondary,
    Divider = TruckLightColors.Divider,
    AccentPrimary = TruckLightColors.AccentPrimary,
    AccentExpense = TruckLightColors.AccentExpense,
    AccentInfo = TruckLightColors.AccentInfo,
    AccentWarning = TruckLightColors.AccentWarning,
    AccentProfit = TruckLightColors.AccentProfit,
    TextPrimary = TruckLightColors.TextPrimary,
    TextSecondary = TruckLightColors.TextSecondary,
    TextLabel = TruckLightColors.TextLabel
)

val LocalTruckColors = compositionLocalOf { TruckDarkColors }

/** Палитра "Digital Cockpit" для экранов Finance/Stats: Zinc-950, glassmorphism, premium accents */
object FinanceCockpitColors {
    val Background = Color(0xFFF4F7FC)            // bright premium background
    val GlassCard = Color(0xF2FFFFFF)             // soft white glass
    val GlassBorder = Color(0xFFE2E8F0)           // light border
    val TextPrimary = Color(0xFF0F172A)           // slate-900
    val TextSecondary = Color(0xFF475569)         // slate-600
    val TextMuted = Color(0xFF64748B)             // slate-500
    val NetProfitStart = Color(0xFF10B981)       // Emerald-500
    val NetProfitEnd = Color(0xFF0D9488)         // Teal-600
    val DieselAccent = Color(0xFFF43F5E)          // Rose-500
    val SalaryAccent = Color(0xFF6366F1)          // Indigo-500
    val ActiveDateBackground = Color(0xFF4F46E5)  // Indigo-600
    val ActiveHighlight = Color(0xFFFAFAFA)       // White for active
    val InactiveDate = Color(0xFFA1A1AA)          // Zinc-400
    val GlowIndigo = Color(0x804F46E5)
    val GlowEmerald = Color(0x6610B981)
}

/** TruckColorPalette adapter for cockpit-themed screens (Stats, etc.) using FinanceCockpitColors */
val StatsCockpitPalette = TruckColorPalette(
    Background = FinanceCockpitColors.Background,
    CardBackground = FinanceCockpitColors.GlassCard,
    SurfaceSecondary = FinanceCockpitColors.GlassBorder,
    Divider = FinanceCockpitColors.GlassBorder,
    AccentPrimary = FinanceCockpitColors.SalaryAccent,
    AccentExpense = FinanceCockpitColors.DieselAccent,
    AccentInfo = FinanceCockpitColors.NetProfitStart,
    AccentWarning = Color(0xFFFFCA28),
    AccentProfit = FinanceCockpitColors.NetProfitStart,
    TextPrimary = FinanceCockpitColors.TextPrimary,
    TextSecondary = FinanceCockpitColors.TextSecondary,
    TextLabel = FinanceCockpitColors.TextMuted
)

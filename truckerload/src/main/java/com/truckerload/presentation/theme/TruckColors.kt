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
    val TextPrimary: Color,
    val TextSecondary: Color,
    val TextLabel: Color
)

// Яркая светлая тема — синий акцент
val TruckLightColors = TruckColorPalette(
    Background = Color(0xFFF0F7FF),
    CardBackground = Color(0xFFFFFFFF),
    SurfaceSecondary = Color(0xFFE3F2FD),
    Divider = Color(0xFFBBDEFB),
    AccentPrimary = Color(0xFF1976D2),
    AccentExpense = Color(0xFFD32F2F),
    AccentInfo = Color(0xFF0D47A1),
    AccentWarning = Color(0xFFF9A825),
    TextPrimary = Color(0xFF0D47A1),
    TextSecondary = Color(0xFF424242),
    TextLabel = Color(0xFF757575)
)

// Яркая тёмная тема — синий акцент
val TruckDarkColors = TruckColorPalette(
    Background = Color(0xFF0A1929),
    CardBackground = Color(0xFF0F2137),
    SurfaceSecondary = Color(0xFF1A365D),
    Divider = Color(0xFF2C5282),
    AccentPrimary = Color(0xFF42A5F5),
    AccentExpense = Color(0xFFEF5350),
    AccentInfo = Color(0xFF64B5F6),
    AccentWarning = Color(0xFFFFCA28),
    TextPrimary = Color(0xFFE3F2FD),
    TextSecondary = Color(0xFF90CAF9),
    TextLabel = Color(0xFF64B5F6)
)

val LocalTruckColors = compositionLocalOf { TruckDarkColors }

package com.truckerload.presentation.theme

import androidx.compose.ui.graphics.Color

/** Light Soft UI palette — teal + orange accents. */
object AppColors {
    val White = Color(0xFFFFFFFF)
    val LightGray = Color(0xFFF5F5F5)
    val WarmCream = Color(0xFFFFF3E0)
    val DarkTeal = Color(0xFF1B3A4B)
    val Orange = Color(0xFFF5A623)
    val OrangeLight = Color(0xFFFFB74D)
    val BorderGray = Color(0xFFE0E0E0)

    val TextPrimary = DarkTeal
    val TextSecondary = Color(0xFF5A6B7A)
    val TextMuted = Color(0xFF9AA5B1)
    val TextOnDark = White
    val TextOnOrange = White
    val TextAccent = Orange

    val RpmGreen = Color(0xFF4CAF50)
    val RpmYellow = Color(0xFFFFC107)
    val RpmRed = Color(0xFFEF5350)
    val Danger = RpmRed

    val DarkTeal15 = DarkTeal.copy(alpha = 0.15f)
    val DarkTeal08 = DarkTeal.copy(alpha = 0.08f)
    val Orange15 = Orange.copy(alpha = 0.15f)
    val White70 = White.copy(alpha = 0.70f)
}

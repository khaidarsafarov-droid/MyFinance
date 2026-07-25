package com.truckerload.presentation.theme

import androidx.compose.ui.graphics.Color

/** App-wide semantic colors — Mindwell Forest (legacy Orange/Teal names are aliases). */
object AppColors {
    val White = Color(0xFFFFFFFF)
    val LightGray = SoftUiColors.BackgroundLight
    val WarmCream = SoftUiColors.PurpleLight
    val DarkTeal = SoftUiColors.PurpleEnd
    val Orange = SoftUiColors.PurpleStart
    val OrangeLight = SoftUiColors.PurpleMuted
    val BorderGray = Color(0xFFE5E7EB)

    val TextPrimary = SoftUiColors.TextPrimaryLight
    val TextSecondary = SoftUiColors.TextSecondaryLight
    val TextMuted = Color(0xFF9CA3AF)
    val TextOnDark = SoftUiColors.TextPrimaryDark
    val TextOnOrange = Color.White
    val TextAccent = SoftUiColors.PurpleStart

    val RpmGreen = Color(0xFF34D399)
    val RpmYellow = Color(0xFFFFD54F)
    val RpmRed = Color(0xFFFF8A80)
    val Danger = RpmRed

    val DarkTeal15 = SoftUiColors.PurpleStart.copy(alpha = 0.15f)
    val DarkTeal08 = SoftUiColors.ShadowTint
    val Orange15 = SoftUiColors.PurpleLight.copy(alpha = 0.65f)
    val White70 = White.copy(alpha = 0.70f)
}

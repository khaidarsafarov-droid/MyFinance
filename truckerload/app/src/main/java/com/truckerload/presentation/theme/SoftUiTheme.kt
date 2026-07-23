package com.truckerload.presentation.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Mindwell Forest palette from the Gemini Truck Log prototype.
 * Sage greens, forest primary cards, soft mint page background.
 */
object SoftUiColors {
    val ForestPrimary = Color(0xFF2F4F3E)
    val ForestAccent = Color(0xFF4A7C59)
    val ForestMuted = Color(0xFF557A64)
    val ForestSoft = Color(0xFFA3B899)
    val Sage = Color(0xFFE6EDE9)
    val SageHover = Color(0xFFD2E0D7)
    val SageBorder = Color(0xFFD2E0D7)
    val SageBorderStrong = Color(0xFFC5D3C9)
    val CardBorder = Color(0xFFE1EAE4)
    val ContentBg = Color(0xFFF9FBFA)
    val ShellBg = Color(0xFFF2F7F4)
    val OuterBg = Color(0xFFD8E2DC)

    /** Legacy names kept as aliases so existing call sites pick up the forest theme. */
    val PurpleStart = ForestAccent
    val PurpleEnd = ForestPrimary
    val SkyBlueEnd = ForestMuted
    val PurpleLight = Sage
    val PurpleMuted = ForestSoft

    val BackgroundLight = ContentBg
    val SurfaceLight = Color(0xFFFFFFFF)
    val SurfaceMuted = ShellBg

    val BackgroundDark = Color(0xFF1A2420)
    val SurfaceDark = Color(0xFF24302A)
    val SurfaceMutedDark = Color(0xFF2E3C35)

    val TextPrimaryLight = ForestPrimary
    val TextSecondaryLight = ForestMuted
    val TextPrimaryDark = Color(0xFFF2F7F4)
    val TextSecondaryDark = Color(0xFFA3B899)

    val ShadowTint = Color(0x142F4F3E)
    val ShadowNeutral = Color(0x0D000000)
}

object SoftUiDimens {
    val ChipRadius = 16.dp
    val ButtonRadius = 16.dp
    val CardRadius = 24.dp
    val CardLargeRadius = 24.dp
}

object SoftUiShapes {
    val Chip = RoundedCornerShape(SoftUiDimens.ChipRadius)
    val Button = RoundedCornerShape(SoftUiDimens.ButtonRadius)
    val Card = RoundedCornerShape(SoftUiDimens.CardRadius)
    val CardLarge = RoundedCornerShape(SoftUiDimens.CardLargeRadius)
    val NavBar = RoundedCornerShape(0.dp)
    val Fab = CircleShape
}

object SoftUiElevation {
    val Card = 2.dp
    val Button = 4.dp
    val NavBar = 0.dp
    val Fab = 8.dp
}

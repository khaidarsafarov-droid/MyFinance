package com.truckerload.presentation.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Soft glassmorphism palette inspired by modern smart-home UI (purple–blue gradients). */
object SoftUiColors {
    val PurpleStart = Color(0xFF8E78FF)
    val PurpleEnd = Color(0xFF6B5CE7)
    val SkyBlueEnd = Color(0xFF5B9FFF)
    val PurpleLight = Color(0xFFEDE9FF)
    val PurpleMuted = Color(0xFFB4A7FF)

    val BackgroundLight = Color(0xFFF5F6FA)
    val SurfaceLight = Color(0xFFFFFFFF)
    val SurfaceMuted = Color(0xFFF0F1F6)

    val BackgroundDark = Color(0xFF1A1B2E)
    val SurfaceDark = Color(0xFF252640)
    val SurfaceMutedDark = Color(0xFF2E3048)

    val TextPrimaryLight = Color(0xFF1A1C2E)
    val TextSecondaryLight = Color(0xFF6B7280)
    val TextPrimaryDark = Color(0xFFF0F0F5)
    val TextSecondaryDark = Color(0xFF9CA3AF)

    val ShadowTint = Color(0x148E78FF)
    val ShadowNeutral = Color(0x0D000000)
}

object SoftUiDimens {
    val ChipRadius = 16.dp
    val ButtonRadius = 20.dp
    val CardRadius = 24.dp
    val CardLargeRadius = 28.dp
}

object SoftUiShapes {
    val Chip = RoundedCornerShape(SoftUiDimens.ChipRadius)
    val Button = RoundedCornerShape(SoftUiDimens.ButtonRadius)
    val Card = RoundedCornerShape(SoftUiDimens.CardRadius)
    val CardLarge = RoundedCornerShape(SoftUiDimens.CardLargeRadius)
    val NavBar = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val Fab = CircleShape
}

object SoftUiElevation {
    val Card = 6.dp
    val Button = 8.dp
    val NavBar = 12.dp
    val Fab = 10.dp
}

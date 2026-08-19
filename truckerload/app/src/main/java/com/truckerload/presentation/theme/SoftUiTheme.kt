package com.truckerload.presentation.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shared soft-UI color tokens for the app's forest palette and legacy palette aliases.
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
    val ContentBg = Color(0xFFF6F6F6)
    val ShellBg = Color(0xFFEEEEEE)
    val OuterBg = Color(0xFFE4E4E4)

    /** Legacy names kept as aliases so existing call sites pick up the forest theme. */
    val PurpleStart = ForestAccent
    val PurpleEnd = ForestPrimary
    val SkyBlueEnd = ForestMuted
    val PurpleLight = Sage
    val PurpleMuted = ForestSoft

    val BackgroundLight = ContentBg
    val SurfaceLight = Color(0xFFFFFFFF)
    val SurfaceMuted = ShellBg

    val BackgroundDark = Color(0xFF171717)
    val SurfaceDark = Color(0xFF252525)
    val SurfaceMutedDark = Color(0xFF2C2C2C)

    /** True-black OLED tokens — saves power on AMOLED and reduces night glare. */
    val BackgroundOled = Color(0xFF000000)
    val SurfaceOled = Color(0xFF121212)
    val SurfaceMutedOled = Color(0xFF1C1C1C)
    val VoiceCallBg = Color(0xFF1A1B2E)
    val VoiceSuccess = Color(0xFF34C759)
    val VoiceDanger = Color(0xFFFF3B30)

    val TextPrimaryLight = ForestPrimary
    val TextSecondaryLight = ForestMuted
    val TextPrimaryDark = Color(0xFFF2F7F4)
    val TextSecondaryDark = Color(0xFFA3B899)

    val ShadowTint = Color(0x142F4F3E)
    val ShadowNeutral = Color(0x0D000000)
}

/**
 * Shared soft-UI shape dimensions used by cards, buttons, chips, and navigation surfaces.
 */
object SoftUiDimens {
    val ChipRadius = OneUiTokens.CornerChip
    val ButtonRadius = OneUiTokens.CornerButton
    val CardRadius = OneUiTokens.CornerCard
    val CardLargeRadius = OneUiTokens.CornerCardLarge
}

/**
 * Shared soft-UI shape tokens derived from [SoftUiDimens].
 */
object SoftUiShapes {
    val Chip = RoundedCornerShape(SoftUiDimens.ChipRadius)
    val Button = RoundedCornerShape(SoftUiDimens.ButtonRadius)
    val Card = RoundedCornerShape(SoftUiDimens.CardRadius)
    val CardLarge = RoundedCornerShape(SoftUiDimens.CardLargeRadius)
    val NavBar = RoundedCornerShape(0.dp)
    val Fab = CircleShape
}

/**
 * Shared soft-UI elevation tokens for layered Compose surfaces.
 */
object SoftUiElevation {
    val Card = 2.dp
    val Button = 4.dp
    val NavBar = 0.dp
    val Fab = 8.dp
}

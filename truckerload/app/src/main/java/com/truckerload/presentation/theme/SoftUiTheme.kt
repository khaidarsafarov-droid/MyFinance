package com.truckerload.presentation.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Daily Task Tracker kit palette with WCAG AA body text.
 *
 * Kit fills (#5B54E6, #48C9B0, #FFB74D, #FF708D) are chrome and charts.
 * Body copy stays near-black / dark gray so 14sp labels stay readable.
 * Token names stay Forest* so existing call sites pick up the new look.
 */
object SoftUiColors {
    /** Primary chrome / buttons / logo plate — darkened kit purple for white-on-fill AA. */
    val ForestPrimary = Color(0xFF5B54E6)
    val ForestAccent = Color(0xFF5B54E6)
    /** Decorative lavender — not body text (fails AA on the light canvas by design). */
    val ForestMuted = Color(0xFFA29BFE)
    val ForestSoft = Color(0xFFD4D0FF)
    val Sage = Color(0xFFEEEDFF)
    val SageHover = Color(0xFFE0DEFF)
    val SageBorder = Color(0xFFE3E2F0)
    val SageBorderStrong = Color(0xFFC4C1E4)
    val CardBorder = Color(0xFFE3E2F0)
    val ContentBg = Color(0xFFF1F1FA)
    val ShellBg = Color(0xFFF1F1FA)
    val OuterBg = Color(0xFFE4E4F0)

    /** Legacy names kept as aliases so existing call sites pick up the kit theme. */
    val PurpleStart = ForestAccent
    val PurpleEnd = ForestPrimary
    val SkyBlueEnd = ForestMuted
    val PurpleLight = Sage
    val PurpleMuted = ForestSoft

    val BackgroundLight = ContentBg
    val SurfaceLight = Color(0xFFFFFFFF)
    val SurfaceMuted = ShellBg

    /** Dedicated dark cabin tokens (indigo, not inverted light). */
    val BackgroundDark = Color(0xFF0B0B22)
    val SurfaceDark = Color(0xFF17173A)
    val SurfaceMutedDark = Color(0xFF2E2A66)
    val CardBorderDark = Color(0xFF2C2B5A)
    val BorderStrongDark = Color(0xFF3A3870)
    /** Lightened primary on the dark cabin. on-primary stays dark so it clears AA on #8F89FF. */
    val ForestAccentDark = Color(0xFF8F89FF)
    val OnForestAccentDark = Color(0xFF14123A)
    val OnContainerLight = Color(0xFF2B2580)
    val OnContainerDark = Color(0xFFDCDAFF)

    /** True-black OLED tokens — saves power on AMOLED and reduces night glare. */
    val BackgroundOled = Color(0xFF000000)
    val SurfaceOled = Color(0xFF121212)
    val SurfaceMutedOled = Color(0xFF1C1C1C)
    val VoiceCallBg = Color(0xFF1A1B2E)
    val VoiceSuccess = Color(0xFF34C759)
    val VoiceDanger = Color(0xFFFF3B30)

    val TextPrimaryLight = Color(0xFF16152B)
    val TextSecondaryLight = Color(0xFF57557A)
    val TextTertiaryLight = Color(0xFF6B6990)
    val TextPrimaryDark = Color(0xFFF2F1FF)
    val TextSecondaryDark = Color(0xFFB4B2D1)
    val TextTertiaryDark = Color(0xFF8E8CB0)
    val OrangeLight = Color(0xFFDF7B14)
    val OrangeDark = Color(0xFFF0A151)
    val HeroStartLight = Color(0xFF2B25A1)
    val HeroEndLight = Color(0xFF0C0F6C)
    val HeroStartDark = Color(0xFF3A33C9)
    val HeroEndDark = Color(0xFF19189A)

    val ShadowTint = Color(0x145B54E6)
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
    val Card = 0.dp
    val Button = 0.dp
    val NavBar = 0.dp
    val Fab = 8.dp
}

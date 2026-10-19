package com.truckerload.presentation.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Midnight & Gold (P2). Body and button text stay WCAG AA.
 * Token names stay Forest* so existing call sites pick up the new look.
 */
object SoftUiColors {
    /** Light primary — indigo. White on this fill clears AA. */
    val ForestPrimary = Color(0xFF3F3D9E)
    val ForestAccent = Color(0xFF3F3D9E)
    /** Decorative pale indigo — not body text (fails AA on the light canvas by design). */
    val ForestMuted = Color(0xFFC9C6DC)
    val ForestSoft = Color(0xFFD9D6EE)
    val Sage = Color(0xFFEAE9F4)
    val SageHover = Color(0xFFDEDBEE)
    val SageBorder = Color(0xFFE6E2D8)
    val SageBorderStrong = Color(0xFFD3CDBF)
    val CardBorder = Color(0xFFE6E2D8)
    val ContentBg = Color(0xFFF5F4F1)
    val ShellBg = Color(0xFFF5F4F1)
    val OuterBg = Color(0xFFE8E6E0)

    /** Legacy names kept as aliases so existing call sites pick up the kit theme. */
    val PurpleStart = ForestAccent
    val PurpleEnd = ForestPrimary
    val SkyBlueEnd = ForestMuted
    val PurpleLight = Sage
    val PurpleMuted = ForestSoft

    val BackgroundLight = ContentBg
    val SurfaceLight = Color(0xFFFDFCFA)
    val SurfaceMuted = ShellBg

    /** Dedicated dark cabin. Primary buttons are champagne gold, not inverted indigo. */
    val BackgroundDark = Color(0xFF0D0F1E)
    val SurfaceDark = Color(0xFF161929)
    val SurfaceMutedDark = Color(0xFF262A4F)
    val CardBorderDark = Color(0xFF262A3E)
    val BorderStrongDark = Color(0xFF34384F)
    /** Dark primary button. on-primary stays espresso so it clears AA on #D6BC85. */
    val ForestAccentDark = Color(0xFFD6BC85)
    val OnForestAccentDark = Color(0xFF1D1708)
    val OnContainerLight = Color(0xFF25265E)
    val OnContainerDark = Color(0xFFE2DFF5)
    val OnDangerDark = Color(0xFF2A1418)

    /** True-black OLED tokens — saves power on AMOLED and reduces night glare. */
    val BackgroundOled = Color(0xFF000000)
    val SurfaceOled = Color(0xFF121212)
    val SurfaceMutedOled = Color(0xFF1C1C1C)
    val VoiceCallBg = Color(0xFF1A1B2E)
    val VoiceSuccess = Color(0xFF34C759)
    val VoiceDanger = Color(0xFFFF3B30)

    val TextPrimaryLight = Color(0xFF16182B)
    val TextSecondaryLight = Color(0xFF585A6E)
    val TextTertiaryLight = Color(0xFF6A6C82)
    val TextPrimaryDark = Color(0xFFEEEBE3)
    val TextSecondaryDark = Color(0xFFA7A6B4)
    val TextTertiaryDark = Color(0xFF8E8D9C)
    val OrangeLight = Color(0xFF86542A)
    val OrangeDark = Color(0xFFDDAE84)
    val HeroStartLight = Color(0xFF1E2150)
    val HeroEndLight = Color(0xFF0F1230)
    val HeroStartDark = Color(0xFF1E2250)
    val HeroEndDark = Color(0xFF111430)
    val HeroTextLight = Color(0xFFF7F2E6)
    val HeroTextMutedLight = Color(0xFFD9CFB4)
    val HeroTextDark = Color(0xFFF5EEDC)
    val HeroTextMutedDark = Color(0xFFCDBF9C)

    val ShadowTint = Color(0x143F3D9E)
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

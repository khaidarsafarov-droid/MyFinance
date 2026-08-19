package com.truckerload.presentation.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Shape tokens aligned with One UI (16 chip, 20 button, 26–28 card). */
object AppShapes {
    val Small = RoundedCornerShape(OneUiTokens.CornerChip)
    val Medium = SoftUiShapes.Chip
    val Large = SoftUiShapes.Card
    val XLarge = SoftUiShapes.CardLarge
    val NavTop = RoundedCornerShape(topStart = SoftUiDimens.CardRadius, topEnd = SoftUiDimens.CardRadius)
    val Circle = CircleShape
}

package com.truckerload.presentation.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Shape tokens aligned with SoftUi / Mindwell Forest (16 chip, 24 card). */
object AppShapes {
    val Small = RoundedCornerShape(12.dp)
    val Medium = SoftUiShapes.Chip
    val Large = SoftUiShapes.Card
    val XLarge = SoftUiShapes.CardLarge
    val NavTop = RoundedCornerShape(topStart = SoftUiDimens.CardRadius, topEnd = SoftUiDimens.CardRadius)
    val Circle = CircleShape
}

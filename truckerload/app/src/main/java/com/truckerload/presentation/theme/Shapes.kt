package com.truckerload.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

/** Material 3 shapes: chips 8 dp, buttons 16 dp, cards 12–16 dp. */
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(OneUiTokens.CornerChip),
    small = SoftUiShapes.Chip,
    medium = SoftUiShapes.Button,
    large = SoftUiShapes.Card,
    extraLarge = SoftUiShapes.CardLarge,
)

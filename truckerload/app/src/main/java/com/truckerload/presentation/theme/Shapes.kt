package com.truckerload.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

/** One UI rounded shapes — cards and controls stay in the 16–28 dp range. */
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(OneUiTokens.CornerChip),
    small = SoftUiShapes.Chip,
    medium = SoftUiShapes.Button,
    large = SoftUiShapes.Card,
    extraLarge = SoftUiShapes.CardLarge,
)

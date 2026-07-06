package com.truckerload.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** One UI–style rounded shapes — soft smart-home UI (16–28dp). */
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = SoftUiShapes.Chip,
    medium = SoftUiShapes.Button,
    large = SoftUiShapes.Card,
    extraLarge = SoftUiShapes.CardLarge,
)

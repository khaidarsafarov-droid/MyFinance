package com.truckerload.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object AppElevation {
    val Card = SoftUiElevation.Card
    val Button = SoftUiElevation.Button
    val Fab = SoftUiElevation.Fab
    val Nav = SoftUiElevation.NavBar
    val Hero = SoftUiElevation.Card
    val Input = 2.dp
    val Chip = 2.dp

    val ShadowTealAmbient = SoftUiColors.ShadowTint
    val ShadowTealSpot = SoftUiColors.ShadowNeutral
    val ShadowOrangeAmbient = SoftUiColors.ShadowTint
    val ShadowOrangeSpot = SoftUiColors.ShadowNeutral

    fun cardShadow(
        modifier: Modifier = Modifier,
        elevation: Dp = Card,
        shape: Shape = SoftUiShapes.Card,
    ): Modifier = modifier.shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = ShadowTealAmbient,
        spotColor = ShadowTealSpot,
    )

    fun heroShadow(
        modifier: Modifier = Modifier,
        shape: Shape = SoftUiShapes.CardLarge,
    ): Modifier = cardShadow(modifier, Hero, shape)

    fun buttonShadow(
        modifier: Modifier = Modifier,
        shape: Shape = SoftUiShapes.Button,
    ): Modifier = modifier.shadow(
        elevation = Button,
        shape = shape,
        ambientColor = ShadowOrangeAmbient,
        spotColor = ShadowOrangeSpot,
    )

    fun inputShadow(
        modifier: Modifier = Modifier,
        shape: Shape = SoftUiShapes.Chip,
    ): Modifier = modifier.shadow(
        elevation = Input,
        shape = shape,
        ambientColor = ShadowTealSpot,
        spotColor = Color(0x06000000),
    )

    fun navShadow(
        modifier: Modifier = Modifier,
    ): Modifier = modifier.shadow(
        elevation = Nav,
        shape = SoftUiShapes.NavBar,
        ambientColor = ShadowTealAmbient,
        spotColor = ShadowTealSpot,
    )
}

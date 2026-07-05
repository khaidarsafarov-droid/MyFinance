package com.truckerload.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object AppElevation {
    val Card = 8.dp
    val Button = 12.dp
    val Fab = 16.dp
    val Nav = 20.dp
    val Hero = 16.dp
    val Input = 4.dp
    val Chip = 2.dp

    val ShadowTealAmbient = Color(0x1A1B3A4B)
    val ShadowTealSpot = Color(0x0D1B3A4B)
    val ShadowOrangeAmbient = Color(0x26F5A623)
    val ShadowOrangeSpot = Color(0x1AF5A623)

    fun cardShadow(
        modifier: Modifier = Modifier,
        elevation: Dp = Card,
        shape: Shape = AppShapes.Large,
    ): Modifier = modifier.shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = ShadowTealAmbient,
        spotColor = ShadowTealSpot,
    )

    fun heroShadow(
        modifier: Modifier = Modifier,
        shape: Shape = AppShapes.XLarge,
    ): Modifier = cardShadow(modifier, Hero, shape)

    fun buttonShadow(
        modifier: Modifier = Modifier,
        shape: Shape = AppShapes.Medium,
    ): Modifier = modifier.shadow(
        elevation = Button,
        shape = shape,
        ambientColor = ShadowOrangeAmbient,
        spotColor = ShadowOrangeSpot,
    )

    fun inputShadow(
        modifier: Modifier = Modifier,
        shape: Shape = AppShapes.Small,
    ): Modifier = modifier.shadow(
        elevation = Input,
        shape = shape,
        ambientColor = ShadowTealSpot,
        spotColor = Color(0x061B3A4B),
    )

    fun navShadow(
        modifier: Modifier = Modifier,
    ): Modifier = modifier.shadow(
        elevation = Nav,
        shape = AppShapes.NavTop,
        ambientColor = Color(0x141B3A4B),
        spotColor = Color(0x0A1B3A4B),
    )
}

package com.truckerload.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

enum class SoftSurfaceKind {
    Card,
    Hero,
    Cream,
    Input,
}

@Composable
fun Modifier.softSurface(
    shape: Shape = AppShapes.Large,
    kind: SoftSurfaceKind = SoftSurfaceKind.Card,
): Modifier {
    val cs = MaterialTheme.colorScheme
    val bg = when (kind) {
        SoftSurfaceKind.Card -> cs.surface
        SoftSurfaceKind.Hero -> cs.primary
        SoftSurfaceKind.Cream -> cs.secondaryContainer
        SoftSurfaceKind.Input -> cs.surfaceVariant
    }
    return background(bg, shape)
}

/** @deprecated Use [softSurface] */
@Composable
fun Modifier.darkGlassSurface(
    shape: Shape = AppShapes.Large,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    solidBackground: Boolean = false,
    elevation: androidx.compose.ui.unit.Dp = 0.dp,
    cardAlpha: Float = 1f,
    enableBlur: Boolean = false,
): Modifier = softSurface(shape = shape, kind = SoftSurfaceKind.Card)

@Composable
fun DarkGlassScreen(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        content = content,
    )
}

@Composable
fun Modifier.darkGlassScreenGradient(): Modifier =
    background(MaterialTheme.colorScheme.background, RectangleShape)

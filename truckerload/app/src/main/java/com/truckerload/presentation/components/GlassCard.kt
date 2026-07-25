package com.truckerload.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.DarkGlassGradients
import com.truckerload.presentation.theme.SoftUiColors
import com.truckerload.presentation.theme.SoftUiDimens
import com.truckerload.presentation.theme.SoftUiElevation
import com.truckerload.presentation.theme.SoftUiShapes

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cornerRadius: Dp = BentoGlassTheme.CardRadius,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
    solidBackground: Boolean = false,
    useCream: Boolean = false,
    useHero: Boolean = false,
    useHighlight: Boolean = false,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape: Shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val cs = MaterialTheme.colorScheme
    val heroBrush = if (useHero) DarkGlassGradients.cardShine else null
    val containerColor = when {
        useHero -> Color.Transparent
        useHighlight -> cs.primaryContainer
        useCream -> cs.secondaryContainer
        else -> cs.surface
    }
    Surface(
        modifier = modifier
            .then(
                if (!useHero) {
                    Modifier.shadow(
                        elevation = SoftUiElevation.Card,
                        shape = shape,
                        ambientColor = SoftUiColors.ShadowTint,
                        spotColor = SoftUiColors.ShadowNeutral,
                    )
                } else {
                    Modifier.shadow(
                        elevation = SoftUiElevation.Card,
                        shape = shape,
                        ambientColor = SoftUiColors.ShadowTint,
                        spotColor = SoftUiColors.ShadowNeutral,
                    )
                },
            )
            .then(if (heroBrush != null) Modifier.background(heroBrush, shape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = containerColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/** Material Card replacement — soft shadow, 24dp corners, consistent padding. */
@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    useHero: Boolean = false,
    useHighlight: Boolean = false,
    contentPadding: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    GlassCard(
        modifier = modifier,
        onClick = onClick,
        cornerRadius = SoftUiDimens.CardRadius,
        useHero = useHero,
        useHighlight = useHighlight,
        contentPadding = contentPadding,
        content = content,
    )
}

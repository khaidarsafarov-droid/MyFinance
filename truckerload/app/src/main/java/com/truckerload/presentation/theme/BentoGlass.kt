package com.truckerload.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object BentoGlassTheme {
    val CardRadius = SoftUiDimens.CardLargeRadius
    val CellRadius = SoftUiDimens.ChipRadius
    val BorderWidth = 1.dp

    val ScreenBackground: Color
        @Composable get() = MaterialTheme.colorScheme.background

    val CardFill: Color
        @Composable get() = MaterialTheme.colorScheme.surface

    val CardBorder: Color
        @Composable get() = MaterialTheme.colorScheme.outline

    val CardBorderMuted: Color
        @Composable get() = MaterialTheme.colorScheme.outlineVariant

    val CardShadow: Color
        @Composable get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)

    val GoalGradientStart: Color
        @Composable get() = MaterialTheme.colorScheme.primary

    val GoalGradientEnd: Color
        @Composable get() = MaterialTheme.colorScheme.primary

    val HeroGradient: Brush
        @Composable get() = DarkGlassGradients.cardShine
}

@Composable
fun BentoGlassScreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    DarkGlassScreen(modifier = modifier, content = content)
}

@Composable
fun BentoGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = BentoGlassTheme.CardRadius,
    borderColor: Color? = null,
    useHeroGradient: Boolean = false,
    useCream: Boolean = false,
    useHighlight: Boolean = false,
    solidBackground: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape: Shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val cs = MaterialTheme.colorScheme
    val containerColor = when {
        solidBackground -> cs.surface
        useHeroGradient -> SoftUiColors.ForestPrimary
        useHighlight -> SoftUiColors.Sage
        useCream -> cs.secondaryContainer
        else -> cs.surface
    }
    Surface(
        modifier = modifier.then(
            Modifier.shadow(
                elevation = SoftUiElevation.Card,
                shape = shape,
                ambientColor = SoftUiColors.ShadowTint,
                spotColor = SoftUiColors.ShadowNeutral,
            ),
        ),
        shape = shape,
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(
            width = if (useHeroGradient) 0.dp else BentoGlassTheme.BorderWidth,
            color = if (useHeroGradient) Color.Transparent else SoftUiColors.CardBorder,
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        content = { Column(content = content) },
    )
}

@Composable
fun BentoGlassClickableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = BentoGlassTheme.CardRadius,
    solidBackground: Boolean = false,
    highlight: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    BentoGlassCard(
        modifier = modifier.clickable(onClick = onClick),
        cornerRadius = cornerRadius,
        solidBackground = solidBackground,
        useHighlight = highlight,
        content = content,
    )
}

@Composable
fun BentoGlassSection(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Text(
            text = title,
            style = AppTypography.CardTitle,
            modifier = Modifier.padding(bottom = if (subtitle != null) 6.dp else 12.dp),
        )
        subtitle?.let {
            Text(
                text = it,
                style = AppTypography.Subtitle,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
        BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(4.dp), content = content)
        }
    }
}

@Composable
fun BentoGlassSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = AppTypography.Body,
        cursorBrush = SolidColor(cs.primary),
        decorationBox = { inner ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.Small,
                color = cs.surfaceVariant,
                tonalElevation = 0.dp,
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    if (value.isEmpty()) {
                        Text(placeholder, style = AppTypography.Body.copy(color = cs.onSurfaceVariant))
                    }
                    inner()
                }
            }
        },
    )
}

@Composable
fun BentoGlassMetricCell(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    accent: Color,
    highlight: Boolean = false,
) {
    val cs = MaterialTheme.colorScheme
    val shape = remember { SoftUiShapes.Chip }
    Surface(
        modifier = modifier
            .heightIn(min = 72.dp)
            .shadow(
                elevation = if (highlight) 4.dp else SoftUiElevation.Card,
                shape = shape,
                ambientColor = SoftUiColors.ShadowTint,
                spotColor = SoftUiColors.ShadowNeutral,
            ),
        shape = shape,
        color = if (highlight) SoftUiColors.PurpleLight else cs.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .heightIn(min = 40.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = AppTypography.CaptionMuted)
            Text(
                text = value,
                style = if (highlight) {
                    AppTypography.NumbersLarge.copy(fontSize = AppTypography.NumbersMetric.fontSize)
                } else {
                    AppTypography.NumbersMetric
                },
                color = accent,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
fun BentoGlassDarkPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    BentoGlassCard(modifier = modifier, useHeroGradient = true, content = content)
}

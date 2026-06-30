package com.truckerload.presentation.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Airy Soft UI tokens — floating white cards, pastel sky, no hard borders. */
object BentoGlassTheme {
    val CardRadius = 24.dp
    val CellRadius = 20.dp
    val BorderWidth = 0.dp

    val ScreenBackground: Color
        @Composable get() = LocalTruckColors.current.Background

    val CardFill: Color
        @Composable get() = LocalTruckColors.current.CardBackground

    val CardBorder: Color
        @Composable get() = Color.Transparent

    val CardBorderMuted: Color
        @Composable get() = LocalTruckColors.current.Divider

    val CardShadow: Color
        @Composable get() = NeoGlassPalette.ShadowSoft

    val GoalGradientStart: Color
        @Composable get() = LocalTruckColors.current.AccentPrimary

    val GoalGradientEnd: Color
        @Composable get() = LocalTruckColors.current.AccentSecondary

    val HeroGradient: Brush
        @Composable get() = SoftGradients.cardShine
}

@Composable
fun BentoGlassScreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val brush = if (isSystemInDarkTheme()) SoftGradients.screenDark else SoftGradients.screen
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush),
        content = content
    )
}

@Composable
fun BentoGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = BentoGlassTheme.CardRadius,
    borderColor: Color? = null,
    useHeroGradient: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val tc = LocalTruckColors.current
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val fill = if (useHeroGradient) {
        Brush.verticalGradient(listOf(tc.SurfaceSecondary.copy(0.35f), tc.CardBackground))
    } else {
        SolidColor(tc.CardBackground)
    }
    Column(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = BentoGlassTheme.CardShadow,
                spotColor = BentoGlassTheme.CardShadow,
            )
            .clip(shape)
            .background(fill)
            .clip(shape),
        content = content
    )
}

@Composable
fun BentoGlassClickableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = BentoGlassTheme.CellRadius,
    content: @Composable ColumnScope.() -> Unit
) {
    BentoGlassCard(
        modifier = modifier.clickable(onClick = onClick),
        cornerRadius = cornerRadius,
        content = {
            Column(modifier = Modifier.padding(20.dp), content = content)
        }
    )
}

@Composable
fun BentoGlassSection(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val tc = LocalTruckColors.current
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = tc.AccentPrimary,
            modifier = Modifier.padding(bottom = if (subtitle != null) 6.dp else 12.dp)
        )
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp), content = content)
        }
    }
}

@Composable
fun BentoGlassSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val tc = LocalTruckColors.current
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = tc.TextPrimary),
        cursorBrush = SolidColor(tc.AccentPrimary),
        decorationBox = { inner ->
            BentoGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = BentoGlassTheme.CellRadius
            ) {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    if (value.isEmpty()) {
                        Text(placeholder, color = tc.TextLabel)
                    }
                    inner()
                }
            }
        }
    )
}

@Composable
fun BentoGlassMetricCell(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    accent: Color,
    highlight: Boolean = false
) {
    val tc = LocalTruckColors.current
    val shape = remember { RoundedCornerShape(BentoGlassTheme.CellRadius) }
    Column(
        modifier = modifier
            .shadow(
                elevation = if (highlight) 10.dp else 8.dp,
                shape = shape,
                ambientColor = BentoGlassTheme.CardShadow,
                spotColor = BentoGlassTheme.CardShadow,
            )
            .clip(shape)
            .background(
                if (highlight) {
                    Brush.verticalGradient(
                        listOf(accent.copy(alpha = 0.10f), tc.CardBackground)
                    )
                } else {
                    SolidColor(tc.CardBackground)
                }
            )
            .padding(20.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = tc.TextLabel
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = if (highlight) 22.sp else 20.sp
            ),
            color = if (highlight) accent else tc.TextPrimary,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun BentoGlassDarkPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    BentoGlassCard(modifier = modifier, content = content)
}

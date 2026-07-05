package com.truckerload.presentation.components

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.DarkGlassGradients
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.NeoGlassPalette
import java.util.Locale

@Composable
fun GoldCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tc = LocalTruckColors.current
    val shape = remember { RoundedCornerShape(BentoGlassTheme.CardRadius) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, shape, ambientColor = NeoGlassPalette.ShadowSoft, spotColor = tc.AccentPrimary.copy(0.25f))
            .clip(shape)
            .background(DarkGlassGradients.cardShine)
            .border(BentoGlassTheme.BorderWidth, tc.GlassBorder, shape)
            .padding(24.dp),
        content = content,
    )
}

@Composable
fun GoldButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val tc = LocalTruckColors.current
    val shape = remember { RoundedCornerShape(20.dp) }
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .shadow(if (enabled) 6.dp else 0.dp, shape, ambientColor = tc.AccentPrimary.copy(0.2f))
            .clip(shape)
            .background(
                if (enabled) DarkGlassGradients.cta
                else Brush.linearGradient(listOf(tc.ProgressTrack, tc.ProgressTrack))
            )
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 28.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) tc.OnAccent else tc.TextSecondary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun GoldDivider(modifier: Modifier = Modifier) {
    val tc = LocalTruckColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        tc.AccentPrimary.copy(alpha = 0.25f),
                        tc.AccentSecondary.copy(alpha = 0.35f),
                        tc.AccentPrimary.copy(alpha = 0.25f),
                        Color.Transparent,
                    ),
                ),
            ),
    )
}

@Composable
fun GoldIcon(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    val tc = LocalTruckColors.current
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        modifier = modifier.size(size),
        tint = tc.AccentPrimary,
    )
}

@Composable
fun GoldAnimatedNumber(
    target: Double,
    modifier: Modifier = Modifier,
    prefix: String = "$",
    format: String = "%,.0f",
) {
    val tc = LocalTruckColors.current
    val animatedValue by animateFloatAsState(
        targetValue = target.toFloat(),
        animationSpec = tween(800, easing = EaseOutCubic),
        label = "softNumber",
    )
    Text(
        text = prefix + String.format(Locale.US, format, animatedValue.toDouble()),
        modifier = modifier,
        style = MaterialTheme.typography.displayMedium,
        color = tc.TextPrimary,
    )
}

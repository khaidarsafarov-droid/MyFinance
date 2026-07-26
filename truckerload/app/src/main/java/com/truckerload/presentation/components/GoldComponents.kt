package com.truckerload.presentation.components

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import java.util.Locale

/**
 * Legacy Gold* API — thin aliases over SoftCard / TlButton.
 * Prefer SoftCard / TlButton / ForestScreenTitle directly in new code.
 */
@Deprecated("Use SoftCard", ReplaceWith("SoftCard(modifier = modifier, content = content)"))
@Composable
fun GoldCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    SoftCard(modifier = modifier, content = content)
}

@Deprecated("Use TlButton", ReplaceWith("TlButton(onClick = onClick, modifier = modifier, enabled = enabled) { Text(text) }"))
@Composable
fun GoldButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TlButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Deprecated("Use Material Divider / HorizontalDivider")
@Composable
fun GoldDivider(modifier: Modifier = Modifier) {
    androidx.compose.material3.HorizontalDivider(
        modifier = modifier,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
    )
}

@Deprecated("Use Icon with MaterialTheme.colorScheme.primary")
@Composable
fun GoldIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = MaterialTheme.colorScheme.primary,
    )
}

@Deprecated("Use AnimatedNumber")
@Composable
fun GoldAnimatedNumber(
    value: Double,
    modifier: Modifier = Modifier,
    prefix: String = "$",
    decimals: Int = 0,
) {
    val animated by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = tween(durationMillis = 700, easing = EaseOutCubic),
        label = "goldNumber",
    )
    Text(
        text = prefix + String.format(Locale.US, "%,.${decimals}f", animated),
        modifier = modifier.defaultMinSize(minHeight = 24.dp),
        style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            fontFeatureSettings = "tnum",
            color = MaterialTheme.colorScheme.primary,
        ),
    )
}

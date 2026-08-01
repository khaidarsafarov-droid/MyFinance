package com.truckerload.presentation.components

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import com.truckerload.presentation.theme.MotionPreferences
import java.util.Locale

@Composable
fun AnimatedNumber(
    target: Double,
    modifier: Modifier = Modifier,
    format: String = "%,.0f",
    prefix: String = "",
    suffix: String = "",
    fontSize: TextUnit = MaterialTheme.typography.headlineMedium.fontSize,
    color: Color = Color.Unspecified,
    useNeon: Boolean = false,
) {
    val motionMs = if (MotionPreferences.reduceMotion) 0 else 400
    val animatedValue by animateFloatAsState(
        targetValue = target.toFloat(),
        animationSpec = tween(motionMs, easing = EaseOutCubic),
        label = "animatedNumber"
    )
    val formatted = prefix + String.format(Locale.US, format, animatedValue.toDouble()) + suffix
    if (useNeon) {
        NeonText(text = formatted, modifier = modifier, fontSize = fontSize, color = color)
    } else {
        Text(
            text = formatted,
            modifier = modifier,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
    }
}

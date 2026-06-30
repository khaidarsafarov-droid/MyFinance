package com.truckerload.presentation.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.glowColor

@Composable
fun NeonText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 32.sp,
    color: Color = LocalTruckColors.current.AccentPrimary,
    glowColor: Color = glowColor(color),
) {
    Text(
        text = text,
        modifier = modifier,
        fontSize = fontSize,
        fontWeight = FontWeight.ExtraBold,
        color = color,
        style = TextStyle(
            shadow = Shadow(
                color = glowColor,
                blurRadius = 20f
            )
        )
    )
}

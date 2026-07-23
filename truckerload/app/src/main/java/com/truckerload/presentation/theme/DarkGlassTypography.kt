package com.truckerload.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun DarkGlassScreenTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = AppTypography.ScreenTitle.copy(color = color),
    )
}

@Composable
fun DarkGlassSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    emoji: String? = null,
) {
    val label = when {
        !emoji.isNullOrBlank() -> "$emoji ${text.uppercase()}"
        else -> text.uppercase()
    }
    Text(
        text = label,
        modifier = modifier,
        style = AppTypography.SectionTitle,
    )
}

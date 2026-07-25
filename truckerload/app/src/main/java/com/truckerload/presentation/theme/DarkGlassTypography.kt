package com.truckerload.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** Brand screen title (uppercase + tracking). Prefer this over legacy DarkGlass* names. */
@Composable
fun ForestScreenTitle(
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

/** Brand section label (uppercase + muted tracking). */
@Composable
fun ForestSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = AppTypography.SectionTitle,
    )
}

@Deprecated("Use ForestScreenTitle", ReplaceWith("ForestScreenTitle(text, modifier, color)"))
@Composable
fun DarkGlassScreenTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
) {
    ForestScreenTitle(text = text, modifier = modifier, color = color)
}

@Deprecated("Use ForestSectionTitle", ReplaceWith("ForestSectionTitle(text, modifier)"))
@Composable
fun DarkGlassSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") emoji: String? = null,
) {
    ForestSectionTitle(text = text, modifier = modifier)
}

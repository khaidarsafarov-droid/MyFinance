package com.truckerload.presentation.theme

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

object AppTextFieldDefaults {
    /** Typed text. Ivory on the midnight field, near-black on the light field. */
    @Composable
    fun inputColor(): Color = MaterialTheme.colorScheme.onSurface

    /** Champagne gold in the dark cabin, indigo in the light cabin. */
    @Composable
    fun cursorColor(): Color = MaterialTheme.colorScheme.primary

    /**
     * Hint on the field fill. Dark mode uses the muted gold hero text when it
     * clears WCAG AA; light mode falls back to secondary text. Never outline:
     * that token is the navy border and disappears on the navy field.
     */
    @Composable
    fun hintColor(): Color {
        val scheme = MaterialTheme.colorScheme
        val brand = LocalBrandPalette.current
        return readableOnField(
            field = scheme.surfaceVariant,
            candidates = listOf(brand.heroTextMuted, scheme.onSurfaceVariant),
            fallback = scheme.onSurface,
        )
    }

    /** Search and adornment icons. Gold when it clears AA on the field fill. */
    @Composable
    fun iconColor(): Color {
        val scheme = MaterialTheme.colorScheme
        val brand = LocalBrandPalette.current
        return readableOnField(
            field = scheme.surfaceVariant,
            candidates = listOf(scheme.primary, brand.heroTextMuted, scheme.onSurface),
            fallback = scheme.onSurface,
        )
    }

    @Composable
    fun outlined() = OutlinedTextFieldDefaults.colors(
        focusedTextColor = inputColor(),
        unfocusedTextColor = inputColor(),
        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cursorColor = cursorColor(),
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedPlaceholderColor = hintColor(),
        unfocusedPlaceholderColor = hintColor(),
        disabledPlaceholderColor = hintColor(),
        focusedLeadingIconColor = iconColor(),
        unfocusedLeadingIconColor = iconColor(),
        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedTrailingIconColor = iconColor(),
        unfocusedTrailingIconColor = iconColor(),
        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedPrefixColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedPrefixColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    )
}

/**
 * First candidate that clears WCAG AA on [field], or [fallback] when none do.
 * Used for search hints and icons so a border token cannot be painted as text.
 */
internal fun readableOnField(field: Color, candidates: List<Color>, fallback: Color): Color =
    candidates.firstOrNull { ContrastRatios.passesAa(it, field) } ?: fallback

/** Min height 48dp for comfortable tablet / finger input. */
fun Modifier.appFormField(): Modifier =
    defaultMinSize(minHeight = UiDimens.FormFieldMinHeight)

fun Modifier.appFormFieldSpacing(): Modifier =
    padding(bottom = UiDimens.FormFieldSpacing)

object AppFilterChipDefaults {
    private val chipShape = RoundedCornerShape(SoftUiDimens.ChipRadius)

    fun target(): Modifier = Modifier.heightIn(min = UiDimens.TouchTarget)

    @Composable
    fun shape() = chipShape

    @Composable
    fun colors() = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        containerColor = MaterialTheme.colorScheme.surface,
        labelColor = MaterialTheme.colorScheme.onSurface,
        iconColor = MaterialTheme.colorScheme.onSurface,
        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
        selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimary,
    )

    @Composable
    fun stateColors() = colors()
}

object AppSwitchDefaults {
    @Composable
    fun colors() = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = MaterialTheme.colorScheme.primary,
        uncheckedThumbColor = Color.White,
        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
        uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
    )
}

object AppSliderDefaults {
    @Composable
    fun colors() = SliderDefaults.colors(
        thumbColor = Color.White,
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
        activeTickColor = Color.Transparent,
        inactiveTickColor = Color.Transparent,
    )
}

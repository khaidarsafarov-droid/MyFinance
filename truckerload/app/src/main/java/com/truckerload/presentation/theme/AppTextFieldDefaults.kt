package com.truckerload.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object AppTextFieldDefaults {
    @Composable
    fun outlined() = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedPlaceholderColor = MaterialTheme.colorScheme.outline,
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.outline,
        focusedPrefixColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedPrefixColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    )
}

object AppFilterChipDefaults {
    private val chipShape = RoundedCornerShape(SoftUiDimens.ChipRadius)

    @Composable
    fun shape() = chipShape

    @Composable
    fun colors() = FilterChipDefaults.filterChipColors(
        selectedContainerColor = SoftUiColors.PurpleLight,
        selectedLabelColor = SoftUiColors.PurpleEnd,
        containerColor = MaterialTheme.colorScheme.surface,
        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        selectedLeadingIconColor = SoftUiColors.PurpleEnd,
        selectedTrailingIconColor = SoftUiColors.PurpleEnd,
    )

    @Composable
    fun stateColors() = FilterChipDefaults.filterChipColors(
        selectedContainerColor = SoftUiColors.PurpleLight,
        selectedLabelColor = SoftUiColors.PurpleEnd,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        labelColor = SoftUiColors.TextSecondaryLight,
    )
}

object AppSwitchDefaults {
    @Composable
    fun colors() = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = SoftUiColors.PurpleStart,
        uncheckedThumbColor = Color.White,
        uncheckedTrackColor = SoftUiColors.SurfaceMuted,
        uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
    )
}

object AppSliderDefaults {
    @Composable
    fun colors() = SliderDefaults.colors(
        thumbColor = Color.White,
        activeTrackColor = SoftUiColors.PurpleStart,
        inactiveTrackColor = SoftUiColors.SurfaceMuted,
        activeTickColor = Color.Transparent,
        inactiveTickColor = Color.Transparent,
    )
}

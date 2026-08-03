package com.truckerload.presentation.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.truckerload.presentation.theme.UiDimens

/**
 * Ensures Material touch-target minimum (48dp) for tablet / phone tap targets.
 */
fun Modifier.touchTarget(
    minWidth: androidx.compose.ui.unit.Dp = UiDimens.TouchTarget,
    minHeight: androidx.compose.ui.unit.Dp = UiDimens.TouchTarget,
): Modifier = defaultMinSize(minWidth = minWidth, minHeight = minHeight)

/**
 * Touch-friendly click without hover ripple — tablets lack persistent hover.
 */
@Composable
fun Modifier.tabletClickable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    role: Role? = null,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    return touchTarget()
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick,
        )
}

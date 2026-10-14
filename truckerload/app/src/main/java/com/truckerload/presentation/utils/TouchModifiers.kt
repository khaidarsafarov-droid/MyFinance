package com.truckerload.presentation.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.truckerload.presentation.theme.UiDimens

/**
 * A custom row or chip: TalkBack treats it as a button, and the hit area is at least 48dp.
 * Keeps the normal ripple. Do not use this on a fixed-size grid cell.
 */
@Composable
fun Modifier.buttonTap(
    onClick: () -> Unit,
    enabled: Boolean = true,
): Modifier = minimumInteractiveComponentSize()
    .clickable(enabled = enabled, role = Role.Button, onClick = onClick)

/** Ensures a Material touch target of at least 48dp. */
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

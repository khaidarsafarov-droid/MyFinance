package com.truckerload.presentation.components

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.truckerload.presentation.theme.UiDimens

/**
 * Ensures interactive chrome meets the 48×48dp tablet/phone touch target.
 * Prefer this over hover-only affordances — tablets have no persistent hover.
 */
fun Modifier.touchTarget(
    minSize: Dp = UiDimens.TouchTarget,
): Modifier = this.then(
    Modifier.defaultMinSize(minWidth = minSize, minHeight = minSize),
)

/** Buttons / chips: enforce minimum hit area for touch. */
fun Modifier.interactiveChrome(): Modifier = this.then(
    Modifier
        .semantics { role = Role.Button }
        .sizeIn(minWidth = UiDimens.TouchTarget, minHeight = UiDimens.TouchTarget),
)

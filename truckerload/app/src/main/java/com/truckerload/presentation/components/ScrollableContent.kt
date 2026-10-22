package com.truckerload.presentation.components

import android.view.Gravity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import kotlin.math.min

/** Fraction of screen height a dialog body may occupy before it scrolls. */
const val DIALOG_BODY_MAX_HEIGHT_FRACTION = 0.55f

const val DIALOG_BODY_MIN_HEIGHT_DP = 240

fun dialogBodyMaxHeightDp(screenHeightDp: Int): Int =
    (screenHeightDp * DIALOG_BODY_MAX_HEIGHT_FRACTION).toInt()
        .coerceAtLeast(DIALOG_BODY_MIN_HEIGHT_DP)

/**
 * Vertical scroll for form / page columns that can exceed the window
 * (phone, keyboard, split-screen). Apply after scaffold padding / [fillMaxSize],
 * before inner content padding.
 */
@Composable
fun Modifier.verticalContentScroll(): Modifier =
    this
        .imePadding()
        .verticalScroll(rememberScrollState())

/**
 * Caps dialog body height and scrolls when fields do not fit.
 * [imePadding] is outside [verticalScroll] so the viewport ends above the keyboard
 * and a focused field can scroll into that viewport.
 */
@Composable
fun Modifier.dialogBodyScroll(): Modifier {
    val maxHeight = dialogBodyMaxHeightDp(LocalConfiguration.current.screenHeightDp).dp
    return this
        .imePadding()
        .heightIn(max = maxHeight)
        .verticalScroll(rememberScrollState())
}

/**
 * Keeps an [androidx.compose.material3.AlertDialog] in the band above the IME.
 * With the keyboard closed this is a no-op, so the dialog stays centered.
 * Compose dialogs center in the full window; shrinking only the text column
 * still leaves that column over the keyboard.
 */
@Composable
fun Modifier.dialogAboveIme(): Modifier {
    val view = LocalView.current
    val ime = WindowInsets.ime.getBottom(LocalDensity.current)
    SideEffect {
        val window = (view as? DialogWindowProvider)?.window
            ?: (view.parent as? DialogWindowProvider)?.window
            ?: return@SideEffect
        val params = window.attributes
        val topGravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        if (ime > 0) {
            if (params.gravity != topGravity || params.y != 0) {
                params.gravity = topGravity
                params.y = 0
                window.attributes = params
            }
        } else if (params.gravity != Gravity.CENTER || params.y != 0) {
            params.gravity = Gravity.CENTER
            params.y = 0
            window.attributes = params
        }
    }
    if (ime == 0) return this
    return this
        .consumeWindowInsets(WindowInsets.ime)
        .layout { measurable, constraints ->
            val safeMax = (constraints.maxHeight - ime).coerceAtLeast(0)
            val placeable = measurable.measure(
                constraints.copy(
                    minHeight = min(constraints.minHeight, safeMax),
                    maxHeight = min(constraints.maxHeight, safeMax),
                ),
            )
            val width = if (constraints.hasBoundedWidth) constraints.maxWidth else placeable.width
            val height = if (constraints.hasBoundedHeight) constraints.maxHeight else placeable.height
            val y = ((safeMax - placeable.height) / 2).coerceAtLeast(0)
            val x = ((width - placeable.width) / 2).coerceAtLeast(0)
            layout(width, height) {
                placeable.place(x, y)
            }
        }
}

/**
 * Column that fills the viewport when content is short (so [Arrangement.SpaceBetween]
 * still works) and scrolls when content is taller than the window.
 */
@Composable
fun FillViewportScrollColumn(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().imePadding()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(contentPadding),
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            content = content,
        )
    }
}

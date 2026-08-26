package com.truckerload.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

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

/** Caps dialog body height and scrolls when fields do not fit. */
@Composable
fun Modifier.dialogBodyScroll(): Modifier {
    val maxHeight = dialogBodyMaxHeightDp(LocalConfiguration.current.screenHeightDp).dp
    return this
        .heightIn(max = maxHeight)
        .verticalScroll(rememberScrollState())
        .imePadding()
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

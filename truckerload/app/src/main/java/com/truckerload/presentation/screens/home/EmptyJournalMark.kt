package com.truckerload.presentation.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.truckerload.presentation.theme.LocalTruckColors

/** A truck and a rate sheet for the empty journal. One drawing, no extra mascot. */
@Composable
internal fun EmptyJournalMark(modifier: Modifier = Modifier) {
    val tc = LocalTruckColors.current
    val ink = tc.AccentPrimary
    val sheet = tc.CardBackground
    val line = tc.TextSecondary
    Canvas(modifier.size(96.dp)) {
        val w = size.width
        val h = size.height
        drawLine(
            color = ink,
            start = Offset(w * 0.08f, h * 0.84f),
            end = Offset(w * 0.92f, h * 0.84f),
            strokeWidth = 3.dp.toPx(),
        )
        drawRoundRect(
            color = ink,
            topLeft = Offset(w * 0.06f, h * 0.50f),
            size = Size(w * 0.42f, h * 0.22f),
            cornerRadius = CornerRadius(8.dp.toPx()),
        )
        drawRoundRect(
            color = ink,
            topLeft = Offset(w * 0.44f, h * 0.38f),
            size = Size(w * 0.20f, h * 0.34f),
            cornerRadius = CornerRadius(6.dp.toPx()),
        )
        val wheel = 7.dp.toPx()
        drawCircle(tc.Background, wheel, Offset(w * 0.20f, h * 0.78f))
        drawCircle(ink, wheel, Offset(w * 0.20f, h * 0.78f), style = Stroke(2.dp.toPx()))
        drawCircle(tc.Background, wheel, Offset(w * 0.52f, h * 0.78f))
        drawCircle(ink, wheel, Offset(w * 0.52f, h * 0.78f), style = Stroke(2.dp.toPx()))
        drawRoundRect(
            color = sheet,
            topLeft = Offset(w * 0.58f, h * 0.12f),
            size = Size(w * 0.32f, h * 0.46f),
            cornerRadius = CornerRadius(4.dp.toPx()),
        )
        drawRoundRect(
            color = ink,
            topLeft = Offset(w * 0.58f, h * 0.12f),
            size = Size(w * 0.32f, h * 0.46f),
            cornerRadius = CornerRadius(4.dp.toPx()),
            style = Stroke(2.dp.toPx()),
        )
        val textLeft = w * 0.64f
        listOf(0.24f, 0.34f, 0.44f).forEach { y ->
            drawLine(
                color = line,
                start = Offset(textLeft, h * y),
                end = Offset(w * 0.84f, h * y),
                strokeWidth = 2.dp.toPx(),
            )
        }
    }
}

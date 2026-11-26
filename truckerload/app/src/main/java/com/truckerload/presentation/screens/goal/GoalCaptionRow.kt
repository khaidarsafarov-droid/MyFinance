package com.truckerload.presentation.screens.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Percent and pace captions on one line. The type size steps down only when
 * a long value such as "245% de l'objectif" would meet the pace caption.
 */
@Composable
internal fun GoalCaptionRow(percentText: String, expectedText: String, color: Color, modifier: Modifier = Modifier) {
    val base = MaterialTheme.typography.bodyMedium
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp),
    ) {
        val measurer = rememberTextMeasurer()
        val gapPx = with(LocalDensity.current) { 9.dp.roundToPx() }
        val maxPx = constraints.maxWidth
        var sizeSp = base.fontSize.value
        fun width(text: String, sp: Float, medium: Boolean): Int {
            val style = base.copy(
                fontSize = sp.sp,
                fontWeight = if (medium) FontWeight.Medium else FontWeight.Normal,
                fontFeatureSettings = "tnum",
            )
            return measurer.measure(text, style = style, maxLines = 1, softWrap = false).size.width
        }
        while (
            sizeSp > MIN_SP &&
            width(percentText, sizeSp, true) + width(expectedText, sizeSp, false) + gapPx > maxPx
        ) {
            sizeSp -= SIZE_STEP_SP
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = percentText,
                color = color,
                maxLines = 1,
                softWrap = false,
                style = base.copy(
                    fontSize = sizeSp.sp,
                    fontWeight = FontWeight.Medium,
                    fontFeatureSettings = "tnum",
                ),
            )
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .width(1.dp)
                    .height(14.dp)
                    .background(color.copy(alpha = 0.7f)),
            )
            Text(
                text = expectedText,
                color = color,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(start = 8.dp),
                style = base.copy(
                    fontSize = sizeSp.sp,
                    fontFeatureSettings = "tnum",
                ),
            )
        }
    }
}

private const val MIN_SP = 9f
private const val SIZE_STEP_SP = 0.5f

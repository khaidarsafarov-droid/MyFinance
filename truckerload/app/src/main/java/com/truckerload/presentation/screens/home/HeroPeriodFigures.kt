package com.truckerload.presentation.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckerload.presentation.theme.AppTypography

private val AmountMetricsGap = 12.dp
private val RpmLabelGap = 6.dp
private val StackedMetricsGap = 4.dp

/**
 * Period gross on the home hero.
 *
 * The amount always stays on a single line and is never ellipsized. It keeps the
 * hero size beside miles and RPM when that row fits. When it does not — a phone
 * showing something like $12,200.00 — the amount takes the full card width and
 * shrinks only as far as that width requires. Miles and RPM move under it.
 */
@Composable
internal fun HeroPeriodFigures(
    gross: String,
    miles: String,
    rpmValue: String,
    rpmLabel: String,
    onPrimary: Color,
    onPrimaryMuted: Color,
    modifier: Modifier = Modifier,
) {
    val amountStyle = AppTypography.HeroNumberOnDark
    val milesStyle = AppTypography.CaptionMuted.copy(color = onPrimaryMuted)
    val rpmStyle = AppTypography.HeroNumberCompact.copy(color = onPrimary)
    val labelStyle = AppTypography.CaptionMuted.copy(color = onPrimaryMuted)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val rowWidthPx = constraints.maxWidth
        val gapPx = with(density) { AmountMetricsGap.roundToPx() }
        val rpmGapPx = with(density) { RpmLabelGap.roundToPx() }
        val fitsBeside = remember(
            gross,
            miles,
            rpmValue,
            rpmLabel,
            amountStyle,
            milesStyle,
            rpmStyle,
            labelStyle,
            rowWidthPx,
            gapPx,
            rpmGapPx,
        ) {
            if (rowWidthPx == Constraints.Infinity) {
                true
            } else {
                val metricsPx = maxOf(
                    measurer.singleLineWidth(miles, milesStyle),
                    measurer.singleLineWidth(rpmValue, rpmStyle) +
                        rpmGapPx +
                        measurer.singleLineWidth(rpmLabel, labelStyle),
                )
                heroAmountFitsBeside(
                    grossWidthPx = measurer.singleLineWidth(gross, amountStyle),
                    metricsWidthPx = metricsPx,
                    gapPx = gapPx,
                    rowWidthPx = rowWidthPx,
                )
            }
        }
        if (fitsBeside) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(AmountMetricsGap),
            ) {
                SingleLineFitText(
                    text = gross,
                    style = amountStyle,
                    modifier = Modifier.weight(1f, fill = false),
                )
                HeroPeriodMetrics(
                    miles = miles,
                    rpmValue = rpmValue,
                    rpmLabel = rpmLabel,
                    milesStyle = milesStyle,
                    rpmStyle = rpmStyle,
                    labelStyle = labelStyle,
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                SingleLineFitText(
                    text = gross,
                    style = amountStyle,
                    modifier = Modifier.fillMaxWidth(),
                )
                HeroPeriodMetrics(
                    miles = miles,
                    rpmValue = rpmValue,
                    rpmLabel = rpmLabel,
                    milesStyle = milesStyle,
                    rpmStyle = rpmStyle,
                    labelStyle = labelStyle,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = StackedMetricsGap),
                )
            }
        }
    }
}

@Composable
private fun HeroPeriodMetrics(
    miles: String,
    rpmValue: String,
    rpmLabel: String,
    milesStyle: TextStyle,
    rpmStyle: TextStyle,
    labelStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = miles,
            style = milesStyle,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RpmLabelGap),
        ) {
            Text(
                text = rpmValue,
                style = rpmStyle,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
            Text(
                text = rpmLabel,
                style = labelStyle,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

/**
 * Draws [text] on one line, shrinking from [style]'s font size until the glyphs
 * fit [modifier]'s max width. The string is never ellipsized.
 */
@Composable
internal fun SingleLineFitText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    minFontSize: TextUnit = 11.sp,
) {
    val preferred = style.fontSize
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier = modifier) {
        val maxWidthPx = constraints.maxWidth
        val fitted = remember(text, style, maxWidthPx, minFontSize, preferred) {
            fitSingleLineFontSize(
                preferred = preferred,
                minFontSize = minFontSize,
                maxWidthPx = maxWidthPx,
                widthAt = { sizeSp ->
                    measurer.singleLineWidth(
                        text,
                        style.copy(fontSize = sizeSp.sp, lineHeight = TextUnit.Unspecified),
                    )
                },
            )
        }
        val lineHeight = if (preferred.isSpecifiedSp() && style.lineHeight.isSpecifiedSp() && preferred.value > 0f) {
            (fitted.value * (style.lineHeight.value / preferred.value)).sp
        } else {
            TextUnit.Unspecified
        }
        Text(
            text = text,
            style = style.copy(fontSize = fitted, lineHeight = lineHeight),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
    }
}

internal fun heroAmountFitsBeside(
    grossWidthPx: Int,
    metricsWidthPx: Int,
    gapPx: Int,
    rowWidthPx: Int,
): Boolean {
    if (rowWidthPx <= 0) return false
    return grossWidthPx + gapPx + metricsWidthPx <= rowWidthPx
}

/**
 * Largest size in `[min, max]` for which [fits] is true.
 * Returns [min] when nothing in the range fits, and [max] when [max] already fits.
 */
internal fun largestFittingSize(
    min: Float,
    max: Float,
    fits: (Float) -> Boolean,
): Float {
    if (min >= max) return if (fits(max)) max else min
    if (fits(max)) return max
    if (!fits(min)) return min
    var low = min
    var high = max
    repeat(24) {
        if (high - low < 0.05f) return low
        val mid = (low + high) / 2f
        if (fits(mid)) low = mid else high = mid
    }
    return low
}

internal fun fitSingleLineFontSize(
    preferred: TextUnit,
    minFontSize: TextUnit,
    maxWidthPx: Int,
    widthAt: (Float) -> Int,
): TextUnit {
    if (!preferred.isSpecifiedSp() || maxWidthPx == Constraints.Infinity) return preferred
    if (maxWidthPx <= 0) return if (minFontSize.isSpecifiedSp()) minFontSize else preferred
    val maxSp = preferred.value
    val floorSp = if (minFontSize.isSpecifiedSp()) minFontSize.value.coerceAtMost(maxSp) else maxSp
    val limit = (maxWidthPx - 1).coerceAtLeast(0)
    if (widthAt(floorSp) <= limit) {
        return largestFittingSize(floorSp, maxSp) { widthAt(it) <= limit }.sp
    }
    return largestFittingSize(1f, floorSp) { widthAt(it) <= limit }.sp
}

private fun TextUnit.isSpecifiedSp(): Boolean = type == TextUnitType.Sp

private fun TextMeasurer.singleLineWidth(
    text: String,
    style: TextStyle,
): Int {
    if (text.isEmpty()) return 0
    return measure(
        text = text,
        style = style,
        overflow = TextOverflow.Visible,
        softWrap = false,
        maxLines = 1,
    ).size.width
}

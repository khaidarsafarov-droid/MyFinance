package com.truckerload.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.theme.LocalTruckColors

@Composable
fun ComparisonIndicator(
    currentValue: Double,
    previousValue: Double?,
    label: String,
    prefix: String = "",
    suffix: String = "",
    isExpense: Boolean = false,
    vsLabel: String = "",
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val tc = LocalTruckColors.current
    val resolvedVsLabel = if (vsLabel.isBlank()) stringResource(R.string.stats_vs_prev_week) else vsLabel
    val (percentChange, arrow, color) = when {
        previousValue == null || previousValue == 0.0 -> Triple(null, "", tc.TextSecondary)
        else -> {
            val change = ((currentValue - previousValue) / previousValue) * 100
            val (c, a) = when {
                change > 0 && !isExpense -> tc.AccentProfit to "↑"
                change < 0 && !isExpense -> tc.AccentExpense to "↓"
                change > 0 && isExpense -> tc.AccentExpense to "↑"
                change < 0 && isExpense -> tc.AccentProfit to "↓"
                else -> tc.TextSecondary to ""
            }
            Triple(change, a, c)
        }
    }
    val animatedColor by animateColorAsState(color, animationSpec = tween(300))

    val cardModifier = if (onClick != null) {
        modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    } else modifier

    Card(
        modifier = cardModifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = tc.CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$prefix${formatValue(currentValue)}$suffix",
                    style = MaterialTheme.typography.titleMedium,
                    color = tc.TextPrimary,
                    fontFamily = FontFamily.SansSerif
                )
                if (percentChange != null) {
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "$arrow ${formatPercent(percentChange)} vs $resolvedVsLabel",
                        style = MaterialTheme.typography.labelMedium,
                        color = tc.TextSecondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(animatedColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = tc.TextLabel,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun formatValue(v: Double): String = when {
    v >= 1000 || v <= -1000 -> String.format("%,.0f", v)
    v >= 1 || v <= -1 -> String.format("%,.2f", v)
    else -> String.format("%.2f", v)
}

private fun formatPercent(v: Double): String = String.format("%+.1f%%", v)

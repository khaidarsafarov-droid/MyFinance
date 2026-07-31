package com.truckerload.presentation.screens.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.truckerload.presentation.components.TlTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.components.SoftCard
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.SoftUiDimens

@Composable
internal fun StatsAiOverlay(
    insight: String,
    actions: List<String>,
    onDismiss: () -> Unit,
    onOpenAdvisor: () -> Unit,
    onAction: (String) -> Unit
) {
    val tc = LocalTruckColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            )
    ) {
        SoftCard(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp)
                .fillMaxWidth(0.92f),
            onClick = {},
            contentPadding = 20.dp,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.stats_ai_card_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = tc.AccentPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_close))
                    }
                }
                Text(
                    text = insight,
                    style = MaterialTheme.typography.bodyMedium,
                    color = tc.TextPrimary
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    actions.take(3).forEach { action ->
                        FilterChip(
                            selected = false,
                            onClick = { onAction(action) },
                            label = { Text(action) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = tc.AccentPrimary.copy(alpha = 0.12f),
                                labelColor = tc.AccentSecondary
                            )
                        )
                    }
                }
                TextButton(onClick = onOpenAdvisor) {
                    Text(stringResource(R.string.stats_open_advisor))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.common_close))
                }
            }
        }
    }
}

@Composable
internal fun ContextCard(month: Int, year: Int) {
    val tc = LocalTruckColors.current
    SoftCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = tc.TextSecondary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                stringResource(R.string.stats_context_month_year, monthShortLabel(month), year),
                style = MaterialTheme.typography.titleMedium,
                color = tc.TextPrimary
            )
        }
    }
}

@Composable
internal fun SmartHeader(period: StatsPeriod, userName: String = "") {
    val tc = LocalTruckColors.current
    val periodLabel = when (period) {
        StatsPeriod.WEEK -> stringResource(R.string.common_week)
        StatsPeriod.MONTH -> stringResource(R.string.common_month)
        StatsPeriod.YEAR -> stringResource(R.string.common_year)
    }
    SoftCard {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (userName.isNotBlank()) {
                    stringResource(R.string.stats_header_greeting_named, userName)
                } else {
                    stringResource(R.string.stats_header_greeting)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = tc.TextPrimary
            )
            Text(
                stringResource(R.string.stats_header_period_format, periodLabel),
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary
            )
        }
    }
}

@Composable
internal fun CerebrasInsightCard(
    visible: Boolean,
    insight: String,
    actions: List<String>,
    onActionClick: (String) -> Unit
) {
    val tc = LocalTruckColors.current
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 320))
    ) {
        SoftCard(
            modifier = Modifier.border(1.dp, tc.GlassBorder, RoundedCornerShape(SoftUiDimens.CardRadius)),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.stats_ai_card_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = tc.AccentPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = insight,
                    style = MaterialTheme.typography.bodyMedium,
                    color = tc.TextPrimary
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    actions.take(3).forEach { action ->
                        FilterChip(
                            selected = false,
                            onClick = { onActionClick(action) },
                            label = { Text(action) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = tc.AccentPrimary.copy(alpha = 0.12f),
                                labelColor = tc.AccentSecondary
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun EmptyMagicBlock() {
    val tc = LocalTruckColors.current
    SoftCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                stringResource(R.string.stats_empty_magic_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                stringResource(R.string.stats_empty_magic_forecast),
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

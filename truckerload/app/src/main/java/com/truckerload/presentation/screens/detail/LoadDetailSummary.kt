package com.truckerload.presentation.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckerload.R
import com.truckerload.domain.model.Load
import com.truckerload.domain.units.DisplayUnits
import com.truckerload.presentation.components.formatJournalDate
import com.truckerload.presentation.components.formatRpm
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.loadSharedBounds
import com.truckerload.presentation.utils.MoneyFormat
import kotlin.math.ceil

@Composable
internal fun LoadDetailSummary(
    load: Load,
    modifier: Modifier = Modifier,
) {
    val milesLabel = stringResource(
        if (DisplayUnits.isMetric) R.string.load_detail_stat_miles_km else R.string.load_detail_stat_miles,
    )
    val gross = if (load.totalRate.cents % 100L == 0L) {
        MoneyFormat.formatCurrency(load.totalRate, decimals = 0)
    } else {
        MoneyFormat.formatCurrency(load.totalRate, decimals = 2)
    }
    val miles = MoneyFormat.formatDistanceNumber(load.drivenMiles)
    val rpm = formatRpm(
        load.totalRate,
        load.drivenMiles,
        stringResource(R.string.rpm_per_mile_format),
    )
    val duration = if (load.durationDays > 0) {
        val days = ceil(load.durationDays).toInt().coerceAtLeast(1)
        pluralStringResource(R.plurals.load_card_duration_days, days, days)
    } else {
        "—"
    }
    val pace = if (load.pace.isPositive()) {
        val decimals = if (load.pace.cents % 100L == 0L) 0 else 2
        stringResource(
            R.string.load_card_per_day,
            MoneyFormat.formatCurrency(load.pace, decimals = decimals),
        )
    } else {
        "—"
    }
    val stops = (load.stopCount.takeIf { it > 0 } ?: (load.puCount + load.delCount)).toString()
    BentoGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .loadSharedBounds(load.id),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(
                        R.string.load_detail_route,
                        load.pointA.ifBlank { "—" },
                        load.pointB.ifBlank { "—" },
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatJournalDate(load.date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DetailMetricCell(
                        label = stringResource(R.string.load_detail_stat_gross),
                        value = gross,
                        modifier = Modifier.weight(1f),
                    )
                    DetailMetricCell(
                        label = milesLabel,
                        value = miles,
                        modifier = Modifier.weight(1f),
                    )
                    DetailMetricCell(
                        label = stringResource(R.string.load_detail_stat_rpm),
                        value = rpm,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DetailMetricCell(
                        label = stringResource(R.string.load_detail_stat_duration),
                        value = duration,
                        modifier = Modifier.weight(1f),
                    )
                    DetailMetricCell(
                        label = stringResource(R.string.load_detail_stat_pace),
                        value = pace,
                        modifier = Modifier.weight(1f),
                    )
                    DetailMetricCell(
                        label = stringResource(R.string.load_detail_stat_stops),
                        value = stops,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailMetricCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.heightIn(min = 64.dp),
        shape = RoundedCornerShape(16.dp),
        color = cs.surface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 13.sp),
                color = cs.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum",
                    color = cs.primary,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

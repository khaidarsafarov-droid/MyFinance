package com.truckerload.presentation.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.StopType
import com.truckerload.presentation.components.LoadRouteBlock
import com.truckerload.presentation.components.RpmValueBadge
import com.truckerload.presentation.components.formatJournalDate
import com.truckerload.presentation.components.formatRpm
import com.truckerload.presentation.components.rpmTier
import com.truckerload.presentation.di.LocalRpmThresholdsStore
import com.truckerload.presentation.screens.add.labelRes
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.loadSharedBounds
import com.truckerload.presentation.utils.MoneyFormat
import kotlin.math.ceil

@Composable
internal fun LoadDetailSummary(load: Load) {
    val thresholds by LocalRpmThresholdsStore.current.thresholds.collectAsStateWithLifecycle()
    val milesLabel = MoneyFormat.formatNumber(load.drivenMiles)
    val rpmValue = if (load.drivenMiles > 0) {
        load.totalRate.cents / 100.0 / load.drivenMiles
    } else {
        null
    }
    val tier = rpmTier(rpmValue, thresholds.minProfit, thresholds.targetProfit)
    val equipment = load.equipmentType?.let { stringResource(it.labelRes()) }
    BentoGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .loadSharedBounds(load.id),
    ) {
        LoadRouteBlock(
            dateLabel = formatJournalDate(load.date),
            tripLabel = "",
            origin = load.pointA.ifBlank { "—" },
            destination = load.pointB.ifBlank { "—" },
            rate = MoneyFormat.formatCurrency(load.totalRate),
            meta = "",
            equipment = equipment,
            modifier = Modifier.padding(20.dp),
            trailing = {
                if (tier != null) {
                    RpmValueBadge(
                        rpmText = formatRpm(
                            load.totalRate,
                            load.drivenMiles,
                            stringResource(R.string.rpm_per_mile_format),
                        ),
                        tier = tier,
                    )
                }
            },
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MetricTile(
            label = stringResource(R.string.load_detail_stat_miles),
            value = milesLabel,
            caption = if (load.deadheadMiles > 0.0) {
                "${MoneyFormat.formatNumber(load.totalMiles)} ${stringResource(R.string.load_detail_miles_loaded)}"
            } else {
                null
            },
            modifier = Modifier.weight(1f),
        )
        MetricTile(
            label = stringResource(R.string.load_detail_stat_duration),
            value = if (load.durationDays > 0) {
                val days = ceil(load.durationDays).toInt().coerceAtLeast(1)
                pluralStringResource(R.plurals.load_card_duration_days, days, days)
            } else {
                "—"
            },
            modifier = Modifier.weight(1f),
        )
        MetricTile(
            label = stringResource(R.string.load_detail_stat_pace),
            value = if (load.pace.isPositive()) {
                stringResource(
                    R.string.load_card_per_day,
                    MoneyFormat.formatCurrency(load.pace, decimals = 2),
                )
            } else {
                "—"
            },
            modifier = Modifier.weight(1f),
        )
    }
    val (puValue, puCaption) = stopPrimary(load, StopType.PU)
    val (delValue, delCaption) = stopPrimary(load, StopType.DEL)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MetricTile(
            label = stringResource(R.string.load_detail_stat_pu),
            value = puValue,
            caption = puCaption,
            modifier = Modifier.weight(1f),
        )
        MetricTile(
            label = stringResource(R.string.load_detail_stat_del),
            value = delValue,
            caption = delCaption,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.heightIn(min = 84.dp),
        shape = RoundedCornerShape(16.dp),
        color = cs.surfaceVariant.copy(alpha = 0.55f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = cs.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum",
                ),
                color = cs.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!caption.isNullOrBlank()) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun stopPrimary(load: Load, type: StopType): Pair<String, String?> {
    val matches = load.stops.filter { it.type == type }
    val stop = if (type == StopType.PU) matches.firstOrNull() else matches.lastOrNull()
    val time = stop?.scheduledTime?.trim().orEmpty()
    val count = matches.size.coerceAtLeast(
        if (type == StopType.PU) load.puCount else load.delCount,
    ).toString()
    return if (time.isNotBlank()) time to count else count to null
}

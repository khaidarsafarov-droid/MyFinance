package com.truckerload.presentation.screens.map

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckerload.R
import com.truckerload.domain.crowd.CrowdLaneAggregate
import com.truckerload.domain.crowd.CrowdRateReport
import com.truckerload.presentation.components.GoogleMapsHeatmapCard
import com.truckerload.presentation.components.getStateDisplayName
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.di.LocalSelectedStateStore
import com.truckerload.presentation.theme.AppFilterChipDefaults
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onBack: () -> Unit = {},
    embedded: Boolean = false,
) {
    val tc = LocalTruckColors.current
    val loadRepository = LocalLoadRepository.current
    val selectedStateStore = LocalSelectedStateStore.current
    val viewModel: MapViewModel = viewModel(
        factory = MapViewModel.Factory(loadRepository, selectedStateStore),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (embedded) {
        MapScreenBody(
            padding = PaddingValues(0.dp),
            uiState = uiState,
            viewModel = viewModel,
            showToolbarTitle = true,
        )
    } else {
        Scaffold(
            containerColor = BentoGlassTheme.ScreenBackground,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.map_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = tc.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(UiDimens.ToolbarTouchTarget),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.common_back),
                                tint = tc.TextPrimary,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BentoGlassTheme.ScreenBackground,
                        titleContentColor = tc.TextPrimary,
                    ),
                )
            },
        ) { padding ->
            MapScreenBody(
                padding = padding,
                uiState = uiState,
                viewModel = viewModel,
                showToolbarTitle = false,
            )
        }
    }
}

@Composable
private fun MapScreenBody(
    padding: PaddingValues,
    uiState: MapUiState,
    viewModel: MapViewModel,
    showToolbarTitle: Boolean,
) {
    val tc = LocalTruckColors.current
    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = tc.AccentPrimary)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (showToolbarTitle) {
            Text(
                text = stringResource(R.string.map_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = tc.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        MapPeriodSelector(
            selected = uiState.period,
            onSelect = viewModel::setPeriod,
        )

        MapMetaRow(
            period = uiState.period,
            tripCount = uiState.totalReports,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = tc.TextSecondary,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(18.dp),
            )
            Text(
                text = stringResource(R.string.map_my_rates_hint),
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
                modifier = Modifier.weight(1f),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp),
        ) {
            GoogleMapsHeatmapCard(
                metrics = uiState.metrics,
                selectedCode = uiState.selectedStateCode,
                refreshing = false,
                onStateSelected = viewModel::setSelectedState,
                modifier = Modifier.fillMaxSize(),
            )
        }

        val summary = uiState.stateSummary
        if (summary != null && summary.stateCode.isNotBlank()) {
            IconLabeledTitle(
                icon = Icons.Filled.Place,
                text = stringResource(
                    R.string.map_crowd_state_header,
                    getStateDisplayName(summary.stateCode),
                    summary.stateCode,
                ),
            )
            IconLabeledBody(
                icon = Icons.Filled.Speed,
                text = stringResource(
                    R.string.map_crowd_state_avg,
                    String.format(Locale.getDefault(), "%.2f", summary.avgOutboundRpm),
                    summary.outboundTrips,
                ),
            )
            Spacer(Modifier.height(2.dp))
            IconLabeledTitle(
                icon = Icons.Filled.Schedule,
                text = stringResource(R.string.map_crowd_recent_title),
            )
            if (summary.recent.isEmpty()) {
                Text(
                    text = stringResource(periodEmptyRecentRes(uiState.period)),
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary,
                    modifier = Modifier.padding(start = 26.dp),
                )
            } else {
                summary.recent.forEach { report ->
                    MyRecentRow(report = report)
                }
            }
        }

        if (uiState.topLanes.isNotEmpty() && uiState.selectedStateCode.isBlank()) {
            IconLabeledTitle(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                text = stringResource(periodTopLanesRes(uiState.period)),
            )
            uiState.topLanes.forEach { lane ->
                TopLaneRow(lane = lane)
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MapPeriodSelector(
    selected: MapPeriod,
    onSelect: (MapPeriod) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MapPeriod.entries.forEach { period ->
            val (icon, labelRes) = when (period) {
                MapPeriod.WEEK -> Icons.Filled.Today to R.string.map_period_week
                MapPeriod.MONTH -> Icons.Filled.DateRange to R.string.map_period_month
                MapPeriod.YEAR -> Icons.Filled.CalendarMonth to R.string.map_period_year
            }
            FilterChip(
                selected = period == selected,
                onClick = { onSelect(period) },
                label = {
                    Text(
                        text = stringResource(labelRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                colors = AppFilterChipDefaults.colors(),
            )
        }
    }
}

@Composable
private fun MapMetaRow(
    period: MapPeriod,
    tripCount: Int,
) {
    val tc = LocalTruckColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.LocalShipping,
            contentDescription = null,
            tint = tc.AccentPrimary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(periodSubtitleRes(period), tripCount),
            style = MaterialTheme.typography.bodyMedium,
            color = tc.TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

@Composable
private fun IconLabeledTitle(
    icon: ImageVector,
    text: String,
) {
    val tc = LocalTruckColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tc.AccentPrimary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = tc.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun IconLabeledBody(
    icon: ImageVector,
    text: String,
) {
    val tc = LocalTruckColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tc.TextSecondary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = tc.TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TopLaneRow(lane: CrowdLaneAggregate) {
    val tc = LocalTruckColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.LocalShipping,
            contentDescription = null,
            tint = tc.TextSecondary,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(
                R.string.map_crowd_lane_row,
                lane.fromState,
                lane.toState,
                String.format(Locale.getDefault(), "%.2f", lane.avgRpm),
                lane.tripCount,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = tc.TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MyRecentRow(report: CrowdRateReport) {
    val tc = LocalTruckColors.current
    val now = System.currentTimeMillis()
    val age = formatCrowdAge(now - report.reportedAtMillis)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Schedule,
            contentDescription = null,
            tint = tc.TextSecondary,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(
                R.string.map_my_recent_row,
                report.fromState,
                report.toState,
                String.format(Locale.getDefault(), "%.2f", report.rpm),
                age,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = tc.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun formatCrowdAge(ageMs: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(ageMs.coerceAtLeast(0))
    return when {
        hours < 1 -> stringResource(R.string.map_crowd_age_minutes, (ageMs / 60_000L).coerceAtLeast(1))
        hours < 24 -> stringResource(R.string.map_crowd_age_hours, hours)
        else -> stringResource(
            R.string.map_crowd_age_days,
            TimeUnit.MILLISECONDS.toDays(ageMs).coerceAtLeast(1),
        )
    }
}

private fun periodSubtitleRes(period: MapPeriod): Int = when (period) {
    MapPeriod.WEEK -> R.string.map_subtitle_period_week
    MapPeriod.MONTH -> R.string.map_subtitle_period_month
    MapPeriod.YEAR -> R.string.map_subtitle_period_year
}

private fun periodTopLanesRes(period: MapPeriod): Int = when (period) {
    MapPeriod.WEEK -> R.string.map_crowd_top_lanes_week
    MapPeriod.MONTH -> R.string.map_crowd_top_lanes_month
    MapPeriod.YEAR -> R.string.map_crowd_top_lanes_year
}

private fun periodEmptyRecentRes(period: MapPeriod): Int = when (period) {
    MapPeriod.WEEK -> R.string.map_crowd_recent_empty_week
    MapPeriod.MONTH -> R.string.map_crowd_recent_empty_month
    MapPeriod.YEAR -> R.string.map_crowd_recent_empty_year
}

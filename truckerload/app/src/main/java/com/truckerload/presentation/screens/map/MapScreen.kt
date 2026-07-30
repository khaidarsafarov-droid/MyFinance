package com.truckerload.presentation.screens.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckerload.R
import com.truckerload.domain.crowd.CrowdRateReport
import com.truckerload.presentation.components.GoogleMapsHeatmapCard
import com.truckerload.presentation.components.getStateDisplayName
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.di.LocalSelectedStateStore
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.ForestScreenTitle
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
        )
    } else {
        Scaffold(
            containerColor = BentoGlassTheme.ScreenBackground,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            ForestScreenTitle(stringResource(R.string.map_title))
                            Text(
                                text = stringResource(
                                    R.string.map_subtitle_my_week,
                                    uiState.totalReports,
                                ),
                                style = AppTypography.Subtitle,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.size(UiDimens.ToolbarTouchTarget)) {
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
            )
        }
    }
}

@Composable
private fun MapScreenBody(
    padding: PaddingValues,
    uiState: MapUiState,
    viewModel: MapViewModel,
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
        Text(
            text = stringResource(R.string.map_my_rates_hint),
            style = MaterialTheme.typography.bodySmall,
            color = tc.TextSecondary,
        )

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
            Text(
                text = stringResource(
                    R.string.map_crowd_state_header,
                    getStateDisplayName(summary.stateCode),
                    summary.stateCode,
                ),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = tc.TextPrimary,
            )
            Text(
                text = stringResource(
                    R.string.map_crowd_state_avg,
                    String.format(Locale.getDefault(), "%.2f", summary.avgOutboundRpm),
                    summary.outboundTrips,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = tc.TextSecondary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.map_crowd_recent_title),
                style = MaterialTheme.typography.titleSmall,
                color = tc.TextPrimary,
            )
            if (summary.recent.isEmpty()) {
                Text(
                    text = stringResource(R.string.map_crowd_recent_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary,
                )
            } else {
                summary.recent.forEach { report ->
                    MyRecentRow(report = report)
                }
            }
        }

        if (uiState.topLanes.isNotEmpty() && uiState.selectedStateCode.isBlank()) {
            Text(
                text = stringResource(R.string.map_crowd_top_lanes),
                style = MaterialTheme.typography.titleSmall,
                color = tc.TextPrimary,
            )
            uiState.topLanes.forEach { lane ->
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
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MyRecentRow(report: CrowdRateReport) {
    val tc = LocalTruckColors.current
    val now = System.currentTimeMillis()
    val age = formatCrowdAge(now - report.reportedAtMillis)
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    )
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

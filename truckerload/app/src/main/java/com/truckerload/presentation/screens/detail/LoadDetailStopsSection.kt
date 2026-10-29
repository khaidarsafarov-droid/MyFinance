package com.truckerload.presentation.screens.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.model.Stop
import com.truckerload.presentation.components.StopTimeline
import com.truckerload.presentation.theme.BentoGlassCard

@Composable
internal fun LoadDetailStopsSection(
    stops: List<Stop>,
    modifier: Modifier = Modifier,
) {
    BentoGlassCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.load_detail_stops_section),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (stops.isEmpty()) {
                Text(
                    text = stringResource(R.string.load_detail_stops_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                StopTimeline(
                    stops = stops,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

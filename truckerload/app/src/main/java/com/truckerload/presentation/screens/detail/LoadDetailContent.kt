package com.truckerload.presentation.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.data.local.entities.PhotoEntity
import com.truckerload.data.local.entities.ScanEntity
import com.truckerload.domain.model.Load
import com.truckerload.presentation.theme.BentoGlassCard

@Composable
internal fun LoadDetailLoadedContent(
    load: Load,
    photos: List<PhotoEntity>,
    scans: List<ScanEntity>,
    onEditMiles: () -> Unit,
    onEditFinish: () -> Unit,
    onSaveDeadhead: (Double) -> Unit,
    onDisputeChanged: (Load) -> Unit,
    onPhotoClick: (String) -> Unit,
    onCamera: () -> Unit,
    onScanner: () -> Unit,
    onScanClick: (ScanEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LoadDetailSummary(load = load)
        LoadDetailActionChips(
            load = load,
            onEditMiles = onEditMiles,
            onEditFinish = onEditFinish,
            onSaveDeadhead = onSaveDeadhead,
            onDisputeChanged = onDisputeChanged,
        )
        LoadDetailStopsSection(stops = load.stops)
        LoadDetailMediaSection(
            photos = photos,
            scans = scans,
            onPhotoClick = onPhotoClick,
            onCamera = onCamera,
            onScanner = onScanner,
            onScanClick = onScanClick,
        )
        BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.load_raw_message),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    load.rawMessage.take(500).ifEmpty { "—" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

package com.truckerload.presentation.screens.attach

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalShipping
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.model.Load
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class AttachPickMode {
    CAMERA,
    SCANNER,
}

/**
 * Picker shown when Camera / Scan is opened from the home-screen widget.
 * Offers the three most recent loads so media is saved onto a chosen trip.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachLoadPickScreen(
    mode: AttachPickMode,
    onLoadSelected: (Load) -> Unit,
    onCancel: () -> Unit,
    onAddLoad: () -> Unit = onCancel,
    loadRepository: LoadRepository = LocalLoadRepository.current,
) {
    val tc = LocalTruckColors.current
    var loads by remember { mutableStateOf<List<Load>?>(null) }

    LaunchedEffect(Unit) {
        loads = withContext(Dispatchers.IO) {
            loadRepository.getLoadsForLinking(limit = 3)
        }
    }

    Scaffold(
        containerColor = BentoGlassTheme.ScreenBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.select_load),
                        color = tc.TextPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
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
        when {
            loads == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            }
            loads.isNullOrEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.attach_pick_empty),
                        style = MaterialTheme.typography.titleMedium,
                        color = tc.TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.attach_pick_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tc.TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = onAddLoad) {
                        Text(stringResource(R.string.home_add_load_button))
                    }
                }
            }
            else -> {
                val list = loads.orEmpty()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Text(
                            text = stringResource(
                                when (mode) {
                                    AttachPickMode.CAMERA -> R.string.attach_pick_camera_hint
                                    AttachPickMode.SCANNER -> R.string.attach_pick_scanner_hint
                                },
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = tc.TextSecondary,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    items(list, key = { it.id }) { load ->
                        AttachLoadRow(load = load, onClick = { onLoadSelected(load) })
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachLoadRow(
    load: Load,
    onClick: () -> Unit,
) {
    val tc = LocalTruckColors.current
    val routeLabel = load.route.ifBlank {
        listOf(load.pointA, load.pointB).filter { it.isNotBlank() }.joinToString(" → ")
    }.ifBlank { load.tripId }
    BentoGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.LocalShipping,
                contentDescription = null,
                tint = tc.AccentPrimary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = load.tripId.ifBlank { load.id },
                    style = MaterialTheme.typography.titleSmall,
                    color = tc.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = routeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        append(load.date)
                        if (load.totalMiles > 0) {
                            append(" · ")
                            append(String.format(Locale.US, "%,.0f mi", load.totalMiles))
                        }
                        if (load.totalRate > 0) {
                            append(" · ")
                            append(String.format(Locale.US, "$%,.0f", load.totalRate))
                        }
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = tc.TextSecondary,
                )
            }
        }
    }
}

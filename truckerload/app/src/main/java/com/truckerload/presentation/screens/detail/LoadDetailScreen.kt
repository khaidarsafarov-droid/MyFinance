package com.truckerload.presentation.screens.detail

import com.truckerload.presentation.icons.AppIcons

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.truckerload.presentation.utils.rememberDecodedBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.truckerload.R
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.ActualFinishDate
import com.truckerload.domain.model.effectiveFinishDate
import com.truckerload.domain.model.formatDurationDays
import com.truckerload.domain.model.formatLoadRoute
import com.truckerload.domain.model.formatPacePerDay
import com.truckerload.domain.model.withRouteMetrics
import com.truckerload.presentation.components.DisputeSection
import com.truckerload.presentation.components.OneUiBottomActionBar
import com.truckerload.presentation.components.StatBox
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.components.formatRpm
import com.truckerload.presentation.components.StopTimeline
import com.truckerload.presentation.di.LocalPhotoRepository
import com.truckerload.presentation.di.LocalScanRepository
import com.truckerload.presentation.screens.privacy.PrivacyTrustBadge
import com.truckerload.utils.ShareHelper
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.OneUiTokens
import com.truckerload.presentation.theme.UiDimens
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadDetailScreen(
    loadId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onEditFinish: () -> Unit,
    onDelete: () -> Unit,
    onPhotoClick: (String) -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val photoRepository = LocalPhotoRepository.current
    val scanRepository = LocalScanRepository.current
    val viewModel: LoadDetailViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val linkedPhotos by photoRepository.watchPhotosByLoadId(loadId).collectAsStateWithLifecycle(initialValue = emptyList())
    val linkedScans by scanRepository.watchScansByLoadId(loadId).collectAsStateWithLifecycle(initialValue = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    val deleteFailed = stringResource(R.string.load_delete_failed)
    val saveErrorEmpty = stringResource(R.string.common_save_error, "")
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    // Reload after returning from Edit (finish date / metrics may have changed).
    DisposableEffect(lifecycleOwner, loadId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                LoadDetailEvent.Deleted -> onDelete()
                is LoadDetailEvent.Message -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    Scaffold(
        containerColor = BentoGlassTheme.ScreenBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            uiState.load?.tripId ?: stringResource(R.string.load_detail_title),
                            color = tc.TextPrimary,
                        )
                        PrivacyTrustBadge(onClick = onOpenPrivacy)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(UiDimens.ToolbarTouchTarget)) {
                        Icon(
                            AppIcons.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = tc.TextPrimary,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(UiDimens.ToolbarTouchTarget),
                    ) {
                        Icon(
                            AppIcons.Delete,
                            contentDescription = stringResource(R.string.load_detail_cd_delete),
                            tint = tc.AccentExpense,
                        )
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = BentoGlassTheme.ScreenBackground,
                    titleContentColor = tc.TextPrimary,
                ),
            )
        },
        bottomBar = {
            OneUiBottomActionBar {
                Button(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text(stringResource(R.string.common_edit))
                }
            }
        },
    ) { padding ->
        when {
            uiState.loadError != null -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(uiState.loadError ?: stringResource(R.string.load_error_generic), color = tc.TextPrimary)
            }
            uiState.isLoading || uiState.load == null -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = tc.AccentPrimary)
            }
            else -> {
                val raw = uiState.load ?: return@Scaffold
                // Live recompute so duration/pace always reflect actualFinishDate + stops.
                val l = remember(raw) { raw.withRouteMetrics() }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                formatLoadRoute(l),
                                style = MaterialTheme.typography.titleMedium,
                                color = tc.TextPrimary,
                            )
                            Text(
                                stringResource(
                                    R.string.load_detail_miles_stops,
                                    l.totalMiles,
                                    l.stopCount.takeIf { it > 0 } ?: (l.puCount + l.delCount),
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = tc.TextSecondary,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StatBox(
                            title = stringResource(R.string.load_detail_stat_total_rate),
                            value = "$${String.format(Locale.US, "%.2f", l.totalRate)}",
                            modifier = Modifier.weight(1f),
                            hero = true,
                        )
                        StatBox(
                            title = stringResource(R.string.load_detail_stat_miles),
                            value = "${String.format(Locale.US, "%.2f", l.totalMiles)}",
                            modifier = Modifier.weight(1f),
                        )
                        StatBox(
                            title = stringResource(R.string.load_detail_stat_rpm),
                            value = formatRpm(l.totalRate, l.totalMiles, stringResource(R.string.rpm_per_mile_format)),
                            modifier = Modifier.weight(1f),
                            hero = true,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StatBox(
                            title = stringResource(R.string.load_detail_stat_duration),
                            value = if (l.durationDays > 0) formatDurationDays(l.durationDays) else "—",
                            modifier = Modifier.weight(1f),
                        )
                        StatBox(
                            title = stringResource(R.string.load_detail_stat_pace),
                            value = if (l.pace > 0) formatPacePerDay(l.pace) else "—",
                            modifier = Modifier.weight(1f),
                        )
                        StatBox(title = stringResource(R.string.load_detail_stat_pu), value = "${l.puCount}", modifier = Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StatBox(
                            title = stringResource(R.string.load_detail_stat_del),
                            value = "${l.delCount}",
                            modifier = Modifier.weight(1f),
                        )
                    }
                    ActualFinishSection(
                        load = l,
                        onPickClick = onEditFinish,
                        onClearClick = {
                            viewModel.setActualFinishDate(null, saveErrorEmpty)
                        },
                    )
                    if (linkedPhotos.isNotEmpty()) {
                        BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    stringResource(R.string.load_detail_photos),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = tc.TextPrimary,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(linkedPhotos, key = { it.id }) { photo ->
                                        val thumb = rememberDecodedBitmap(photo.filePath)
                                        if (thumb != null) {
                                            Image(
                                                bitmap = thumb,
                                                contentDescription = photo.fileName,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .width(96.dp)
                                                    .height(96.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable { onPhotoClick(photo.id) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (linkedScans.isNotEmpty()) {
                        BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    stringResource(R.string.load_detail_scans),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = tc.TextPrimary,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )
                                linkedScans.forEach { scan ->
                                    Text(
                                        text = scan.fileName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = tc.TextPrimary,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                ShareHelper(context).sharePdf(java.io.File(scan.filePath))
                                            }
                                            .padding(vertical = 6.dp),
                                    )
                                }
                            }
                        }
                    }
                    if (l.stops.isNotEmpty()) {
                        BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    stringResource(R.string.load_detail_stops),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = tc.TextPrimary,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )
                                StopTimeline(stops = l.stops)
                            }
                        }
                    }
                    BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                        DisputeSection(
                            load = l,
                            onDisputeChanged = { updated ->
                                viewModel.updateDispute(updated, saveErrorEmpty)
                            },
                            modifier = Modifier.padding(20.dp),
                        )
                    }
                    BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                stringResource(R.string.load_raw_message),
                                style = MaterialTheme.typography.labelMedium,
                                color = tc.TextLabel,
                            )
                            Text(
                                (l.rawMessage).take(500).ifEmpty { "—" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = tc.TextSecondary,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
    if (showDeleteConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.load_delete_confirm_title)) },
            text = { Text(stringResource(R.string.load_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete(deleteFailed)
                }) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun ActualFinishSection(
    load: Load,
    onPickClick: () -> Unit,
    onClearClick: () -> Unit,
) {
    val tc = LocalTruckColors.current
    val finishLabel = if (!load.actualFinishDate.isNullOrBlank()) {
        ActualFinishDate.displayLabel(load.actualFinishDate)
            ?: load.effectiveFinishDate()
            ?: "—"
    } else {
        load.effectiveFinishDate() ?: "—"
    }
    val statusText = if (!load.actualFinishDate.isNullOrBlank()) {
        stringResource(R.string.load_detail_finish_manual, finishLabel)
    } else {
        stringResource(R.string.load_detail_finish_from_stops, finishLabel)
    }
    BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(OneUiTokens.CardGap),
        ) {
            Text(
                stringResource(R.string.load_detail_finish_title),
                style = MaterialTheme.typography.titleMedium,
                color = tc.TextPrimary,
            )
            Text(
                statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = tc.TextSecondary,
            )
            Text(
                stringResource(R.string.load_detail_finish_hint),
                style = MaterialTheme.typography.labelSmall,
                color = tc.TextLabel,
            )
            OutlinedButton(
                onClick = onPickClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.load_detail_finish_pick))
            }
            if (!load.actualFinishDate.isNullOrBlank()) {
                OutlinedButton(
                    onClick = onClearClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.load_detail_finish_clear))
                }
            }
        }
    }
}

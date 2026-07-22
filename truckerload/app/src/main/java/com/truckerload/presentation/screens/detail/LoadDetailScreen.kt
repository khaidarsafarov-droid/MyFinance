package com.truckerload.presentation.screens.detail

import android.app.Application
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckerload.R
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.effectiveFinishDate
import com.truckerload.domain.model.formatDurationDays
import com.truckerload.domain.model.formatLoadRoute
import com.truckerload.domain.model.formatPacePerDay
import com.truckerload.presentation.components.DisputeSection
import com.truckerload.presentation.components.StatBox
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.components.formatRpm
import com.truckerload.presentation.components.StopTimeline
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.di.LocalPhotoRepository
import com.truckerload.presentation.di.LocalScanRepository
import com.truckerload.utils.ShareHelper
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens
import com.truckerload.utils.dateStringToUtcDatePickerMillis
import com.truckerload.utils.utcDatePickerMillisToDateString
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadDetailScreen(
    loadId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPhotoClick: (String) -> Unit = {},
) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val loadRepository = LocalLoadRepository.current
    val photoRepository = LocalPhotoRepository.current
    val scanRepository = LocalScanRepository.current
    val viewModel: LoadDetailViewModel = viewModel(
        key = "detail_$loadId",
        factory = LoadDetailViewModel.Factory(application, loadId, loadRepository),
    )
    val uiState by viewModel.uiState.collectAsState()
    val linkedPhotos by photoRepository.watchPhotosByLoadId(loadId).collectAsState(initial = emptyList())
    val linkedScans by scanRepository.watchScansByLoadId(loadId).collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    val deleteFailed = stringResource(R.string.load_delete_failed)
    val saveErrorEmpty = stringResource(R.string.common_save_error, "")
    var showDeleteConfirm by remember { mutableStateOf(false) }
    LaunchedEffect(loadId) {
        viewModel.refresh()
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
                    Text(
                        uiState.load?.tripId ?: stringResource(R.string.load_detail_title),
                        color = tc.TextPrimary,
                    )
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
                actions = {
                    IconButton(onClick = onEdit, modifier = Modifier.size(UiDimens.ToolbarTouchTarget)) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.load_detail_cd_edit),
                            tint = tc.TextPrimary,
                        )
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(UiDimens.ToolbarTouchTarget),
                    ) {
                        Icon(
                            Icons.Default.Delete,
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
                val l = uiState.load!!
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
                            value = "$${String.format("%.2f", l.totalRate)}",
                            modifier = Modifier.weight(1f),
                        )
                        StatBox(
                            title = stringResource(R.string.load_detail_stat_miles),
                            value = "${String.format("%.2f", l.totalMiles)}",
                            modifier = Modifier.weight(1f),
                        )
                        StatBox(
                            title = stringResource(R.string.load_detail_stat_rpm),
                            value = formatRpm(l.totalRate, l.totalMiles, stringResource(R.string.rpm_per_mile_format)),
                            modifier = Modifier.weight(1f),
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
                        onPickClick = { viewModel.setShowFinishPicker(true) },
                        onClearClick = {
                            viewModel.setActualFinishDate(null, saveErrorEmpty)
                        },
                    )
                    if (uiState.showFinishPicker) {
                        val initialIso = l.actualFinishDate ?: l.effectiveFinishDate()
                        val today = Calendar.getInstance(Locale.US)
                        val todayIso = "%04d-%02d-%02d".format(
                            Locale.US,
                            today.get(Calendar.YEAR),
                            today.get(Calendar.MONTH) + 1,
                            today.get(Calendar.DAY_OF_MONTH),
                        )
                        val initialMs = dateStringToUtcDatePickerMillis(initialIso ?: todayIso)
                            ?: System.currentTimeMillis()
                        val yearForRange = (initialIso ?: todayIso).take(4).toIntOrNull()
                            ?: today.get(Calendar.YEAR)
                        val dateState = rememberDatePickerState(
                            initialSelectedDateMillis = initialMs,
                            yearRange = IntRange(yearForRange - 2, yearForRange + 1),
                        )
                        DatePickerDialog(
                            onDismissRequest = { viewModel.setShowFinishPicker(false) },
                            colors = androidx.compose.material3.DatePickerDefaults.colors(
                                containerColor = tc.CardBackground,
                            ),
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        val ms = dateState.selectedDateMillis
                                        if (ms == null) {
                                            viewModel.setShowFinishPicker(false)
                                            return@TextButton
                                        }
                                        val iso = utcDatePickerMillisToDateString(ms)
                                        viewModel.setActualFinishDate(iso, saveErrorEmpty)
                                    },
                                ) { Text(stringResource(R.string.common_ok)) }
                            },
                            dismissButton = {
                                TextButton(onClick = { viewModel.setShowFinishPicker(false) }) {
                                    Text(stringResource(R.string.common_cancel))
                                }
                            },
                        ) { DatePicker(state = dateState) }
                    }
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
    val finishLabel = load.effectiveFinishDate() ?: "—"
    val statusText = if (!load.actualFinishDate.isNullOrBlank()) {
        stringResource(R.string.load_detail_finish_manual, finishLabel)
    } else {
        stringResource(R.string.load_detail_finish_from_stops, finishLabel)
    }
    BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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

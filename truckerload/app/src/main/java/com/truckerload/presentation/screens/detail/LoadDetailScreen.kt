package com.truckerload.presentation.screens.detail

import android.graphics.BitmapFactory
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.formatDurationDays
import com.truckerload.domain.model.formatLoadRoute
import com.truckerload.domain.model.formatPacePerDay
import com.truckerload.presentation.components.DisputeSection
import com.truckerload.presentation.components.StatBox
import com.truckerload.presentation.components.formatRpm
import com.truckerload.presentation.components.StopTimeline
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.di.LocalPhotoRepository
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val loadRepository = LocalLoadRepository.current
    val photoRepository = LocalPhotoRepository.current
    val linkedPhotos by photoRepository.watchPhotosByLoadId(loadId).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var load by remember(loadId) { mutableStateOf<Load?>(null) }
    var loadError by remember(loadId) { mutableStateOf<String?>(null) }
    var isLoading by remember(loadId) { mutableStateOf(true) }
    LaunchedEffect(loadId) {
        if (loadId.isBlank()) {
            loadError = context.resources.getString(R.string.load_invalid)
            isLoading = false
            return@LaunchedEffect
        }
        loadError = null
        isLoading = true
        load = try {
            withContext(Dispatchers.IO) {
                loadRepository.getLoadById(loadId)
            }
        } catch (e: Exception) {
            loadError = e.message ?: context.resources.getString(R.string.load_error_loading)
            null
        }
        if (load == null && loadError == null) loadError = context.resources.getString(R.string.load_detail_not_found)
        isLoading = false
    }
    Scaffold(
        containerColor = BentoGlassTheme.ScreenBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(load?.tripId ?: stringResource(R.string.load_detail_title), color = tc.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(UiDimens.ToolbarTouchTarget)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = tc.TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onEdit, modifier = Modifier.size(UiDimens.ToolbarTouchTarget)) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.load_detail_cd_edit), tint = tc.TextPrimary)
                    }
                    IconButton(onClick = {
                        scope.launch {
                            try {
                                loadRepository.deleteLoad(loadId)
                                onDelete()
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(
                                    e.message ?: context.getString(R.string.load_delete_failed)
                                )
                            }
                        }
                    }, modifier = Modifier.size(UiDimens.ToolbarTouchTarget)) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.load_detail_cd_delete), tint = tc.AccentExpense)
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = BentoGlassTheme.ScreenBackground,
                    titleContentColor = tc.TextPrimary
                )
            )
        }
    ) { padding ->
        when {
            loadError != null -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(loadError ?: stringResource(R.string.load_error_generic), color = tc.TextPrimary)
            }
            isLoading || load == null -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = tc.AccentPrimary)
            }
            else -> {
                val l = load!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                formatLoadRoute(l),
                                style = MaterialTheme.typography.titleMedium,
                                color = tc.TextPrimary
                            )
                            Text(
                                "${String.format("%,.2f", l.totalMiles)} mi · ${l.stopCount.takeIf { it > 0 } ?: (l.puCount + l.delCount)} stops",
                                style = MaterialTheme.typography.bodyMedium,
                                color = tc.TextSecondary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatBox(title = stringResource(R.string.load_detail_stat_total_rate), value = "$${String.format("%.2f", l.totalRate)}", modifier = Modifier.weight(1f))
                        StatBox(title = stringResource(R.string.load_detail_stat_miles), value = "${String.format("%.2f", l.totalMiles)}", modifier = Modifier.weight(1f))
                        StatBox(
                            title = stringResource(R.string.load_detail_stat_rpm),
                            value = formatRpm(l.totalRate, l.totalMiles, stringResource(R.string.rpm_per_mile_format)),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatBox(
                            title = stringResource(R.string.load_detail_stat_duration),
                            value = if (l.durationDays > 0) formatDurationDays(l.durationDays) else "—",
                            modifier = Modifier.weight(1f)
                        )
                        StatBox(
                            title = stringResource(R.string.load_detail_stat_pace),
                            value = if (l.pace > 0) formatPacePerDay(l.pace) else "—",
                            modifier = Modifier.weight(1f)
                        )
                        StatBox(title = "PU", value = "${l.puCount}", modifier = Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatBox(title = stringResource(R.string.load_detail_stat_del), value = "${l.delCount}", modifier = Modifier.weight(1f))
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
                                        val thumb = remember(photo.filePath) {
                                            java.io.File(photo.filePath).takeIf { it.exists() }?.let { file ->
                                                BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                                            }
                                        }
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
                    if (l.stops.isNotEmpty()) {
                        BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(stringResource(R.string.load_detail_stops), style = MaterialTheme.typography.titleMedium, color = tc.TextPrimary, modifier = Modifier.padding(bottom = 8.dp))
                                StopTimeline(stops = l.stops)
                            }
                        }
                    }
                    BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                        DisputeSection(
                            load = l,
                            onDisputeChanged = { updated ->
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            loadRepository.updateLoad(updated)
                                        }
                                        load = updated
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar(
                                            e.message ?: context.getString(R.string.common_save_error, "")
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.padding(20.dp),
                        )
                    }
                    BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(stringResource(R.string.load_raw_message), style = MaterialTheme.typography.labelMedium, color = tc.TextLabel)
                            Text((l.rawMessage).take(500).ifEmpty { "—" }, style = MaterialTheme.typography.bodyMedium, color = tc.TextSecondary, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

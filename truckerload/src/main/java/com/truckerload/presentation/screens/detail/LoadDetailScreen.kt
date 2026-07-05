package com.truckerload.presentation.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.model.Load
import com.truckerload.presentation.components.PenaltyItem
import com.truckerload.presentation.components.StatBox
import com.truckerload.presentation.components.formatRpm
import com.truckerload.presentation.components.StopTimeline
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.theme.LocalTruckColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadDetailScreen(
    loadId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val loadRepository = LocalLoadRepository.current
    val scope = rememberCoroutineScope()
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
        containerColor = tc.Background,
        topBar = {
            TopAppBar(
                title = { Text(load?.tripId ?: stringResource(R.string.load_detail_title), color = tc.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = tc.TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onEdit, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.load_detail_cd_edit), tint = tc.TextPrimary)
                    }
                    IconButton(onClick = {
                        scope.launch {
                            try {
                                loadRepository.deleteLoad(loadId)
                                onDelete()
                            } catch (_: Exception) { }
                        }
                    }, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.load_detail_cd_delete), tint = tc.AccentExpense)
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = tc.Background,
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
                        .padding(16.dp)
                ) {
                    Text(
                        "${l.pointA}  ━━━━━━━━━━━►  ${l.pointB}",
                        style = MaterialTheme.typography.titleMedium,
                        color = tc.TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text("${String.format("%,.2f", l.totalMiles)} mi", style = MaterialTheme.typography.bodyMedium, color = tc.TextSecondary, modifier = Modifier.padding(bottom = 16.dp))
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatBox(title = stringResource(R.string.load_detail_stat_total_rate), value = "$${String.format("%.2f", l.totalRate)}", modifier = Modifier.weight(1f))
                        StatBox(title = stringResource(R.string.load_detail_stat_miles), value = "${String.format("%.2f", l.totalMiles)}", modifier = Modifier.weight(1f))
                        StatBox(title = stringResource(R.string.load_detail_stat_rpm), value = formatRpm(l.totalRate, l.totalMiles), modifier = Modifier.weight(1f))
                    }
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatBox(title = "PU", value = "${l.puCount}", modifier = Modifier.weight(1f))
                        StatBox(title = stringResource(R.string.load_detail_stat_del), value = "${l.delCount}", modifier = Modifier.weight(1f))
                    }
                    if (l.stops.isNotEmpty()) {
                        Text(stringResource(R.string.load_detail_stops), style = MaterialTheme.typography.titleMedium, color = tc.TextPrimary, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
                        StopTimeline(stops = l.stops)
                    }
                    if (l.penalties.isNotEmpty()) {
                        Text(stringResource(R.string.load_detail_penalties), style = MaterialTheme.typography.titleMedium, color = tc.TextPrimary, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
                        l.penalties.forEach { PenaltyItem(description = it.description, amount = it.amount) }
                    }
                    Text(stringResource(R.string.load_raw_message), style = MaterialTheme.typography.labelMedium, color = tc.TextLabel, modifier = Modifier.padding(top = 24.dp))
                    Text((l.rawMessage).take(500).ifEmpty { "—" }, style = MaterialTheme.typography.bodyMedium, color = tc.TextSecondary, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

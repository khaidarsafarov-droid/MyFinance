package com.truckerload.presentation.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import com.truckerload.domain.model.Load
import com.truckerload.presentation.components.PenaltyItem
import com.truckerload.presentation.components.StatBox
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
    val loadRepository = LocalLoadRepository.current
    val scope = rememberCoroutineScope()
    var load by remember(loadId) { mutableStateOf<Load?>(null) }
    var loadError by remember(loadId) { mutableStateOf<String?>(null) }
    var isLoading by remember(loadId) { mutableStateOf(true) }
    LaunchedEffect(loadId) {
        if (loadId.isBlank()) {
            loadError = "Invalid load"
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
            loadError = e.message ?: "Error loading load"
            null
        }
        if (load == null && loadError == null) loadError = "Load not found"
        isLoading = false
    }
    Scaffold(
        containerColor = tc.Background,
        topBar = {
            TopAppBar(
                title = { Text(load?.tripId ?: "Load", color = tc.AccentInfo) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = tc.TextPrimary) }
                },
                actions = {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = tc.TextPrimary) }
                    IconButton(onClick = {
                        scope.launch {
                            try {
                                loadRepository.deleteLoad(loadId)
                                onDelete()
                            } catch (_: Exception) { }
                        }
                    }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = tc.AccentExpense) }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = tc.Background,
                    titleContentColor = tc.AccentInfo
                )
            )
        }
    ) { padding ->
        when {
            loadError != null -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(loadError ?: "Error", color = tc.TextPrimary)
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
                        StatBox(title = "Total Rate", value = "$${String.format("%.2f", l.totalRate)}", modifier = Modifier.weight(1f))
                        StatBox(title = "Miles", value = "${String.format("%.2f", l.totalMiles)}", modifier = Modifier.weight(1f))
                    }
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatBox(title = "PU", value = "${l.puCount}", modifier = Modifier.weight(1f))
                        StatBox(title = "DEL", value = "${l.delCount}", modifier = Modifier.weight(1f))
                    }
                    if (l.stops.isNotEmpty()) {
                        Text("Стопы", style = MaterialTheme.typography.titleMedium, color = tc.TextPrimary, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
                        StopTimeline(stops = l.stops)
                    }
                    if (l.penalties.isNotEmpty()) {
                        Text("Штрафы", style = MaterialTheme.typography.titleMedium, color = tc.TextPrimary, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
                        l.penalties.forEach { PenaltyItem(description = it.description, amount = it.amount) }
                    }
                    Text("Raw message", style = MaterialTheme.typography.labelMedium, color = tc.TextLabel, modifier = Modifier.padding(top = 24.dp))
                    Text((l.rawMessage).take(500).ifEmpty { "—" }, style = MaterialTheme.typography.bodyMedium, color = tc.TextSecondary, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

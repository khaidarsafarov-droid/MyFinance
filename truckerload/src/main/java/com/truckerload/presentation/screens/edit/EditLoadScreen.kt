package com.truckerload.presentation.screens.edit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.truckerload.domain.model.Load
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.utils.formatDateTimeForDisplay
import com.truckerload.utils.getWeekNumberAndYearFromDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLoadScreen(
    loadId: String,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val loadRepository = LocalLoadRepository.current
    var load by remember(loadId) { mutableStateOf<Load?>(null) }
    var loadError by remember(loadId) { mutableStateOf<String?>(null) }
    var tripId by remember { mutableStateOf("") }
    var loadDate by remember { mutableStateOf("") }
    var totalRate by remember { mutableStateOf("") }
    var totalMiles by remember { mutableStateOf("") }
    var pointA by remember { mutableStateOf("") }
    var pointB by remember { mutableStateOf("") }
    LaunchedEffect(loadId) {
        if (loadId.isBlank()) {
            loadError = "Invalid load"
            return@LaunchedEffect
        }
        loadError = null
        load = try {
            withContext(Dispatchers.IO) {
                loadRepository.getLoadById(loadId)
            }
        } catch (e: Exception) {
            loadError = e.message
            null
        }
        load?.let {
            tripId = it.tripId
            loadDate = it.date
            totalRate = it.totalRate.toString()
            totalMiles = it.totalMiles.toString()
            pointA = it.pointA
            pointB = it.pointB
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit load") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            loadError?.let { err ->
                Text(err, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 16.dp))
            }
            if (load == null && loadError == null && loadId.isNotBlank()) {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            load?.let { l ->
                Text(
                    "Добавлено: ${formatDateTimeForDisplay(l.parsedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            OutlinedTextField(value = tripId, onValueChange = { tripId = it }, label = { Text("Trip ID") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = loadDate,
                onValueChange = { loadDate = it },
                label = { Text("Дата груза (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("load_date — можно изменить") }
            )
            OutlinedTextField(value = totalRate, onValueChange = { totalRate = it }, label = { Text("Total Rate") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = totalMiles, onValueChange = { totalMiles = it }, label = { Text("Total Miles") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = pointA, onValueChange = { pointA = it }, label = { Text("Point A") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = pointB, onValueChange = { pointB = it }, label = { Text("Point B") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val l = load ?: return@Button
                    val newDate = loadDate.ifBlank { l.date }
                    val (weekNumber, year) = getWeekNumberAndYearFromDate(newDate)
                    val updated = l.copy(
                        date = newDate,
                        weekNumber = weekNumber,
                        year = year,
                        totalRate = totalRate.toDoubleOrNull() ?: l.totalRate,
                        totalMiles = totalMiles.toDoubleOrNull() ?: l.totalMiles,
                        pointA = pointA,
                        pointB = pointB,
                        updatedAt = System.currentTimeMillis()
                    )
                    CoroutineScope(Dispatchers.Main).launch {
                        loadRepository.updateLoad(updated)
                        onSaved()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save changes")
            }
        }
    }
}

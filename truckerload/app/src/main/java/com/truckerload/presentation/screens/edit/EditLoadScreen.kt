package com.truckerload.presentation.screens.edit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.truckerload.presentation.components.TlButton as Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.model.Load
import com.truckerload.presentation.components.DisputeSection
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.utils.formatDateTimeForDisplay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLoadScreen(
    loadId: String,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    onOptimisticUpdate: ((Load) -> Unit)? = null,
    onRevertOptimistic: ((String) -> Unit)? = null
) {
    val tc = LocalTruckColors.current
    val loadRepository = LocalLoadRepository.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var load by remember(loadId) { mutableStateOf<Load?>(null) }
    var loadError by remember(loadId) { mutableStateOf<String?>(null) }
    var tripId by remember { mutableStateOf("") }
    var loadDate by remember { mutableStateOf("") }
    var totalRate by remember { mutableStateOf("") }
    var totalMiles by remember { mutableStateOf("") }
    var pointA by remember { mutableStateOf("") }
    var pointB by remember { mutableStateOf("") }
    var disputeLoad by remember { mutableStateOf<Load?>(null) }
    LaunchedEffect(loadId) {
        if (loadId.isBlank()) {
            loadError = context.getString(R.string.load_invalid)
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
            disputeLoad = it
        }
    }
    Scaffold(
        containerColor = BentoGlassTheme.ScreenBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_load_title), color = tc.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = tc.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BentoGlassTheme.ScreenBackground,
                    titleContentColor = tc.TextPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            loadError?.let { err ->
                Text(err, color = tc.AccentExpense, modifier = Modifier.padding(bottom = 16.dp))
            }
            if (load == null && loadError == null && loadId.isNotBlank()) {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            load?.let { l ->
                Text(
                    stringResource(R.string.edit_load_added_at, formatDateTimeForDisplay(l.parsedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = tc.AccentPrimary,
                        unfocusedBorderColor = tc.Divider,
                        focusedLabelColor = tc.AccentPrimary,
                        unfocusedLabelColor = tc.TextSecondary,
                        focusedTextColor = tc.TextPrimary,
                        unfocusedTextColor = tc.TextPrimary,
                        focusedContainerColor = BentoGlassTheme.CardFill,
                        unfocusedContainerColor = BentoGlassTheme.CardFill,
                        focusedSupportingTextColor = tc.TextSecondary,
                        unfocusedSupportingTextColor = tc.TextSecondary,
                        cursorColor = tc.AccentPrimary
                    )
                    OutlinedTextField(value = tripId, onValueChange = { tripId = it }, label = { Text(stringResource(R.string.edit_load_trip_id)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(BentoGlassTheme.CellRadius), colors = fieldColors)
                    OutlinedTextField(
                        value = loadDate,
                        onValueChange = { loadDate = it },
                        label = { Text(stringResource(R.string.edit_load_date_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text(stringResource(R.string.edit_load_date_help)) },
                        shape = RoundedCornerShape(BentoGlassTheme.CellRadius),
                        colors = fieldColors
                    )
                    OutlinedTextField(value = totalRate, onValueChange = { totalRate = it }, label = { Text(stringResource(R.string.edit_load_total_rate)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(BentoGlassTheme.CellRadius), colors = fieldColors)
                    OutlinedTextField(value = totalMiles, onValueChange = { totalMiles = it }, label = { Text(stringResource(R.string.edit_load_total_miles)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(BentoGlassTheme.CellRadius), colors = fieldColors)
                    OutlinedTextField(value = pointA, onValueChange = { pointA = it }, label = { Text(stringResource(R.string.edit_load_point_a)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(BentoGlassTheme.CellRadius), colors = fieldColors)
                    OutlinedTextField(value = pointB, onValueChange = { pointB = it }, label = { Text(stringResource(R.string.edit_load_point_b)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(BentoGlassTheme.CellRadius), colors = fieldColors)
                }
            }
            disputeLoad?.let { dispute ->
                BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                    DisputeSection(
                        load = dispute,
                        onDisputeChanged = { updated -> disputeLoad = updated },
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val l = load ?: return@Button
                    val newDate = loadDate.ifBlank { l.date }
                    val updated = (disputeLoad ?: l).copy(
                        date = newDate,
                        totalRate = totalRate.toDoubleOrNull() ?: l.totalRate,
                        totalMiles = totalMiles.toDoubleOrNull() ?: l.totalMiles,
                        pointA = pointA,
                        pointB = pointB,
                        updatedAt = System.currentTimeMillis()
                    )
                    isSaving = true
                    saveError = null
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                loadRepository.updateLoad(updated)
                            }
                            onOptimisticUpdate?.invoke(updated)
                            onSaved()
                        } catch (e: Exception) {
                            saveError = context.getString(R.string.common_save_error, e.message.orEmpty())
                            isSaving = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !isSaving,
            ) {
                Text(
                    if (isSaving) stringResource(R.string.add_load_saving)
                    else stringResource(R.string.edit_load_save_changes)
                )
            }
            saveError?.let { err ->
                Text(err, color = tc.AccentExpense, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

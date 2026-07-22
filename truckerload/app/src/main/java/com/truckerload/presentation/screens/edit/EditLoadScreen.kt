package com.truckerload.presentation.screens.edit

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckerload.R
import com.truckerload.domain.model.Load
import com.truckerload.presentation.components.DisputeSection
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.utils.formatDateTimeForDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLoadScreen(
    loadId: String,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    onOptimisticUpdate: ((Load) -> Unit)? = null,
    onRevertOptimistic: ((String) -> Unit)? = null,
) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val loadRepository = LocalLoadRepository.current
    val viewModel: EditLoadViewModel = viewModel(
        key = "edit_$loadId",
        factory = EditLoadViewModel.Factory(application, loadId, loadRepository),
    )
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            viewModel.clearSaved()
            onSaved()
        }
    }

    Scaffold(
        containerColor = BentoGlassTheme.ScreenBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_load_title), color = tc.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            uiState.loadError?.let { err ->
                Text(err, color = tc.AccentExpense, modifier = Modifier.padding(bottom = 16.dp))
            }
            if (uiState.isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.original?.let { l ->
                Text(
                    stringResource(R.string.edit_load_added_at, formatDateTimeForDisplay(l.parsedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            if (!uiState.isLoading && uiState.original != null) {
                BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
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
                            cursorColor = tc.AccentPrimary,
                        )
                        OutlinedTextField(
                            value = uiState.tripId,
                            onValueChange = viewModel::setTripId,
                            label = { Text(stringResource(R.string.edit_load_trip_id)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(BentoGlassTheme.CellRadius),
                            colors = fieldColors,
                        )
                        OutlinedTextField(
                            value = uiState.loadDate,
                            onValueChange = viewModel::setLoadDate,
                            label = { Text(stringResource(R.string.edit_load_date_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            supportingText = { Text(stringResource(R.string.edit_load_date_help)) },
                            shape = RoundedCornerShape(BentoGlassTheme.CellRadius),
                            colors = fieldColors,
                        )
                        OutlinedTextField(
                            value = uiState.totalRate,
                            onValueChange = viewModel::setTotalRate,
                            label = { Text(stringResource(R.string.edit_load_total_rate)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(BentoGlassTheme.CellRadius),
                            colors = fieldColors,
                        )
                        OutlinedTextField(
                            value = uiState.totalMiles,
                            onValueChange = viewModel::setTotalMiles,
                            label = { Text(stringResource(R.string.edit_load_total_miles)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(BentoGlassTheme.CellRadius),
                            colors = fieldColors,
                        )
                        OutlinedTextField(
                            value = uiState.pointA,
                            onValueChange = viewModel::setPointA,
                            label = { Text(stringResource(R.string.edit_load_point_a)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(BentoGlassTheme.CellRadius),
                            colors = fieldColors,
                        )
                        OutlinedTextField(
                            value = uiState.pointB,
                            onValueChange = viewModel::setPointB,
                            label = { Text(stringResource(R.string.edit_load_point_b)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(BentoGlassTheme.CellRadius),
                            colors = fieldColors,
                        )
                    }
                }
                uiState.disputeLoad?.let { dispute ->
                    BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                        DisputeSection(
                            load = dispute,
                            onDisputeChanged = viewModel::setDisputeLoad,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        viewModel.save(
                            saveErrorFormatter = { msg ->
                                context.getString(R.string.common_save_error, msg)
                            },
                            onOptimisticUpdate = onOptimisticUpdate,
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = !uiState.isSaving,
                ) {
                    Text(
                        if (uiState.isSaving) {
                            stringResource(R.string.add_load_saving)
                        } else {
                            stringResource(R.string.edit_load_save_changes)
                        },
                    )
                }
                uiState.saveError?.let { err ->
                    Text(err, color = tc.AccentExpense, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}

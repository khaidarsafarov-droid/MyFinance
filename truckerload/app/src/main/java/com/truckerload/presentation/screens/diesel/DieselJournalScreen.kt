package com.truckerload.presentation.screens.diesel

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.presentation.components.SoftActionChip
import com.truckerload.presentation.components.SoftAppPageScaffold
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.screens.add.JournalDatePickerDialog
import com.truckerload.presentation.screens.add.JournalWeekSelectorRow
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.MoneyFormat
import com.truckerload.utils.dateStringToUtcDatePickerMillis
import com.truckerload.utils.utcDatePickerMillisToDateString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DieselJournalScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEditDiesel: (Int) -> Unit,
) {
    val tc = LocalTruckColors.current
    val viewModel: DieselJournalViewModel = hiltViewModel()
    val importViewModel: DieselImportViewModel = hiltViewModel()
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val importState by importViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            importViewModel.parseFile(uri)
        }
    }

    LaunchedEffect(importState) {
        when (val state = importState) {
            is DieselImportUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                importViewModel.dismiss()
            }
            is DieselImportUiState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                importViewModel.dismiss()
            }
            else -> Unit
        }
    }

    if (showDatePicker) {
        val initial = ui.selectedDateIso?.let { dateStringToUtcDatePickerMillis(it) }
            ?: System.currentTimeMillis()
        JournalDatePickerDialog(
            recordedAtMillis = System.currentTimeMillis(),
            initialDateMillis = initial,
            onDismiss = { showDatePicker = false },
            onConfirm = { utcMillis ->
                viewModel.selectDate(utcDatePickerMillisToDateString(utcMillis))
            },
        )
    }

    if (importState is DieselImportUiState.Review) {
        val reviewState = importState as DieselImportUiState.Review
        DieselImportReviewSheet(
            review = reviewState.review,
            isApplying = reviewState.isApplying,
            onDismiss = importViewModel::dismiss,
            onApply = importViewModel::apply,
            onEditExisting = { id ->
                importViewModel.dismiss()
                onEditDiesel(id)
            },
        )
    }

    SoftAppPageScaffold(
        title = stringResource(R.string.diesel_title),
        showBack = true,
        onBack = onBack,
        showPhoneMenu = false,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        actions = {
            SoftActionChip(
                icon = AppIcons.FileDownload,
                contentDescription = stringResource(R.string.diesel_import_action),
                onClick = {
                    fileLauncher.launch(
                        arrayOf(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            "application/vnd.ms-excel",
                            "application/octet-stream",
                        ),
                    )
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                modifier = Modifier.size(72.dp),
            ) {
                Icon(
                    imageVector = AppIcons.Add,
                    contentDescription = stringResource(R.string.add_diesel_title),
                    modifier = Modifier.size(36.dp),
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    JournalWeekSelectorRow(
                        weekNumber = ui.weekNumber,
                        weekLabel = ui.weekLabel,
                        onPreviousWeek = viewModel::selectPreviousWeek,
                        onNextWeek = viewModel::selectNextWeek,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.diesel_filter_all_days),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (ui.selectedDateIso == null) tc.AccentPrimary else tc.TextSecondary,
                            modifier = Modifier
                                .clickable(onClick = viewModel::clearDateFilter)
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                        )
                        Text(
                            text = ui.selectedDateIso?.let { isoToDisplayDate(it) }
                                ?: stringResource(R.string.diesel_filter_pick_date),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (ui.selectedDateIso != null) tc.AccentPrimary else tc.TextSecondary,
                            modifier = Modifier
                                .clickable { showDatePicker = true }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                        )
                    }
                    Text(
                        text = stringResource(
                            R.string.diesel_week_total,
                            MoneyFormat.formatCurrency(ui.weekTotal, decimals = 2),
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = tc.TextPrimary,
                    )
                }
            }
            if (importState is DieselImportUiState.Loading) {
                item {
                    Text(
                        text = stringResource(R.string.diesel_import_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tc.TextSecondary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            if (ui.entries.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.diesel_empty_week),
                        style = MaterialTheme.typography.bodyLarge,
                        color = tc.TextSecondary,
                        modifier = Modifier.padding(top = 32.dp, bottom = 16.dp),
                    )
                }
            } else {
                items(ui.entries, key = { it.id }) { fill ->
                    DieselFillCard(
                        diesel = fill,
                        onClick = { onEditDiesel(fill.id) },
                    )
                }
            }
        }
    }
}

private fun isoToDisplayDate(iso: String): String {
    if (iso.length < 10) return iso
    return "${iso.substring(8, 10)}.${iso.substring(5, 7)}.${iso.substring(0, 4)}"
}

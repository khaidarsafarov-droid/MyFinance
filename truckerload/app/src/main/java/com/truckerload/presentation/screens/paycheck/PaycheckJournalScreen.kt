package com.truckerload.presentation.screens.paycheck

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.truckerload.presentation.screens.add.JournalWeekSelectorRow
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.MoneyFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaycheckJournalScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
) {
    val tc = LocalTruckColors.current
    val viewModel: PaycheckJournalViewModel = hiltViewModel()
    val ui by viewModel.uiState.collectAsStateWithLifecycle()

    SoftAppPageScaffold(
        title = stringResource(R.string.paycheck_title),
        showBack = true,
        onBack = onBack,
        showPhoneMenu = false,
        actions = {
            SoftActionChip(
                icon = AppIcons.Add,
                contentDescription = stringResource(R.string.add_paycheck_title),
                onClick = onAdd,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
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
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.paycheck_filter_all_weeks),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (ui.showAllWeeks) tc.AccentPrimary else tc.TextSecondary,
                            modifier = Modifier
                                .clickable(onClick = viewModel::showAllWeeks)
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                        )
                    }
                    OutlinedTextField(
                        value = ui.searchQuery,
                        onValueChange = viewModel::setSearchQuery,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.paycheck_search_hint)) },
                        shape = RoundedCornerShape(14.dp),
                        colors = AppTextFieldDefaults.outlined(),
                    )
                    Text(
                        text = stringResource(
                            if (ui.showAllWeeks) R.string.paycheck_all_total else R.string.paycheck_week_total,
                            MoneyFormat.formatCurrency(ui.total, decimals = 2),
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = tc.TextPrimary,
                    )
                }
            }
            if (ui.entries.isEmpty()) {
                item {
                    Text(
                        text = stringResource(
                            when {
                                ui.searchQuery.isNotBlank() -> R.string.paycheck_empty_search
                                ui.showAllWeeks -> R.string.paycheck_empty_all
                                else -> R.string.paycheck_empty_week
                            },
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = tc.TextSecondary,
                        modifier = Modifier.padding(top = 32.dp, bottom = 16.dp),
                    )
                }
            } else {
                items(ui.entries, key = { it.id }) { paycheck ->
                    PaycheckCard(paycheck = paycheck)
                }
            }
        }
    }
}

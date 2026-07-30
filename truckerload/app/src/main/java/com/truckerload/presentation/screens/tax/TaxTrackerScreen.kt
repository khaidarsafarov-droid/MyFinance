package com.truckerload.presentation.screens.tax

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.FinanceCockpitColors
import com.truckerload.presentation.theme.LocalTruckColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxTrackerScreen(onBack: () -> Unit) {
    val tc = LocalTruckColors.current
    val viewModel: TaxTrackerViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = BentoGlassTheme.ScreenBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tax_title), color = tc.TextPrimary) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = tc.TextPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BentoGlassTheme.ScreenBackground,
                    titleContentColor = tc.TextPrimary
                )
            )
        }
    ) { padding ->
        var refreshing by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val pullRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                refreshing = true
                viewModel.refresh()
                scope.launch {
                    delay(1000)
                    refreshing = false
                }
            },
            modifier = Modifier.fillMaxSize(),
            state = pullRefreshState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.nextQuarterlyDate.isNotBlank()) {
                    BentoGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = FinanceCockpitColors.SalaryAccent.copy(alpha = 0.35f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.tax_next_quarter, uiState.nextQuarterlyDate), style = MaterialTheme.typography.titleSmall, color = tc.TextPrimary)
                            Text(stringResource(R.string.tax_in_days, uiState.daysUntilNextQuarterly), style = MaterialTheme.typography.bodySmall, color = tc.TextSecondary)
                        }
                    }
                }
                BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.tax_income), style = MaterialTheme.typography.titleSmall, color = tc.TextPrimary)
                        Text("$${String.format(Locale.US, "%,.2f", uiState.totalGrossIncome)}", style = MaterialTheme.typography.headlineMedium, color = FinanceCockpitColors.SalaryAccent)
                    }
                }
                BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.tax_deductions), style = MaterialTheme.typography.titleSmall, color = tc.TextPrimary)
                        Text(stringResource(R.string.tax_diesel_deduction, uiState.dieselDeductions), style = MaterialTheme.typography.bodyMedium, color = tc.AccentExpense)
                        Text(stringResource(R.string.tax_per_diem, uiState.perDiemDays, uiState.perDiemAmount), style = MaterialTheme.typography.bodyMedium, color = tc.AccentExpense)
                        Text(stringResource(R.string.tax_total_deductions, uiState.totalDeductions + uiState.perDiemAmount), style = MaterialTheme.typography.bodyMedium, color = tc.TextSecondary)
                    }
                }
                BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.taxable_income), style = MaterialTheme.typography.titleSmall, color = tc.TextPrimary)
                        Text("$${String.format(Locale.US, "%,.2f", uiState.taxableIncome)}", style = MaterialTheme.typography.titleMedium, color = tc.TextPrimary)
                    }
                }
                BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("SE Tax (~15.3%): $${String.format(Locale.US, "%,.2f", uiState.selfEmploymentTax)}", style = MaterialTheme.typography.bodyMedium, color = tc.TextSecondary)
                        Text("Federal Tax: $${String.format(Locale.US, "%,.2f", uiState.federalTax)}", style = MaterialTheme.typography.bodyMedium, color = tc.TextSecondary)
                        Text(stringResource(R.string.tax_total_owed, uiState.totalTaxOwed), style = MaterialTheme.typography.headlineSmall, color = tc.AccentExpense)
                    }
                }
                Text(
                    stringResource(R.string.tax_disclaimer),
                    style = MaterialTheme.typography.labelSmall,
                    color = tc.TextLabel,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

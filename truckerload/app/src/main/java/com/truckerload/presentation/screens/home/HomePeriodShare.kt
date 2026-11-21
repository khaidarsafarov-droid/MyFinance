package com.truckerload.presentation.screens.home

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.domain.filter.LoadFilter
import com.truckerload.presentation.components.HomePeriodFilterDropdown
import com.truckerload.presentation.components.TlTextButton as TextButton
import com.truckerload.presentation.utils.adaptiveHorizontalPadding
import com.truckerload.utils.PeriodReportFormat
import com.truckerload.utils.PeriodReportShare

/**
 * One share icon. The dialog offers PDF or plain text, then the system share sheet
 * sends the period already selected on Home.
 */
@Composable
internal fun HomePeriodShare(
    state: HomeUiState,
    driverName: String,
): () -> Unit {
    val viewModel: PeriodReportViewModel = hiltViewModel()
    val context = LocalContext.current
    val failedMessage = stringResource(R.string.period_report_failed)
    val ready by viewModel.shareReady.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var open by remember { mutableStateOf(false) }

    LaunchedEffect(ready) {
        val share = ready ?: return@LaunchedEffect
        runCatching { PeriodReportShare.share(context, share) }
            .onFailure {
                Toast.makeText(context, failedMessage, Toast.LENGTH_LONG).show()
            }
        viewModel.consumeShare()
    }
    LaunchedEffect(error) {
        val message = error ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        viewModel.consumeError()
    }
    if (open) {
        PeriodReportFormatDialog(
            onDismiss = { open = false },
            onPick = { format ->
                open = false
                viewModel.share(state, driverName, format)
            },
        )
    }
    return { open = true }
}

@Composable
internal fun HomePeriodShareButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            Icons.Default.Share,
            contentDescription = stringResource(R.string.period_report_share),
            tint = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
internal fun HomeRecentHeader(
    uiState: HomeUiState,
    showFilter: Boolean,
    onFilterSelected: (LoadFilter) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenMonthPicker: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.home_recent_loads),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            softWrap = true,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = adaptiveHorizontalPadding()),
        )
        if (showFilter) {
            HomePeriodFilterDropdown(
                currentFilter = uiState.filter,
                selectedYear = uiState.selectedYear,
                selectedDateLabel = uiState.selectedDateLabel,
                selectedWeekLabel = uiState.selectedWeekLabel,
                selectedMonthLabel = uiState.selectedMonthLabel,
                onFilterSelected = onFilterSelected,
                onOpenCalendar = onOpenCalendar,
                onOpenMonthPicker = onOpenMonthPicker,
            )
        }
    }
}

@Composable
private fun PeriodReportFormatDialog(
    onDismiss: () -> Unit,
    onPick: (PeriodReportFormat) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                TextButton(
                    onClick = { onPick(PeriodReportFormat.PDF) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.period_report_as_pdf))
                }
                TextButton(
                    onClick = { onPick(PeriodReportFormat.TEXT) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.period_report_as_text))
                }
            }
        }
    }
}

package com.truckerload.presentation.screens.expenses

import android.widget.Toast
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.domain.model.MiscExpense
import com.truckerload.presentation.components.SoftActionChip
import com.truckerload.presentation.components.SoftAppPageScaffold
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens
import com.truckerload.presentation.utils.MoneyFormat
import com.truckerload.utils.MiscExpenseExporter
import kotlinx.coroutines.launch

@Composable
fun MiscExpenseScreen(
    onBack: () -> Unit,
) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val viewModel: MiscExpenseViewModel = hiltViewModel()
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val editor = ui.editor
    when {
        ui.confirmDeleteId != null -> {
            MiscExpenseDeleteConfirmDialog(
                onDismiss = viewModel::dismissDeleteConfirm,
                onConfirm = viewModel::confirmDelete,
            )
        }
        editor != null -> {
            MiscExpenseEditorDialog(
                editor = editor,
                isSaving = ui.isSaving,
                onDismiss = viewModel::dismissEditor,
                onChange = viewModel::updateEditor,
                onSave = viewModel::save,
                onDelete = viewModel::requestDelete,
            )
        }
    }

    SoftAppPageScaffold(
        title = stringResource(R.string.misc_expense_title),
        showBack = true,
        onBack = onBack,
        showPhoneMenu = false,
        actions = {
            SoftActionChip(
                icon = AppIcons.Share,
                contentDescription = stringResource(R.string.misc_expense_send_title),
                onClick = {
                    if (ui.entries.isEmpty()) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.misc_expense_send_empty),
                            Toast.LENGTH_SHORT,
                        ).show()
                    } else {
                        scope.launch {
                            runCatching {
                                val file = MiscExpenseExporter.writeCsvFile(context, ui.entries)
                                MiscExpenseExporter.shareCsv(context, file)
                            }.onFailure {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.misc_expense_send_error),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::openNew,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(UiDimens.FabSize),
            ) {
                Icon(
                    AppIcons.Add,
                    contentDescription = stringResource(R.string.misc_expense_add),
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.misc_expense_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tc.TextSecondary,
                    )
                    Text(
                        text = stringResource(
                            R.string.misc_expense_total,
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
                        text = stringResource(R.string.misc_expense_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = tc.TextSecondary,
                        modifier = Modifier.padding(top = 32.dp, bottom = 16.dp),
                    )
                }
            } else {
                items(ui.entries, key = { it.id }) { expense ->
                    MiscExpenseCard(
                        expense = expense,
                        onClick = { viewModel.openExisting(expense) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MiscExpenseCard(
    expense: MiscExpense,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
    BentoGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = isoToDisplayDate(expense.date),
                    style = MaterialTheme.typography.titleSmall,
                    color = tc.TextPrimary,
                )
                Text(
                    text = MoneyFormat.formatCurrency(expense.amount, decimals = 2),
                    style = AppTypography.NumbersSmall,
                    color = tc.AccentExpense,
                )
            }
            Text(
                text = expense.description,
                style = MaterialTheme.typography.bodyMedium,
                color = tc.TextSecondary,
            )
        }
    }
}

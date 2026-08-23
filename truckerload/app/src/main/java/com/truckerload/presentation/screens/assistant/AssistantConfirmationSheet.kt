package com.truckerload.presentation.screens.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.MoneyFormat
import com.truckerload.utils.formatDateTimeForDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantConfirmationSheet(
    mutation: PendingAssistantMutation,
    isSaving: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onFix: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tc = LocalTruckColors.current
    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.assistant_confirm_title),
                style = MaterialTheme.typography.titleMedium,
                color = tc.TextPrimary,
            )
            Text(
                text = confirmationBody(mutation),
                style = MaterialTheme.typography.bodyLarge,
                color = tc.TextSecondary,
            )
            Button(
                onClick = onConfirm,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.assistant_confirm_save))
            }
            OutlinedButton(
                onClick = onFix,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.assistant_confirm_fix))
            }
            OutlinedButton(
                onClick = onCancel,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    }
}

@Composable
private fun confirmationBody(mutation: PendingAssistantMutation): String {
    return when (mutation) {
        is PendingAssistantMutation.DieselDraft -> {
            val diesel = mutation.diesel
            val amount = MoneyFormat.formatCurrency(diesel.totalAmount, decimals = 2)
            val gallons = diesel.gallons?.let { MoneyFormat.formatNumber(it, decimals = 1) }
            val whenLabel = formatDateTimeForDisplay(diesel.addedAt)
            if (gallons != null) {
                stringResource(R.string.assistant_confirm_diesel_gallons, amount, gallons, whenLabel)
            } else {
                stringResource(R.string.assistant_confirm_diesel, amount, whenLabel)
            }
        }
        is PendingAssistantMutation.PaycheckDraft -> {
            val paycheck = mutation.paycheck
            stringResource(
                R.string.assistant_confirm_paycheck,
                MoneyFormat.formatCurrency(paycheck.netAmount, decimals = 2),
                paycheck.weekLabel,
            )
        }
    }
}

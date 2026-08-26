package com.truckerload.presentation.screens.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlTextButton as TextButton
import com.truckerload.presentation.components.verticalContentScroll
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.utils.formatDateForDisplay

@Composable
fun DieselQuickAddScreen(
    onDone: () -> Unit,
    onCancel: () -> Unit,
) {
    val viewModel: AddDieselViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tc = LocalTruckColors.current

    DieselLocationPermissionEffect(onGranted = viewModel::ensureLocation)

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            viewModel.clearSaved()
            onDone()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(24.dp),
        color = tc.Background,
        tonalElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalContentScroll()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.add_diesel_quick_add_title),
                style = MaterialTheme.typography.titleLarge,
                color = tc.TextPrimary,
            )
            Text(
                text = stringResource(
                    R.string.add_diesel_quick_add_hint,
                    formatDateForDisplay(uiState.recordedAtMillis),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
            )
            DieselAmountFields(
                uiState = uiState,
                onGallonsChange = viewModel::setGallonsText,
                onPriceChange = viewModel::setPricePerGallonText,
                onDiscountChange = viewModel::setDiscountPriceText,
                onLocationChange = viewModel::setLocationText,
                onImagePicked = viewModel::scanReceipt,
                compact = false,
            )
            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = tc.AccentExpense,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.common_cancel))
                }
                Button(
                    onClick = viewModel::save,
                    modifier = Modifier
                        .weight(1.4f)
                        .height(48.dp),
                    enabled = !uiState.isSaving && !uiState.isScanning,
                ) {
                    Text(stringResource(R.string.add_diesel_confirm))
                }
            }
        }
    }
}

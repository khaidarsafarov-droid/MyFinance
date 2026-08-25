package com.truckerload.presentation.screens.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens
import com.truckerload.presentation.utils.MoneyFormat
import java.util.Locale

@Composable
fun DieselAmountFields(
    uiState: AddDieselUiState,
    onGallonsChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onDiscountChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onImagePicked: (android.net.Uri) -> Unit,
    compact: Boolean = false,
) {
    val tc = LocalTruckColors.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DieselReceiptCaptureRow(
            enabled = !uiState.isSaving,
            isScanning = uiState.isScanning,
            onImagePicked = onImagePicked,
        )
        uiState.scanMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = if (uiState.gallons == null && uiState.pricePerGallon == null) {
                    tc.TextSecondary
                } else {
                    tc.AccentProfit
                },
            )
        }
        OutlinedTextField(
            value = uiState.locationText,
            onValueChange = onLocationChange,
            label = { Text(stringResource(R.string.add_diesel_location)) },
            placeholder = {
                Text(
                    if (uiState.isResolvingLocation) {
                        stringResource(R.string.add_diesel_location_resolving)
                    } else {
                        stringResource(R.string.add_diesel_location_hint)
                    },
                )
            },
            trailingIcon = {
                if (uiState.isResolvingLocation) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = UiDimens.InputMinHeight),
            shape = RoundedCornerShape(14.dp),
            colors = AppTextFieldDefaults.outlined(),
            singleLine = true,
        )
        OutlinedTextField(
            value = uiState.gallonsText,
            onValueChange = onGallonsChange,
            label = { Text(stringResource(R.string.add_diesel_gallons)) },
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = UiDimens.InputMinHeight),
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = AppTextFieldDefaults.outlined(),
            singleLine = true,
        )
        OutlinedTextField(
            value = uiState.pricePerGallonText,
            onValueChange = onPriceChange,
            label = { Text(stringResource(R.string.add_diesel_price_per_gallon)) },
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = UiDimens.InputMinHeight),
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = AppTextFieldDefaults.outlined(),
            singleLine = true,
        )
        if (!compact) {
            OutlinedTextField(
                value = uiState.discountPriceText,
                onValueChange = onDiscountChange,
                label = { Text(stringResource(R.string.add_diesel_discount_price)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = UiDimens.InputMinHeight),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = AppTextFieldDefaults.outlined(),
                singleLine = true,
            )
        }
        uiState.paidTotal?.let { total ->
            Text(
                text = stringResource(R.string.add_diesel_paid_total, MoneyFormat.formatCurrency(total)),
                style = if (compact) MaterialTheme.typography.titleMedium else AppTypography.HeroNumber,
                color = tc.TextPrimary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        uiState.savings?.takeIf { it > 0.0 }?.let { saved ->
            Text(
                text = stringResource(R.string.add_diesel_you_saved, MoneyFormat.formatCurrency(saved)),
                style = MaterialTheme.typography.titleMedium,
                color = tc.AccentProfit,
            )
        }
        uiState.gallons?.let { gallons ->
            Text(
                text = stringResource(
                    R.string.add_diesel_gallons_preview,
                    String.format(Locale.US, "%.2f", gallons),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
            )
        }
    }
}

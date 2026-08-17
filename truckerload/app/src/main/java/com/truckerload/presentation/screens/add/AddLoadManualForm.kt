package com.truckerload.presentation.screens.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors

@Composable
fun AddLoadManualForm(
    fields: ManualLoadFields,
    onTripId: (String) -> Unit,
    onDate: (String) -> Unit,
    onRate: (String) -> Unit,
    onMiles: (String) -> Unit,
    onPointA: (String) -> Unit,
    onPointB: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
    val colors = AppTextFieldDefaults.outlined()
    BentoGlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.add_load_manual_hint),
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
            )
            OutlinedTextField(
                value = fields.tripId,
                onValueChange = onTripId,
                label = { Text(stringResource(R.string.edit_load_trip_id)) },
                placeholder = { Text(stringResource(R.string.add_load_trip_optional)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(BentoGlassTheme.CellRadius),
                colors = colors,
            )
            OutlinedTextField(
                value = fields.date,
                onValueChange = onDate,
                label = { Text(stringResource(R.string.edit_load_date_label)) },
                supportingText = { Text(stringResource(R.string.edit_load_date_help)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(BentoGlassTheme.CellRadius),
                colors = colors,
            )
            OutlinedTextField(
                value = fields.rate,
                onValueChange = onRate,
                label = { Text(stringResource(R.string.edit_load_total_rate)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(BentoGlassTheme.CellRadius),
                colors = colors,
            )
            OutlinedTextField(
                value = fields.miles,
                onValueChange = onMiles,
                label = { Text(stringResource(R.string.edit_load_total_miles)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(BentoGlassTheme.CellRadius),
                colors = colors,
            )
            OutlinedTextField(
                value = fields.pointA,
                onValueChange = onPointA,
                label = { Text(stringResource(R.string.edit_load_point_a)) },
                placeholder = { Text(stringResource(R.string.add_load_point_a_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(BentoGlassTheme.CellRadius),
                colors = colors,
            )
            OutlinedTextField(
                value = fields.pointB,
                onValueChange = onPointB,
                label = { Text(stringResource(R.string.edit_load_point_b)) },
                placeholder = { Text(stringResource(R.string.add_load_point_b_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(BentoGlassTheme.CellRadius),
                colors = colors,
            )
        }
    }
}

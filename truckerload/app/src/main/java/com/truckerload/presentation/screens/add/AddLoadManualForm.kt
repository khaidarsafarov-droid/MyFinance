package com.truckerload.presentation.screens.add

import com.truckerload.presentation.icons.AppIcons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
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
    onPointChange: (Int, String) -> Unit,
    onAddPoint: () -> Unit,
    onRemovePoint: (Int) -> Unit,
    modifier: Modifier = Modifier,
    hintRes: Int = R.string.add_load_manual_hint,
) {
    val tc = LocalTruckColors.current
    val colors = AppTextFieldDefaults.outlined()
    BentoGlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(hintRes),
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
            fields.allPoints().forEachIndexed { index, value ->
                val letter = ManualLoadFields.pointLetter(index)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { onPointChange(index, it) },
                        label = { Text(stringResource(R.string.add_load_point_n, letter)) },
                        placeholder = { Text(pointPlaceholder(index)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(BentoGlassTheme.CellRadius),
                        colors = colors,
                    )
                    if (index >= ManualLoadFields.MIN_ROUTE_POINTS) {
                        IconButton(onClick = { onRemovePoint(index) }) {
                            Icon(
                                AppIcons.Close,
                                contentDescription = stringResource(
                                    R.string.add_load_remove_point,
                                    letter,
                                ),
                                tint = tc.TextSecondary,
                            )
                        }
                    }
                }
            }
            fields.nextPointLetter()?.let { nextLetter ->
                OutlinedButton(
                    onClick = onAddPoint,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                ) {
                    Icon(AppIcons.Add, contentDescription = null)
                    Text(
                        stringResource(R.string.add_load_add_point, nextLetter),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun pointPlaceholder(index: Int): String = stringResource(
    when (index) {
        0 -> R.string.add_load_point_a_placeholder
        1 -> R.string.add_load_point_b_placeholder
        else -> R.string.add_load_point_more_placeholder
    },
)

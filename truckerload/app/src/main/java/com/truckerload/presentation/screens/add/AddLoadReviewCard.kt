package com.truckerload.presentation.screens.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.parser.LoadCompleteness
import com.truckerload.domain.parser.LoadField
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.LocalTruckColors

@Composable
fun loadFieldLabel(field: LoadField): String = stringResource(
    when (field) {
        LoadField.RATE -> R.string.add_load_field_rate
        LoadField.PICKUP -> R.string.add_load_field_pickup
        LoadField.DELIVERY -> R.string.add_load_field_delivery
        LoadField.MILES -> R.string.add_load_field_miles
        LoadField.DATE -> R.string.add_load_field_date
        LoadField.TRIP_ID -> R.string.add_load_field_trip_id
    },
)

@Composable
private fun labels(fields: List<LoadField>): String =
    fields.map { loadFieldLabel(it) }.joinToString(", ")

/** Shows what the importer could not read, so the driver knows what to complete. */
@Composable
fun AddLoadReviewCard(
    completeness: LoadCompleteness,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
    BentoGlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (completeness.isComplete) {
                Text(
                    text = stringResource(R.string.add_load_review_complete),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tc.Success,
                )
                return@Column
            }
            Text(
                text = stringResource(R.string.add_load_review_title),
                style = MaterialTheme.typography.titleSmall,
                color = tc.TextPrimary,
            )
            if (completeness.missingRequired.isNotEmpty()) {
                Text(
                    text = stringResource(
                        R.string.add_load_review_required,
                        labels(completeness.missingRequired),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.AccentExpense,
                )
            }
            if (completeness.missingOptional.isNotEmpty()) {
                Text(
                    text = stringResource(
                        R.string.add_load_review_optional,
                        labels(completeness.missingOptional),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary,
                )
            }
        }
    }
}

/** Final gate before storing a load that is missing non-blocking fields. */
@Composable
fun AddLoadConfirmIncompleteDialog(
    completeness: LoadCompleteness,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_load_confirm_incomplete_title)) },
        text = {
            Text(
                stringResource(
                    R.string.add_load_confirm_incomplete_body,
                    labels(completeness.missingOptional),
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.add_load_confirm_incomplete_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.add_load_confirm_incomplete_edit))
            }
        },
    )
}

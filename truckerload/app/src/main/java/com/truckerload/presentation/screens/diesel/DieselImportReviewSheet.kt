package com.truckerload.presentation.screens.diesel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.importing.DieselImportAction
import com.truckerload.domain.importing.DieselImportReview
import com.truckerload.domain.model.Diesel
import com.truckerload.presentation.components.TlButton
import com.truckerload.presentation.components.TlOutlinedButton
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens
import com.truckerload.presentation.utils.MoneyFormat
import com.truckerload.utils.formatDateForDisplay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DieselImportReviewSheet(
    review: DieselImportReview,
    isApplying: Boolean,
    onDismiss: () -> Unit,
    onApply: (DieselImportAction) -> Unit,
    onEditExisting: (Int) -> Unit,
) {
    val tc = LocalTruckColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { if (!isApplying) onDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (review.hasConflict) {
                    stringResource(R.string.diesel_import_conflict_title)
                } else {
                    stringResource(R.string.diesel_import_preview_title)
                },
                style = MaterialTheme.typography.titleLarge,
                color = tc.TextPrimary,
            )
            Text(
                text = stringResource(
                    R.string.diesel_import_week_subtitle,
                    review.weekRangeLabel(),
                    review.import.fills.size,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = tc.TextSecondary,
            )
            review.import.driverName?.let { driver ->
                Text(
                    text = stringResource(R.string.diesel_import_driver, driver),
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary,
                )
            }

            ComparisonCard(review = review)

            if (review.hasConflict) {
                Text(
                    text = stringResource(R.string.diesel_import_existing_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = tc.TextPrimary,
                )
                review.existing.forEach { fill ->
                    ImportFillRow(
                        diesel = fill,
                        editable = true,
                        onClick = { onEditExisting(fill.id) },
                    )
                }
            }

            Text(
                text = stringResource(R.string.diesel_import_from_file_title),
                style = MaterialTheme.typography.titleSmall,
                color = tc.TextPrimary,
            )
            review.importedPreview.forEach { fill ->
                ImportFillRow(diesel = fill, editable = false, onClick = {})
            }

            if (review.hasConflict) {
                TlOutlinedButton(
                    onClick = { onApply(DieselImportAction.ADD_FROM_FILE) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isApplying,
                ) {
                    Text(stringResource(R.string.diesel_import_add_from_file))
                }
                TlButton(
                    onClick = { onApply(DieselImportAction.REPLACE_WEEK) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isApplying,
                ) {
                    Text(stringResource(R.string.diesel_import_replace_week))
                }
            } else {
                TlButton(
                    onClick = { onApply(DieselImportAction.ADD_FROM_FILE) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isApplying,
                ) {
                    Text(stringResource(R.string.diesel_import_confirm))
                }
            }

            TlOutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isApplying,
            ) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    }
}

@Composable
private fun ComparisonCard(review: DieselImportReview) {
    val tc = LocalTruckColors.current
    val cmp = review.comparison
    BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.diesel_import_compare_title),
                style = MaterialTheme.typography.titleSmall,
                color = tc.TextPrimary,
            )
            CompareLine(
                label = stringResource(R.string.diesel_import_compare_in_app),
                count = cmp.existingCount,
                total = cmp.existingTotal,
                gallons = cmp.existingGallons,
            )
            CompareLine(
                label = stringResource(R.string.diesel_import_compare_file),
                count = cmp.importedCount,
                total = cmp.importedTotal,
                gallons = cmp.importedGallons,
            )
            if (review.hasConflict) {
                Text(
                    text = stringResource(
                        R.string.diesel_import_compare_delta,
                        MoneyFormat.formatCurrency(cmp.deltaTotal, decimals = 2),
                        String.format(Locale.US, "%.1f", cmp.deltaGallons),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun CompareLine(label: String, count: Int, total: Double, gallons: Double) {
    val tc = LocalTruckColors.current
    Text(
        text = stringResource(
            R.string.diesel_import_compare_line,
            label,
            count,
            MoneyFormat.formatCurrency(total, decimals = 2),
            String.format(Locale.US, "%.1f", gallons),
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = tc.TextSecondary,
    )
}

@Composable
private fun ImportFillRow(
    diesel: Diesel,
    editable: Boolean,
    onClick: () -> Unit,
) {
    val tc = LocalTruckColors.current
    BentoGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (editable) {
                    Modifier
                        .heightIn(min = UiDimens.TouchTarget)
                        .clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatDateForDisplay(diesel.addedAt),
                    style = MaterialTheme.typography.titleSmall,
                    color = tc.TextPrimary,
                )
                Text(
                    text = diesel.location ?: stringResource(R.string.diesel_location_unknown),
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary,
                )
                if (editable) {
                    Text(
                        text = stringResource(R.string.diesel_import_tap_to_edit),
                        style = MaterialTheme.typography.labelSmall,
                        color = tc.AccentPrimary,
                    )
                }
            }
            Text(
                text = MoneyFormat.formatCurrency(diesel.totalAmount, decimals = 2),
                style = MaterialTheme.typography.titleSmall,
                color = tc.TextPrimary,
            )
        }
    }
}

private fun DieselImportReview.weekRangeLabel(): String =
    "${import.weekStartDate} – ${import.weekEndDate}"

package com.truckerload.presentation.screens.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.model.ScanDocumentCategory
import com.truckerload.presentation.theme.AppFilterChipDefaults

fun ScanDocumentCategory.labelRes(): Int = when (this) {
    ScanDocumentCategory.LOAD -> R.string.scan_category_load
    ScanDocumentCategory.PAYCHECK -> R.string.scan_category_paycheck
    ScanDocumentCategory.DIESEL -> R.string.scan_category_diesel
    ScanDocumentCategory.TRUCK -> R.string.scan_category_truck
    ScanDocumentCategory.OTHER -> R.string.scan_category_other
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScanCategoryFilterChips(
    selected: ScanDocumentCategory?,
    counts: Map<ScanDocumentCategory, Int>,
    totalCount: Int,
    onSelect: (ScanDocumentCategory?) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.scan_category_all, totalCount)) },
            colors = AppFilterChipDefaults.colors(),
        )
        ScanDocumentCategory.entries.forEach { category ->
            val count = counts[category] ?: 0
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(category) },
                label = { Text("${stringResource(category.labelRes())} $count") },
                colors = AppFilterChipDefaults.colors(),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScanCategoryPickerChips(
    selected: ScanDocumentCategory,
    onSelect: (ScanDocumentCategory) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ScanDocumentCategory.entries.forEach { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(category) },
                enabled = enabled,
                label = { Text(stringResource(category.labelRes())) },
                colors = AppFilterChipDefaults.colors(),
            )
        }
    }
}

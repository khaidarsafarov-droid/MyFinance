package com.truckerload.presentation.screens.tax

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.tax.AccountantExportSection
import com.truckerload.presentation.components.TlButton
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxSendDataSheet(
    year: Int,
    exporting: Boolean,
    onDismiss: () -> Unit,
    onSend: (Set<AccountantExportSection>) -> Unit,
) {
    val tc = LocalTruckColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var allTogether by remember { mutableStateOf(true) }
    var loads by remember { mutableStateOf(true) }
    var diesel by remember { mutableStateOf(true) }
    var perDiem by remember { mutableStateOf(true) }
    var maintenance by remember { mutableStateOf(true) }

    fun syncFromAll(checked: Boolean) {
        allTogether = checked
        if (checked) {
            loads = true
            diesel = true
            perDiem = true
            maintenance = true
        }
    }

    fun syncIndividual() {
        allTogether = loads && diesel && perDiem && maintenance
    }

    val selection: Set<AccountantExportSection> = when {
        allTogether -> setOf(AccountantExportSection.ALL)
        else -> buildSet {
            if (loads) add(AccountantExportSection.LOADS)
            if (diesel) add(AccountantExportSection.DIESEL)
            if (perDiem) add(AccountantExportSection.PER_DIEM)
            if (maintenance) add(AccountantExportSection.MAINTENANCE)
        }
    }
    val canSend = allTogether || selection.isNotEmpty()

    ModalBottomSheet(
        onDismissRequest = { if (!exporting) onDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.tax_send_title),
                style = MaterialTheme.typography.titleLarge,
                color = tc.TextPrimary,
            )
            Text(
                text = stringResource(R.string.tax_send_subtitle, year),
                style = MaterialTheme.typography.bodyMedium,
                color = tc.TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Text(
                text = stringResource(R.string.tax_send_what_label),
                style = MaterialTheme.typography.titleSmall,
                color = tc.TextPrimary,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
            )

            SectionCheckRow(
                label = stringResource(R.string.tax_send_section_all),
                checked = allTogether,
                enabled = !exporting,
                onCheckedChange = { syncFromAll(it) },
            )
            SectionCheckRow(
                label = stringResource(R.string.tax_send_section_loads),
                checked = loads,
                enabled = !exporting && !allTogether,
                onCheckedChange = {
                    loads = it
                    syncIndividual()
                },
            )
            SectionCheckRow(
                label = stringResource(R.string.tax_send_section_diesel),
                checked = diesel,
                enabled = !exporting && !allTogether,
                onCheckedChange = {
                    diesel = it
                    syncIndividual()
                },
            )
            SectionCheckRow(
                label = stringResource(R.string.tax_send_section_per_diem),
                checked = perDiem,
                enabled = !exporting && !allTogether,
                onCheckedChange = {
                    perDiem = it
                    syncIndividual()
                },
            )
            SectionCheckRow(
                label = stringResource(R.string.tax_send_section_maintenance),
                checked = maintenance,
                enabled = !exporting && !allTogether,
                onCheckedChange = {
                    maintenance = it
                    syncIndividual()
                },
            )

            Text(
                text = stringResource(R.string.tax_send_apps_hint),
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
            )

            TlButton(
                onClick = { onSend(selection) },
                modifier = Modifier.fillMaxWidth(),
                enabled = canSend && !exporting,
            ) {
                Text(
                    if (exporting) {
                        stringResource(R.string.tax_send_preparing)
                    } else {
                        stringResource(R.string.tax_send_action)
                    },
                )
            }
        }
    }
}

@Composable
private fun SectionCheckRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val tc = LocalTruckColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = UiDimens.TouchTarget)
            .clickable(
                enabled = enabled,
                role = Role.Checkbox,
                onClick = { onCheckedChange(!checked) },
            )
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) tc.TextPrimary else tc.TextLabel,
            modifier = Modifier.weight(1f),
        )
    }
}

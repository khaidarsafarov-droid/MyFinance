package com.truckerload.presentation.screens.add

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.model.EquipmentType
import com.truckerload.presentation.theme.AppFilterChipDefaults
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.LocalTruckColors

@Composable
fun EquipmentTypeChipRow(
    selected: EquipmentType?,
    onSelect: (EquipmentType?) -> Unit,
    modifier: Modifier = Modifier,
    includeAllChip: Boolean = false,
    allowClear: Boolean = true,
    title: String? = stringResource(R.string.equipment_type_label),
) {
    val tc = LocalTruckColors.current
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (title != null) {
            Text(title, style = AppTypography.Subtitle, color = tc.TextSecondary)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (includeAllChip) {
                FilterChip(
                    selected = selected == null,
                    onClick = { onSelect(null) },
                    label = { Text(stringResource(R.string.equipment_filter_all)) },
                    colors = AppFilterChipDefaults.colors(),
                )
            }
            EquipmentType.entries.forEach { type ->
                val isOn = selected == type
                FilterChip(
                    selected = isOn,
                    onClick = {
                        onSelect(if (isOn && allowClear && !includeAllChip) null else type)
                    },
                    label = { Text("${type.emoji()} ${stringResource(type.labelRes())}") },
                    colors = AppFilterChipDefaults.colors(),
                )
            }
        }
    }
}

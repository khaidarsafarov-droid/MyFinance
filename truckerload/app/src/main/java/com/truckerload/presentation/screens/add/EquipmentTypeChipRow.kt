package com.truckerload.presentation.screens.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.model.EquipmentType
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.AppChipLabel
import com.truckerload.presentation.theme.AppFilterChip
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.LocalBrandPalette
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
    val types = EquipmentType.entries
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (title != null) {
            Text(title, style = AppTypography.Subtitle, color = tc.TextSecondary)
        }
        // The more-details card pads 12.dp and clips to its rounded shape.
        // A horizontal row with no content inset was sliding under that clip,
        // so the first chip (Power Only's tail in the screenshot) was cut off.
        // Pull the scroller out to the clip and put the same 12.dp back as
        // contentPadding. Chips do not take focus: a focused selection scrolls
        // itself into view and slices the chip before it.
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = (-12).dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (includeAllChip) {
                item(key = "all") {
                    AppFilterChip(
                        selected = selected == null,
                        onClick = { onSelect(null) },
                        modifier = Modifier.focusProperties { canFocus = false },
                        label = { AppChipLabel(stringResource(R.string.equipment_filter_all)) },
                        leadingIcon = { EquipmentGlyph(AppIcons.LocalShipping, LocalBrandPalette.current.iconViolet) },
                    )
                }
            }
            items(types, key = { it.name }) { type ->
                val isOn = selected == type
                AppFilterChip(
                    selected = isOn,
                    onClick = {
                        onSelect(if (isOn && allowClear && !includeAllChip) null else type)
                    },
                    modifier = Modifier.focusProperties { canFocus = false },
                    label = { AppChipLabel(stringResource(type.labelRes())) },
                    leadingIcon = { EquipmentGlyph(type) },
                )
            }
        }
    }
}

@Composable
private fun EquipmentGlyph(type: EquipmentType) {
    val brand = LocalBrandPalette.current
    val (icon, tint) = when (type) {
        EquipmentType.REEFER -> AppIcons.AcUnit to brand.iconViolet
        EquipmentType.FLATBED, EquipmentType.CARGO_VAN -> AppIcons.LocalShipping to brand.iconGreen
        EquipmentType.POWER_ONLY, EquipmentType.CAR_HAULER -> AppIcons.LocalShipping to brand.orange
        EquipmentType.DRY_VAN, EquipmentType.BOX_TRUCK -> AppIcons.LocalShipping to brand.iconBlue
        EquipmentType.AMAZON_RELAY, EquipmentType.OTHER -> AppIcons.LocalShipping to brand.iconViolet
    }
    EquipmentGlyph(icon, tint)
}

@Composable
private fun EquipmentGlyph(icon: ImageVector, tint: Color) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(18.dp),
    )
}

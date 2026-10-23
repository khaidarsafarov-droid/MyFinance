package com.truckerload.presentation.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.units.DisplayUnits
import com.truckerload.domain.units.DistanceUnit
import com.truckerload.presentation.components.SoftAppPageScaffold
import com.truckerload.presentation.di.LocalSettingsDataStore
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.adaptiveHorizontalPadding
import com.truckerload.presentation.utils.useNavigationRail
import com.truckerload.widget.WidgetRefresh
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistanceUnitScreen(
    onBack: () -> Unit,
    showBack: Boolean = true,
) {
    val store = LocalSettingsDataStore.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tabletChrome = useNavigationRail()
    var selected by remember { mutableStateOf(DisplayUnits.current) }
    DistanceUnitContent(
        selected = selected,
        showBack = showBack && !tabletChrome,
        onBack = onBack,
        onSelect = { unit ->
            scope.launch {
                val explicit = store.getExplicitDistanceUnitOnce()
                if (explicit == unit) {
                    selected = unit
                    return@launch
                }
                store.saveDistanceUnit(unit)
                DisplayUnits.install(unit)
                selected = unit
                WidgetRefresh.refreshAndUpdateAsync(context)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DistanceUnitContent(
    selected: DistanceUnit,
    onSelect: (DistanceUnit) -> Unit,
    onBack: () -> Unit,
    showBack: Boolean,
    modifier: Modifier = Modifier,
) {
    val miles = stringResource(R.string.settings_units_miles)
    val kilometers = stringResource(R.string.settings_units_kilometers)
    val options = listOf(DistanceUnit.MILES to miles, DistanceUnit.KILOMETERS to kilometers)
    SoftAppPageScaffold(
        title = stringResource(R.string.settings_units_title),
        showBack = showBack,
        onBack = onBack,
        showPhoneMenu = false,
        modifier = modifier,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            contentPadding = PaddingValues(
                horizontal = adaptiveHorizontalPadding(),
                vertical = 8.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(contentType = "header") {
                Text(
                    text = stringResource(R.string.settings_units_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalTruckColors.current.TextSecondary,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            items(options, key = { it.first.storageTag }, contentType = { "unit" }) { (unit, label) ->
                UnitOptionCard(
                    label = label,
                    selected = unit == selected,
                    onClick = { onSelect(unit) },
                )
            }
        }
    }
}

@Composable
private fun UnitOptionCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tc = LocalTruckColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) tc.AccentPrimary.copy(alpha = 0.16f) else tc.SurfaceSecondary,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = label,
                color = tc.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = tc.AccentPrimary),
            )
        }
    }
}

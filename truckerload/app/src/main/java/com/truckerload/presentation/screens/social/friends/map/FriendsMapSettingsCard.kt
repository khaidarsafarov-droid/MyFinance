package com.truckerload.presentation.screens.social.friends.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.friends.FriendsRouteDisplayMode
import com.truckerload.presentation.theme.AppSwitchDefaults
import com.truckerload.presentation.theme.LocalTruckColors

@Composable
fun FriendsMapSettingsCard(
    sharePathEnabled: Boolean,
    routeVehicleTruck: Boolean,
    routeDisplayMode: FriendsRouteDisplayMode,
    locationBatterySaver: Boolean,
    hasLocationPermission: Boolean,
    onSharePathEnabled: (Boolean) -> Unit,
    onNeedLocationPermission: () -> Unit,
    onRouteVehicleTruck: (Boolean) -> Unit,
    onRouteDisplayMode: (FriendsRouteDisplayMode) -> Unit,
    onLocationBatterySaver: (Boolean) -> Unit,
) {
    val tc = LocalTruckColors.current
    var expanded by remember { mutableStateOf(false) }
    val summaryParts = buildList {
        if (sharePathEnabled) add(stringResource(R.string.friends_settings_hint_share))
        if (locationBatterySaver) add(stringResource(R.string.friends_settings_hint_battery))
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { expanded = !expanded }
                        .padding(vertical = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.friends_settings_section),
                        style = MaterialTheme.typography.titleSmall,
                        color = tc.TextPrimary,
                    )
                    if (!expanded && summaryParts.isNotEmpty()) {
                        Text(
                            text = summaryParts.joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = tc.TextSecondary,
                        )
                    }
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = stringResource(R.string.friends_settings_section),
                        tint = tc.TextSecondary,
                    )
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(end = 10.dp, bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.friends_share_path_toggle),
                            style = MaterialTheme.typography.titleSmall,
                            color = tc.TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = sharePathEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && !hasLocationPermission) {
                                    onNeedLocationPermission()
                                }
                                onSharePathEnabled(enabled)
                            },
                            colors = AppSwitchDefaults.colors(),
                        )
                    }
                    if (!hasLocationPermission) {
                        Text(
                            text = stringResource(R.string.friends_need_location_permission),
                            style = MaterialTheme.typography.bodySmall,
                            color = tc.AccentPrimary,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.friends_route_truck_toggle),
                            style = MaterialTheme.typography.bodySmall,
                            color = tc.TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = routeVehicleTruck,
                            onCheckedChange = onRouteVehicleTruck,
                            colors = AppSwitchDefaults.colors(),
                        )
                    }
                    FriendsRouteModeSelector(
                        selected = routeDisplayMode,
                        onSelect = onRouteDisplayMode,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.friends_battery_saver_toggle),
                            style = MaterialTheme.typography.bodySmall,
                            color = tc.TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = locationBatterySaver,
                            onCheckedChange = onLocationBatterySaver,
                            colors = AppSwitchDefaults.colors(),
                        )
                    }
                }
            }
        }
    }
}

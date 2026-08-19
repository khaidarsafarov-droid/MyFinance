package com.truckerload.presentation.screens.social.friends.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.friends.FriendsRouteDisplayMode
import com.truckerload.presentation.theme.AppFilterChipDefaults
import com.truckerload.presentation.theme.LocalTruckColors

@Composable
fun FriendsRouteModeSelector(
    selected: FriendsRouteDisplayMode,
    onSelect: (FriendsRouteDisplayMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.friends_route_mode_title),
            style = MaterialTheme.typography.bodySmall,
            color = tc.TextPrimary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selected == FriendsRouteDisplayMode.TRAVELED,
                onClick = { onSelect(FriendsRouteDisplayMode.TRAVELED) },
                label = { Text(stringResource(R.string.friends_route_mode_traveled)) },
                colors = AppFilterChipDefaults.colors(),
            )
            FilterChip(
                selected = selected == FriendsRouteDisplayMode.REMAINING,
                onClick = { onSelect(FriendsRouteDisplayMode.REMAINING) },
                label = { Text(stringResource(R.string.friends_route_mode_remaining)) },
                colors = AppFilterChipDefaults.colors(),
            )
        }
    }
}

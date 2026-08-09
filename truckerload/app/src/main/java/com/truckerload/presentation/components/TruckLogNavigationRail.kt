package com.truckerload.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.navigation.Routes
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens

private data class RailDestination(
    val route: String,
    val icon: ImageVector,
    val labelRes: Int,
)

private val tabletDestinations = listOf(
    RailDestination(Routes.HOME, Icons.AutoMirrored.Outlined.Assignment, R.string.nav_logbook),
    RailDestination(Routes.STATS, Icons.Outlined.Flag, R.string.nav_weekly_goal),
    RailDestination(Routes.COMMUNITY, Icons.Outlined.Groups, R.string.nav_community),
    RailDestination(Routes.PROFILE, Icons.Outlined.Person, R.string.nav_profile),
)

/**
 * Tablet landscape start sidebar (permanent drawer style).
 * Wider than Material NavigationRail so landscape tablets use the full width
 * instead of a skinny centered icon column.
 */
@Composable
fun TruckLogNavigationRail(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onDrawerNavigate: (DrawerDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
    val itemColors = NavigationDrawerItemDefaults.colors(
        selectedContainerColor = tc.AccentPrimary.copy(alpha = 0.12f),
        selectedIconColor = tc.AccentPrimary,
        selectedTextColor = tc.AccentPrimary,
        unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
        unselectedIconColor = tc.TextSecondary,
        unselectedTextColor = tc.TextSecondary,
    )

    Column(
        modifier = modifier
            .width(UiDimens.TabletNavSidebarWidth)
            .fillMaxHeight()
            .background(BentoGlassTheme.CardFill)
            .padding(horizontal = 12.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.home_brand_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = tc.AccentPrimary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = BentoGlassTheme.CardBorderMuted, thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            tabletDestinations.forEach { dest ->
                val selected = isRailDestinationSelected(currentRoute, dest.route)
                NavigationDrawerItem(
                    selected = selected,
                    onClick = { onNavigate(dest.route) },
                    icon = {
                        Icon(
                            dest.icon,
                            contentDescription = stringResource(dest.labelRes),
                            modifier = Modifier.size(UiDimens.IconNav),
                        )
                    },
                    label = { Text(stringResource(dest.labelRes)) },
                    colors = itemColors,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = BentoGlassTheme.CardBorderMuted, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            NavigationDrawerItem(
                selected = false,
                onClick = { onDrawerNavigate(DrawerDestination.SETTINGS) },
                icon = {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.nav_settings),
                        modifier = Modifier.size(UiDimens.IconNav),
                    )
                },
                label = { Text(stringResource(R.string.nav_settings)) },
                colors = itemColors,
                modifier = Modifier.fillMaxWidth(),
            )
            NavigationDrawerItem(
                selected = currentRoute == Routes.ANALYTICS ||
                    currentRoute == Routes.ADVANCED_STATS ||
                    currentRoute == Routes.MAP,
                onClick = { onDrawerNavigate(DrawerDestination.REPORTS) },
                icon = {
                    Icon(
                        Icons.Outlined.BarChart,
                        contentDescription = stringResource(R.string.drawer_reports),
                        modifier = Modifier.size(UiDimens.IconNav),
                    )
                },
                label = { Text(stringResource(R.string.drawer_reports)) },
                colors = itemColors,
                modifier = Modifier.fillMaxWidth(),
            )
            NavigationDrawerItem(
                selected = currentRoute == Routes.SCAN_GALLERY || currentRoute == Routes.SCANNER,
                onClick = { onDrawerNavigate(DrawerDestination.DOCUMENTS) },
                icon = {
                    Icon(
                        Icons.Outlined.Description,
                        contentDescription = stringResource(R.string.drawer_documents),
                        modifier = Modifier.size(UiDimens.IconNav),
                    )
                },
                label = { Text(stringResource(R.string.drawer_documents)) },
                colors = itemColors,
                modifier = Modifier.fillMaxWidth(),
            )
            NavigationDrawerItem(
                selected = currentRoute == Routes.CAMERA,
                onClick = { onNavigate(Routes.CAMERA) },
                icon = {
                    Icon(
                        Icons.Outlined.CameraAlt,
                        contentDescription = stringResource(R.string.camera),
                        modifier = Modifier.size(UiDimens.IconNav),
                    )
                },
                label = { Text(stringResource(R.string.camera)) },
                colors = itemColors,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun isRailDestinationSelected(currentRoute: String?, targetRoute: String): Boolean {
    if (currentRoute == null) return false
    return when (targetRoute) {
        Routes.HOME -> currentRoute == Routes.HOME ||
            currentRoute.startsWith("load_detail") ||
            currentRoute.startsWith("edit_load") ||
            currentRoute == Routes.ADD_LOAD
        Routes.STATS -> currentRoute == Routes.STATS
        Routes.COMMUNITY -> currentRoute == Routes.COMMUNITY ||
            currentRoute.startsWith("social_chat") ||
            currentRoute.startsWith("voice_room") ||
            currentRoute == Routes.VOICE_ROOMS ||
            currentRoute.startsWith("call") ||
            currentRoute == Routes.STATUS ||
            currentRoute == Routes.GROUPS ||
            currentRoute.startsWith("group_detail")
        Routes.PROFILE -> currentRoute == Routes.PROFILE ||
            currentRoute == Routes.PROFILE_EDIT ||
            currentRoute.startsWith("profile_peer")
        Routes.ANALYTICS -> currentRoute == Routes.ANALYTICS ||
            currentRoute == Routes.ADVANCED_STATS ||
            currentRoute == Routes.MAP
        Routes.SETTINGS -> currentRoute == Routes.SETTINGS ||
            currentRoute == Routes.FINANCIAL_ADVISOR
        else -> currentRoute == targetRoute
    }
}

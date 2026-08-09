package com.truckerload.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.navigation.Routes
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens

/** Fixed sidebar width so the rail stays a left chrome strip, not a centered phone column. */
private val RailWidth = 96.dp

private data class RailDestination(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val labelRes: Int,
)

private val tabletDestinations = listOf(
    RailDestination(Routes.HOME, Icons.AutoMirrored.Outlined.Assignment, R.string.nav_logbook),
    RailDestination(Routes.STATS, Icons.Outlined.Flag, R.string.nav_weekly_goal),
    RailDestination(Routes.COMMUNITY, Icons.Outlined.Groups, R.string.nav_community),
    RailDestination(Routes.PROFILE, Icons.Outlined.Person, R.string.nav_profile),
)

@Composable
fun TruckLogNavigationRail(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onDrawerNavigate: (DrawerDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current

    NavigationRail(
        modifier = modifier
            .width(RailWidth)
            .fillMaxHeight(),
        containerColor = BentoGlassTheme.CardFill,
        contentColor = tc.TextPrimary,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.home_brand_title),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = tc.AccentPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Spacer(modifier.height(16.dp))
            HorizontalDivider(color = BentoGlassTheme.CardBorderMuted, thickness = 0.5.dp)
            Spacer(modifier.height(8.dp))
        }

        tabletDestinations.forEach { dest ->
            val selected = isRailDestinationSelected(currentRoute, dest.route)
            NavigationRailItem(
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
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = tc.AccentPrimary,
                    selectedTextColor = tc.AccentPrimary,
                    indicatorColor = tc.AccentPrimary.copy(alpha = 0.12f),
                    unselectedIconColor = tc.TextSecondary,
                    unselectedTextColor = tc.TextSecondary,
                ),
            )
        }

        Spacer(modifier.weight(1f))
        HorizontalDivider(color = BentoGlassTheme.CardBorderMuted, thickness = 0.5.dp)

        NavigationRailItem(
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
            colors = NavigationRailItemDefaults.colors(
                unselectedIconColor = tc.TextSecondary,
                unselectedTextColor = tc.TextSecondary,
            ),
        )
        NavigationRailItem(
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
            colors = NavigationRailItemDefaults.colors(
                unselectedIconColor = tc.TextSecondary,
                unselectedTextColor = tc.TextSecondary,
            ),
        )
        NavigationRailItem(
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
            colors = NavigationRailItemDefaults.colors(
                unselectedIconColor = tc.TextSecondary,
                unselectedTextColor = tc.TextSecondary,
            ),
        )
        NavigationRailItem(
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
            colors = NavigationRailItemDefaults.colors(
                unselectedIconColor = tc.TextSecondary,
                unselectedTextColor = tc.TextSecondary,
            ),
        )
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

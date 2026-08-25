package com.truckerload.presentation.components

import com.truckerload.presentation.icons.AppIcons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.truckerload.R
import com.truckerload.data.preferences.ProfileIdentity
import com.truckerload.presentation.di.LocalProfileRepository
import com.truckerload.presentation.di.LocalUserProfileStore
import com.truckerload.presentation.navigation.Routes
import com.truckerload.presentation.theme.SoftUiColors
import com.truckerload.presentation.theme.SoftUiShapes
import com.truckerload.presentation.theme.UiDimens

private data class SidebarDestination(
    val route: String?,
    val drawer: DrawerDestination?,
    val icon: ImageVector,
    val labelRes: Int,
)

private val primaryDestinations = listOf(
    SidebarDestination(Routes.HOME, null, AppIcons.Assignment, R.string.nav_logbook),
    SidebarDestination(Routes.STATS, null, AppIcons.Flag, R.string.nav_weekly_goal),
    SidebarDestination(Routes.PROFILE, null, AppIcons.Person, R.string.nav_profile),
)

private val toolDestinations = listOf(
    SidebarDestination(null, DrawerDestination.SETTINGS, AppIcons.Settings, R.string.nav_settings),
    SidebarDestination(null, DrawerDestination.REPORTS, AppIcons.BarChart, R.string.drawer_reports),
    SidebarDestination(null, DrawerDestination.DOCUMENTS, AppIcons.Description, R.string.drawer_documents),
    SidebarDestination(Routes.CAMERA, null, AppIcons.CameraAlt, R.string.camera),
)

/**
 * Tablet navigation: compact icon rail on 7–10″ portrait, wide labeled sidebar
 * on large landscape windows.
 */
@Composable
fun TruckLogNavigationRail(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onDrawerNavigate: (DrawerDestination) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val socialProfile by LocalProfileRepository.current.watchMyEnhancedProfile()
        .collectAsStateWithLifecycle(initialValue = null)
    val userProfile by LocalUserProfileStore.current.profile.collectAsStateWithLifecycle()
    val defaultDriverName = stringResource(R.string.default_driver_name)
    val displayName = remember(socialProfile, userProfile, defaultDriverName) {
        socialProfile?.displayName
            ?.takeIf { it.isNotBlank() && it !in setOf(defaultDriverName, "Driver", "User") }
            ?: userProfile?.displayName
                ?.takeIf { it.isNotBlank() && it != userProfile?.email }
            ?: defaultDriverName
    }
    val photoUrl = ProfileIdentity.displayPhotoUrl(
        roomAvatar = socialProfile?.avatarUrl,
        providerPhotoUrl = userProfile?.photoUrl,
        customPhoto = userProfile?.customPhoto == true,
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(if (compact) UiDimens.CompactRailWidth else UiDimens.WideSidebarWidth)
            .background(SoftUiColors.ForestPrimary)
            .padding(
                horizontal = if (compact) 8.dp else 16.dp,
                vertical = if (compact) 12.dp else 20.dp,
            ),
    ) {
        if (compact) {
            CompactTabletRailContent(
                currentRoute = currentRoute,
                onNavigate = onNavigate,
                onDrawerNavigate = onDrawerNavigate,
            )
            return@Column
        }
        Text(
            text = stringResource(R.string.home_brand_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
        )
        Spacer(Modifier.height(20.dp))
        SidebarAvatar(photoUrl = photoUrl, name = displayName)
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.tablet_sidebar_greeting, displayName),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            primaryDestinations.forEach { dest ->
                val selected = dest.route != null && isRailDestinationSelected(currentRoute, dest.route)
                SoftSidebarItem(
                    selected = selected,
                    icon = dest.icon,
                    label = stringResource(dest.labelRes),
                    onClick = {
                        dest.route?.let(onNavigate)
                    },
                )
            }
            Spacer(Modifier.height(12.dp))
            toolDestinations.forEach { dest ->
                val selected = when {
                    dest.route != null -> isRailDestinationSelected(currentRoute, dest.route)
                    dest.drawer == DrawerDestination.SETTINGS ->
                        currentRoute == Routes.SETTINGS ||
                            currentRoute == Routes.FINANCIAL_ADVISOR
                    dest.drawer == DrawerDestination.REPORTS ->
                        currentRoute == Routes.ANALYTICS || currentRoute == Routes.MAP
                    dest.drawer == DrawerDestination.DOCUMENTS ->
                        currentRoute == Routes.SCAN_GALLERY || currentRoute == Routes.SCANNER
                    else -> false
                }
                SoftSidebarItem(
                    selected = selected,
                    icon = dest.icon,
                    label = stringResource(dest.labelRes),
                    onClick = {
                        when {
                            dest.route != null -> onNavigate(dest.route)
                            dest.drawer != null -> onDrawerNavigate(dest.drawer)
                        }
                    },
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        BackupSidebarCard(onOpenSettings = { onDrawerNavigate(DrawerDestination.SETTINGS) })
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.CompactTabletRailContent(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onDrawerNavigate: (DrawerDestination) -> Unit,
) {
    Text(
        text = stringResource(R.string.home_brand_title),
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
    )
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        (primaryDestinations + toolDestinations).forEach { dest ->
            val selected = when {
                dest.route != null -> isRailDestinationSelected(currentRoute, dest.route)
                dest.drawer == DrawerDestination.SETTINGS ->
                    currentRoute == Routes.SETTINGS || currentRoute == Routes.FINANCIAL_ADVISOR
                dest.drawer == DrawerDestination.REPORTS ->
                    currentRoute == Routes.ANALYTICS || currentRoute == Routes.MAP
                dest.drawer == DrawerDestination.DOCUMENTS ->
                    currentRoute == Routes.SCAN_GALLERY || currentRoute == Routes.SCANNER
                else -> false
            }
            CompactRailItem(
                selected = selected,
                icon = dest.icon,
                label = stringResource(dest.labelRes),
                onClick = {
                    when {
                        dest.route != null -> onNavigate(dest.route)
                        dest.drawer != null -> onDrawerNavigate(dest.drawer)
                    }
                },
            )
        }
    }
}

@Composable
private fun CompactRailItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val bg = if (selected) SoftUiColors.Sage else Color.Transparent
    val fg = if (selected) SoftUiColors.ForestPrimary else Color.White.copy(alpha = 0.92f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .heightIn(min = UiDimens.TouchTarget)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = fg,
            modifier = Modifier.size(UiDimens.IconNav),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SidebarAvatar(photoUrl: String?, name: String) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(SoftUiColors.ForestAccent),
        contentAlignment = Alignment.Center,
    ) {
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "T",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
        }
    }
}

@Composable
private fun SoftSidebarItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val bg = if (selected) SoftUiColors.Sage else Color.Transparent
    val fg = if (selected) SoftUiColors.ForestPrimary else Color.White.copy(alpha = 0.88f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = fg,
            modifier = Modifier.size(UiDimens.IconNav),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BackupSidebarCard(onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SoftUiShapes.Card)
            .background(SoftUiColors.ForestAccent.copy(alpha = 0.35f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                AppIcons.CloudUpload,
                contentDescription = null,
                tint = SoftUiColors.Sage,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = stringResource(R.string.tablet_sidebar_backup_title),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
        }
        Text(
            text = stringResource(R.string.tablet_sidebar_backup_body),
            style = MaterialTheme.typography.bodySmall,
            color = SoftUiColors.Sage,
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onOpenSettings,
                )
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.tablet_sidebar_backup_cta),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = SoftUiColors.ForestPrimary,
            )
        }
    }
}

internal fun isRailDestinationSelected(currentRoute: String?, targetRoute: String): Boolean {
    if (currentRoute == null) return false
    return when (targetRoute) {
        Routes.HOME -> currentRoute == Routes.HOME ||
            currentRoute.startsWith("load_detail") ||
            currentRoute.startsWith("edit_load") ||
            currentRoute == Routes.ADD_LOAD
        Routes.STATS -> currentRoute == Routes.STATS
        Routes.PROFILE -> currentRoute == Routes.PROFILE
        Routes.ANALYTICS -> currentRoute == Routes.ANALYTICS || currentRoute == Routes.MAP
        Routes.SETTINGS -> currentRoute == Routes.SETTINGS ||
            currentRoute == Routes.FINANCIAL_ADVISOR
        Routes.CAMERA -> currentRoute == Routes.CAMERA
        else -> currentRoute == targetRoute
    }
}

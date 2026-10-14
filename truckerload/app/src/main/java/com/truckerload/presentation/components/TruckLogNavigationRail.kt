package com.truckerload.presentation.components

import com.truckerload.presentation.icons.AppIcons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.presentation.di.LocalMaintenanceRepository
import com.truckerload.utils.FeedbackManager
import kotlinx.coroutines.flow.map
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

/**
 * Tablet navigation: compact icon rail on 7–10″ portrait, wide labeled sidebar
 * on large landscape windows. Holds every drawer destination so the overlay
 * drawer is not used in tablet mode.
 */
@Composable
fun TruckLogNavigationRail(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onQuickAction: () -> Unit,
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
    val onItemClick: (TabletRailSpec) -> Unit = { item ->
        activateTabletRailItem(
            item = item,
            onNavigate = onNavigate,
            onDrawerNavigate = onDrawerNavigate,
        )
    }

    val maintenance = LocalMaintenanceRepository.current
    val overdue by remember(maintenance) {
        maintenance.watchActiveProgress().map { items -> items.count { it.isDue } }
    }.collectAsStateWithLifecycle(initialValue = 0)
    val railColor = MaterialTheme.colorScheme.surface
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(if (compact) UiDimens.CompactRailWidth else UiDimens.WideSidebarWidth)
            .background(railColor)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outline)
            .padding(
                horizontal = if (compact) 8.dp else 16.dp,
                vertical = if (compact) 12.dp else 20.dp,
            ),
    ) {
        if (compact) {
            CompactTabletRailContent(
                currentRoute = currentRoute,
                overdueCount = overdue,
                onItemClick = onItemClick,
                onQuickAction = onQuickAction,
            )
        } else {
            WideTabletRailContent(
                photoUrl = photoUrl,
                displayName = displayName,
                currentRoute = currentRoute,
                overdueCount = overdue,
                onItemClick = onItemClick,
                onQuickAction = onQuickAction,
                onOpenSettings = { onDrawerNavigate(DrawerDestination.SETTINGS) },
            )
        }
    }
}

@Composable
private fun ColumnScope.WideTabletRailContent(
    photoUrl: String?,
    displayName: String,
    currentRoute: String?,
    overdueCount: Int,
    onItemClick: (TabletRailSpec) -> Unit,
    onQuickAction: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    AppBrandLogo(size = 48.dp, cornerRadius = 12.dp)
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.home_brand_title),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(20.dp))
    SidebarAvatar(photoUrl = photoUrl, name = displayName)
    Spacer(Modifier.height(10.dp))
    Text(
        text = stringResource(R.string.tablet_sidebar_greeting, displayName),
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface,
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
        tabletRailPrimaryItems.forEach { dest ->
            val selected = isTabletRailItemSelected(currentRoute, dest)
            SoftSidebarItem(
                selected = selected,
                icon = railIcon(dest, selected),
                label = stringResource(dest.labelRes),
                badgeCount = if (dest.route == Routes.PROFILE) overdueCount else 0,
                onClick = { onItemClick(dest) },
            )
        }
        RailQuickAction(compact = false, onClick = onQuickAction)
        Spacer(Modifier.height(8.dp))
        RailMoreTools(
            currentRoute = currentRoute,
            compact = false,
            onItemClick = onItemClick,
        )
    }

    Spacer(Modifier.height(12.dp))
    BackupSidebarCard(onOpenSettings = onOpenSettings)
}

@Composable
private fun ColumnScope.CompactTabletRailContent(
    currentRoute: String?,
    overdueCount: Int,
    onItemClick: (TabletRailSpec) -> Unit,
    onQuickAction: () -> Unit,
) {
    Text(
        text = stringResource(R.string.home_brand_title),
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
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
        tabletRailPrimaryItems.forEach { dest ->
            val selected = isTabletRailItemSelected(currentRoute, dest)
            CompactRailItem(
                selected = selected,
                icon = railIcon(dest, selected),
                label = stringResource(dest.labelRes),
                badgeCount = if (dest.route == Routes.PROFILE) overdueCount else 0,
                onClick = { onItemClick(dest) },
            )
        }
        RailQuickAction(compact = true, onClick = onQuickAction)
        RailMoreTools(
            currentRoute = currentRoute,
            compact = true,
            onItemClick = onItemClick,
        )
    }
}

@Composable
private fun SoftSidebarSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 2.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun CompactRailItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    badgeCount: Int = 0,
    onClick: () -> Unit,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .heightIn(min = UiDimens.TouchTarget)
            .padding(vertical = 6.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box {
            Icon(
                icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(UiDimens.IconNav),
            )
            if (badgeCount > 0) {
                RailDueBadge(count = badgeCount)
            }
        }
        Text(
            text = label,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
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
    badgeCount: Int = 0,
    onClick: () -> Unit,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box {
            Icon(
                icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(UiDimens.IconNav),
            )
            if (badgeCount > 0) {
                RailDueBadge(count = badgeCount)
            }
        }
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
            .background(MaterialTheme.colorScheme.surfaceVariant)
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
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = stringResource(R.string.tablet_sidebar_backup_title),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = stringResource(R.string.tablet_sidebar_backup_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onOpenSettings)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.tablet_sidebar_backup_cta),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun RailQuickAction(compact: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    if (compact) {
        Box(
            modifier = Modifier
                .padding(vertical = 6.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(cs.primary)
                .clickable(onClick = {
                    FeedbackManager.onNavSelect()
                    onClick()
                }),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                AppIcons.Add,
                contentDescription = stringResource(R.string.quick_actions_title),
                tint = cs.onPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(cs.primary)
                .clickable(onClick = {
                    FeedbackManager.onNavSelect()
                    onClick()
                })
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                AppIcons.Add,
                contentDescription = null,
                tint = cs.onPrimary,
                modifier = Modifier.size(UiDimens.IconNav),
            )
            Text(
                text = stringResource(R.string.quick_actions_title),
                color = cs.onPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

@Composable
private fun RailMoreTools(
    currentRoute: String?,
    compact: Boolean,
    onItemClick: (TabletRailSpec) -> Unit,
) {
    val toolSelected = tabletRailToolItems.any { isTabletRailItemSelected(currentRoute, it) }
    var open by remember { mutableStateOf(toolSelected) }
    LaunchedEffect(toolSelected) {
        if (toolSelected) open = true
    }
    if (compact) {
        CompactRailItem(
            selected = open,
            icon = if (open) AppIcons.ExpandLess else AppIcons.ExpandMore,
            label = stringResource(R.string.nav_more),
            onClick = { open = !open },
        )
    } else {
        SoftSidebarItem(
            selected = open,
            icon = if (open) AppIcons.ExpandLess else AppIcons.ExpandMore,
            label = stringResource(R.string.nav_more),
            onClick = { open = !open },
        )
    }
    if (!open) return
    tabletRailToolItems.forEach { dest ->
        val selected = isTabletRailItemSelected(currentRoute, dest)
        if (!compact) {
            dest.sectionLabelRes?.let { SoftSidebarSectionLabel(stringResource(it)) }
            SoftSidebarItem(
                selected = selected,
                icon = railIcon(dest, selected),
                label = stringResource(dest.labelRes),
                onClick = { onItemClick(dest) },
            )
        } else {
            CompactRailItem(
                selected = selected,
                icon = railIcon(dest, selected),
                label = stringResource(dest.labelRes),
                onClick = { onItemClick(dest) },
            )
        }
    }
}

@Composable
private fun BoxScope.RailDueBadge(count: Int) {
    val label = if (count > 9) "9+" else count.toString()
    Text(
        text = label,
        color = MaterialTheme.colorScheme.onError,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = 8.dp, y = (-6).dp)
            .heightIn(min = 16.dp)
            .background(MaterialTheme.colorScheme.error, CircleShape)
            .padding(horizontal = 4.dp),
    )
}

private fun railIcon(item: TabletRailSpec, selected: Boolean): ImageVector {
    if (item.isLogout) return AppIcons.Logout
    return when (item.drawer) {
        DrawerDestination.SETTINGS -> AppIcons.Settings
        DrawerDestination.MAP -> AppIcons.Map
        DrawerDestination.PAYCHECK -> AppIcons.Payments
        DrawerDestination.DIESEL -> AppIcons.LocalGasStation
        DrawerDestination.MISC_EXPENSES -> AppIcons.Description
        DrawerDestination.REPORTS -> AppIcons.BarChart
        DrawerDestination.TAX_TRACKER -> AppIcons.TableChart
        DrawerDestination.CAMERA -> AppIcons.CameraAlt
        DrawerDestination.SCANNER -> AppIcons.DocumentScanner
        DrawerDestination.MAINTENANCE -> AppIcons.Build
        DrawerDestination.DOCUMENTS -> AppIcons.Description
        DrawerDestination.IMPROVE -> AppIcons.EditNote
        DrawerDestination.ABOUT -> AppIcons.Info
        null -> when (item.route) {
            Routes.HOME -> if (selected) AppIcons.LocalShipping else AppIcons.LocalShippingOutlined
            Routes.STATS -> if (selected) AppIcons.Flag else AppIcons.FlagOutlined
            Routes.PROFILE -> if (selected) AppIcons.Person else AppIcons.PersonOutlined
            else -> AppIcons.Assignment
        }
    }
}

private fun activateTabletRailItem(
    item: TabletRailSpec,
    onNavigate: (String) -> Unit,
    onDrawerNavigate: (DrawerDestination) -> Unit,
) {
    when {
        item.route != null -> onNavigate(item.route)
        item.drawer != null -> onDrawerNavigate(item.drawer)
    }
}

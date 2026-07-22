package com.truckerload.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors

enum class DrawerDestination {
    PROFILE,
    SETTINGS,
    REPORTS,
    DOCUMENTS,
    SCANNER,
    CAMERA,
    SUPPORT,
    ABOUT,
}

@Composable
fun AppDrawerContent(
    onNavigate: (DrawerDestination) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current

    ModalDrawerSheet(
        modifier = modifier,
        drawerContainerColor = BentoGlassTheme.CardFill,
    ) {
        Column(modifier = Modifier.padding(vertical = 24.dp)) {
            Text(
                text = stringResource(R.string.home_brand_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = tc.AccentPrimary,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = BentoGlassTheme.CardBorderMuted)
            Spacer(Modifier.height(8.dp))

            drawerItem(
                icon = Icons.Outlined.Person,
                label = stringResource(R.string.nav_profile),
                onClick = { onNavigate(DrawerDestination.PROFILE); onClose() },
            )
            drawerItem(
                icon = Icons.Outlined.Settings,
                label = stringResource(R.string.nav_settings),
                onClick = { onNavigate(DrawerDestination.SETTINGS); onClose() },
            )
            drawerItem(
                icon = Icons.Outlined.BarChart,
                label = stringResource(R.string.drawer_reports),
                onClick = { onNavigate(DrawerDestination.REPORTS); onClose() },
            )
            drawerItem(
                icon = Icons.Outlined.Description,
                label = stringResource(R.string.drawer_documents),
                onClick = { onNavigate(DrawerDestination.DOCUMENTS); onClose() },
            )
            drawerItem(
                icon = Icons.Outlined.DocumentScanner,
                label = stringResource(R.string.scanner),
                onClick = { onNavigate(DrawerDestination.SCANNER); onClose() },
            )
            drawerItem(
                icon = Icons.Outlined.CameraAlt,
                label = stringResource(R.string.camera),
                onClick = { onNavigate(DrawerDestination.CAMERA); onClose() },
            )
            drawerItem(
                icon = Icons.Outlined.SupportAgent,
                label = stringResource(R.string.drawer_support),
                onClick = { onNavigate(DrawerDestination.SUPPORT); onClose() },
            )
            drawerItem(
                icon = Icons.Outlined.Info,
                label = stringResource(R.string.drawer_about),
                onClick = { onNavigate(DrawerDestination.ABOUT); onClose() },
            )
        }
    }
}

@Composable
private fun drawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val tc = LocalTruckColors.current
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = label, tint = tc.TextSecondary) },
        label = { Text(label, color = tc.TextPrimary) },
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
    )
}

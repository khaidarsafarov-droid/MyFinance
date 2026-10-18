package com.truckerload.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.truckerload.presentation.theme.LocalBrandPalette
import com.truckerload.presentation.theme.LocalTruckColors

enum class DrawerDestination {
    SETTINGS,
    REPORTS,
    MAP,
    DOCUMENTS,
    MAINTENANCE,
    TAX_TRACKER,
    PAYCHECK,
    DIESEL,
    MISC_EXPENSES,
    CAMERA,
    SCANNER,
    ABOUT,
    IMPROVE,
}

@Composable
fun AppDrawerContent(
    onNavigate: (DrawerDestination) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    currentRoute: String? = null,
) {
    val brand = LocalBrandPalette.current

    ModalDrawerSheet(
        modifier = modifier,
        drawerContainerColor = brand.page,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 24.dp),
        ) {
            AppBrandLogo(
                size = 56.dp,
                cornerRadius = 14.dp,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp),
            )
            Text(
                text = stringResource(R.string.home_brand_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = brand.primary,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = BentoGlassTheme.CardBorderMuted)
            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
            DrawerSectionLabel(stringResource(R.string.drawer_section_finance))
            drawerEntry(DrawerDestination.PAYCHECK, R.string.paycheck_title, currentRoute, onNavigate, onClose)
            drawerEntry(DrawerDestination.DIESEL, R.string.diesel_title, currentRoute, onNavigate, onClose)
            drawerEntry(DrawerDestination.MISC_EXPENSES, R.string.misc_expense_title, currentRoute, onNavigate, onClose)
            drawerEntry(DrawerDestination.REPORTS, R.string.drawer_reports, currentRoute, onNavigate, onClose)
            drawerEntry(DrawerDestination.TAX_TRACKER, R.string.tax_title, currentRoute, onNavigate, onClose)
            drawerEntry(DrawerDestination.MAP, R.string.drawer_map, currentRoute, onNavigate, onClose)

            DrawerSectionLabel(stringResource(R.string.drawer_section_data_entry))
            drawerEntry(DrawerDestination.CAMERA, R.string.camera, currentRoute, onNavigate, onClose)
            drawerEntry(DrawerDestination.SCANNER, R.string.scanner, currentRoute, onNavigate, onClose)

            DrawerSectionLabel(stringResource(R.string.drawer_section_maintenance))
            drawerEntry(DrawerDestination.MAINTENANCE, R.string.maintenance_title, currentRoute, onNavigate, onClose)
            drawerEntry(DrawerDestination.DOCUMENTS, R.string.drawer_documents, currentRoute, onNavigate, onClose)
            drawerEntry(DrawerDestination.SETTINGS, R.string.nav_settings, currentRoute, onNavigate, onClose)
            drawerEntry(DrawerDestination.IMPROVE, R.string.drawer_improve, currentRoute, onNavigate, onClose)
            drawerEntry(DrawerDestination.ABOUT, R.string.drawer_about, currentRoute, onNavigate, onClose)
            }
        }
    }
}

@Composable
private fun DrawerSectionLabel(text: String) {
    val tc = LocalTruckColors.current
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = tc.TextSecondary,
        modifier = Modifier.padding(start = 28.dp, top = 16.dp, bottom = 4.dp, end = 16.dp),
    )
}

@Composable
private fun drawerEntry(
    destination: DrawerDestination,
    labelRes: Int,
    currentRoute: String?,
    onNavigate: (DrawerDestination) -> Unit,
    onClose: () -> Unit,
) {
    val selected = currentRoute != null && isDrawerDestinationSelected(currentRoute, destination)
    drawerItem(
        icon = drawerIcon(destination, selected),
        label = stringResource(labelRes),
        selected = selected,
        onClick = { onNavigate(destination); onClose() },
    )
}

@Composable
private fun drawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    selected: Boolean = false,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    indented: Boolean = false,
) {
    val brand = LocalBrandPalette.current
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = label, tint = if (selected) brand.primary else brand.textSecondary) },
        label = { Text(label, color = brand.textPrimary) },
        selected = selected,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = brand.container,
            unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
        ),
        badge = trailingIcon?.let {
            {
                Icon(it, contentDescription = null, tint = brand.textSecondary)
            }
        },
        modifier = Modifier
            .padding(NavigationDrawerItemDefaults.ItemPadding)
            .then(if (indented) Modifier.padding(start = 16.dp) else Modifier),
    )
}

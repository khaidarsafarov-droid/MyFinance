package com.truckerload.presentation.components

import com.truckerload.presentation.icons.AppIcons

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.di.userComponentManager
import com.truckerload.presentation.di.LocalAuthStore
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.sync.SessionTeardown
import kotlinx.coroutines.launch

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
) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val authStore = LocalAuthStore.current
    val scope = rememberCoroutineScope()
    var showLogoutConfirm by remember { mutableStateOf(false) }

    ModalDrawerSheet(
        modifier = modifier,
        drawerContainerColor = BentoGlassTheme.CardFill,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 24.dp),
        ) {
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
                icon = AppIcons.Settings,
                label = stringResource(R.string.nav_settings),
                onClick = { onNavigate(DrawerDestination.SETTINGS); onClose() },
            )
            drawerItem(
                icon = AppIcons.Map,
                label = stringResource(R.string.drawer_map),
                onClick = { onNavigate(DrawerDestination.MAP); onClose() },
            )

            DrawerSectionLabel(stringResource(R.string.drawer_section_finance))
            drawerItem(
                icon = AppIcons.Payments,
                label = stringResource(R.string.paycheck_title),
                onClick = { onNavigate(DrawerDestination.PAYCHECK); onClose() },
            )
            drawerItem(
                icon = AppIcons.LocalGasStation,
                label = stringResource(R.string.diesel_title),
                onClick = { onNavigate(DrawerDestination.DIESEL); onClose() },
            )
            drawerItem(
                icon = AppIcons.Description,
                label = stringResource(R.string.misc_expense_title),
                onClick = { onNavigate(DrawerDestination.MISC_EXPENSES); onClose() },
            )
            drawerItem(
                icon = AppIcons.BarChart,
                label = stringResource(R.string.drawer_reports),
                onClick = { onNavigate(DrawerDestination.REPORTS); onClose() },
            )
            drawerItem(
                icon = AppIcons.TableChart,
                label = stringResource(R.string.tax_title),
                onClick = { onNavigate(DrawerDestination.TAX_TRACKER); onClose() },
            )

            DrawerSectionLabel(stringResource(R.string.drawer_section_data_entry))
            drawerItem(
                icon = AppIcons.CameraAlt,
                label = stringResource(R.string.camera),
                onClick = { onNavigate(DrawerDestination.CAMERA); onClose() },
            )
            drawerItem(
                icon = AppIcons.DocumentScanner,
                label = stringResource(R.string.scanner),
                onClick = { onNavigate(DrawerDestination.SCANNER); onClose() },
            )

            DrawerSectionLabel(stringResource(R.string.drawer_section_maintenance))
            drawerItem(
                icon = AppIcons.Build,
                label = stringResource(R.string.maintenance_title),
                onClick = { onNavigate(DrawerDestination.MAINTENANCE); onClose() },
            )
            drawerItem(
                icon = AppIcons.Description,
                label = stringResource(R.string.drawer_documents),
                onClick = { onNavigate(DrawerDestination.DOCUMENTS); onClose() },
            )
            drawerItem(
                icon = AppIcons.EditNote,
                label = stringResource(R.string.drawer_improve),
                onClick = { onNavigate(DrawerDestination.IMPROVE); onClose() },
            )
            drawerItem(
                icon = AppIcons.Info,
                label = stringResource(R.string.drawer_about),
                onClick = { onNavigate(DrawerDestination.ABOUT); onClose() },
            )

            Spacer(Modifier.weight(1f))
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = BentoGlassTheme.CardBorderMuted,
            )
            drawerItem(
                icon = AppIcons.Logout,
                label = stringResource(R.string.drawer_logout),
                onClick = {
                    onClose()
                    showLogoutConfirm = true
                },
            )
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = {
                Text(
                    stringResource(R.string.settings_logout_confirm_title),
                    color = tc.TextPrimary,
                )
            },
            text = {
                Text(
                    stringResource(R.string.settings_logout_confirm_message),
                    color = tc.TextSecondary,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            SessionTeardown.signOut(
                                context = context,
                                authStore = authStore,
                                endSession = { context.userComponentManager().endSession() },
                            )
                            showLogoutConfirm = false
                            onClose()
                            Toast.makeText(
                                context,
                                context.getString(R.string.settings_logout_success),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                ) {
                    Text(stringResource(R.string.settings_logout_button))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutConfirm = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
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
private fun drawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    indented: Boolean = false,
) {
    val tc = LocalTruckColors.current
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = label, tint = tc.TextSecondary) },
        label = { Text(label, color = tc.TextPrimary) },
        selected = false,
        onClick = onClick,
        badge = trailingIcon?.let {
            {
                Icon(it, contentDescription = null, tint = tc.TextSecondary)
            }
        },
        modifier = Modifier
            .padding(NavigationDrawerItemDefaults.ItemPadding)
            .then(if (indented) Modifier.padding(start = 16.dp) else Modifier),
    )
}

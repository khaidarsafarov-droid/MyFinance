package com.truckerload.presentation.components

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SupportAgent
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
import com.truckerload.BuildConfig
import com.truckerload.R
import com.truckerload.data.local.AppDatabase
import com.truckerload.presentation.di.LocalAuthStore
import com.truckerload.presentation.di.LocalUserProfileStore
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.sync.TelegramBotForegroundService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val context = LocalContext.current
    val authStore = LocalAuthStore.current
    val userProfileStore = LocalUserProfileStore.current
    val scope = rememberCoroutineScope()
    var showLogoutConfirm by remember { mutableStateOf(false) }

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

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = BentoGlassTheme.CardBorderMuted)
            Spacer(Modifier.height(8.dp))
            drawerItem(
                icon = Icons.AutoMirrored.Outlined.Logout,
                label = stringResource(R.string.drawer_logout),
                onClick = {
                    if (BuildConfig.LOCAL_ONLY_MODE) {
                        onClose()
                        Toast.makeText(
                            context,
                            context.getString(R.string.drawer_logout_local_only),
                            Toast.LENGTH_SHORT,
                        ).show()
                    } else {
                        showLogoutConfirm = true
                    }
                },
            )
        }
    }

    if (showLogoutConfirm && !BuildConfig.LOCAL_ONLY_MODE) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text(stringResource(R.string.settings_logout_confirm_title), color = tc.TextPrimary) },
            text = { Text(stringResource(R.string.settings_logout_confirm_message), color = tc.TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            // Stop Telegram first so it cannot write into a closed Room pool.
                            TelegramBotForegroundService.stopForLogout(context)
                            delay(300)
                            AppDatabase.closeCurrent()
                            userProfileStore.unbind()
                            authStore.logout()
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

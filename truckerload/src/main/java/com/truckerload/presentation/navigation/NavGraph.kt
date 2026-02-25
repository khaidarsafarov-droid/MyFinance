package com.truckerload.presentation.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.screens.add.AddDieselScreen
import com.truckerload.presentation.screens.add.AddLoadScreen
import com.truckerload.presentation.screens.add.AddPaycheckScreen
import com.truckerload.presentation.screens.detail.LoadDetailScreen
import com.truckerload.presentation.screens.edit.EditLoadScreen
import com.truckerload.presentation.screens.finance.FinanceScreen
import com.truckerload.presentation.screens.home.HomeScreen
import com.truckerload.presentation.screens.chat.ChatScreen
import com.truckerload.presentation.screens.settings.SettingsScreen
import com.truckerload.presentation.screens.stats.StatsScreen

object Routes {
    const val HOME = "home"
    const val FINANCE = "finance"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val CHAT = "chat"
    const val LOAD_DETAIL = "load_detail/{loadId}"
    const val ADD_LOAD = "add_load"
    const val EDIT_LOAD = "edit_load/{loadId}"
    const val ADD_PAYCHECK = "add_paycheck"
    const val ADD_DIESEL = "add_diesel"

    fun loadDetail(loadId: String) = "load_detail/$loadId"
    fun editLoad(loadId: String) = "edit_load/$loadId"
}

@Composable
fun NavGraph(navController: androidx.navigation.NavHostController = rememberNavController()) {
    val tc = LocalTruckColors.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = currentDestination?.route in listOf(Routes.HOME, Routes.FINANCE, Routes.CHAT, Routes.STATS, Routes.SETTINGS)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Column(Modifier.padding(0.dp)) {
                    HorizontalDivider(color = tc.Divider, thickness = 1.dp)
                    NavigationBar(
                        containerColor = tc.Background,
                        contentColor = tc.TextPrimary,
                        tonalElevation = 0.dp
                    ) {
                        listOf(
                            Triple(Routes.HOME, Icons.Default.LocalShipping, "Лоуды"),
                            Triple(Routes.FINANCE, Icons.Default.Wallet, "Финансы"),
                            Triple(Routes.CHAT, Icons.Default.Chat, "Чат"),
                            Triple(Routes.STATS, Icons.Default.BarChart, "Статистика"),
                            Triple(Routes.SETTINGS, Icons.Default.Settings, "Настройки")
                        ).forEach { (route, icon, label) ->
                            val selected = currentDestination?.hierarchy?.any { it.route == route } == true
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        icon,
                                        contentDescription = label,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = { Text(label) },
                                selected = selected,
                                onClick = {
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = tc.AccentPrimary,
                                    selectedTextColor = tc.AccentPrimary,
                                    indicatorColor = tc.AccentPrimary.copy(alpha = 0.15f),
                                    unselectedIconColor = tc.TextSecondary,
                                    unselectedTextColor = tc.TextSecondary
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onLoadClick = { navController.navigate(Routes.loadDetail(it)) },
                    onAddLoad = { navController.navigate(Routes.ADD_LOAD) },
                    onStats = { navController.navigate(Routes.STATS) },
                    onSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }
            composable(Routes.FINANCE) {
                FinanceScreen(
                    onAddPaycheck = { navController.navigate(Routes.ADD_PAYCHECK) },
                    onAddDiesel = { navController.navigate(Routes.ADD_DIESEL) },
                    onLoadClick = { navController.navigate(Routes.loadDetail(it)) }
                )
            }
            composable(Routes.CHAT) {
                ChatScreen()
            }
            composable(Routes.STATS) {
                StatsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.LOAD_DETAIL,
                arguments = listOf(navArgument("loadId") { type = NavType.StringType })
            ) { backStackEntry ->
                val loadId = backStackEntry.arguments?.getString("loadId").orEmpty()
                LoadDetailScreen(
                    loadId = loadId,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Routes.editLoad(loadId)) },
                    onDelete = { navController.popBackStack() }
                )
            }
            composable(Routes.ADD_LOAD) {
                AddLoadScreen(onSaved = { navController.popBackStack() }, onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.EDIT_LOAD,
                arguments = listOf(navArgument("loadId") { type = NavType.StringType })
            ) { backStackEntry ->
                val loadId = backStackEntry.arguments?.getString("loadId").orEmpty()
                EditLoadScreen(loadId = loadId, onSaved = { navController.popBackStack() }, onBack = { navController.popBackStack() })
            }
            composable(Routes.ADD_PAYCHECK) {
                AddPaycheckScreen(onSaved = { navController.popBackStack() }, onBack = { navController.popBackStack() })
            }
            composable(Routes.ADD_DIESEL) {
                AddDieselScreen(onSaved = { navController.popBackStack() }, onBack = { navController.popBackStack() })
            }
        }
    }
}

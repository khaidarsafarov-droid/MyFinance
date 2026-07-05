package com.truckerload.presentation.navigation

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckerload.BuildConfig
import com.truckerload.R
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.screens.home.HomeViewModel
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
import com.truckerload.presentation.theme.FinanceCockpitColors
import com.truckerload.presentation.screens.add.AddDieselScreen
import com.truckerload.presentation.screens.add.AddLoadScreen
import com.truckerload.presentation.screens.add.AddPaycheckScreen
import com.truckerload.presentation.screens.detail.LoadDetailScreen
import com.truckerload.presentation.screens.edit.EditLoadScreen
import com.truckerload.presentation.screens.finance.FinanceScreen
import com.truckerload.presentation.screens.home.HomeScreen
import com.truckerload.presentation.screens.stats.StatsScreen
import com.truckerload.presentation.screens.tax.TaxTrackerScreen
import com.truckerload.presentation.screens.advisor.FinancialAdvisorScreen
import com.truckerload.presentation.screens.map.MapScreen
import com.truckerload.presentation.screens.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val FINANCE = "finance"
    const val STATS = "stats"
    const val MAP = "map"
    const val LOAD_DETAIL = "load_detail/{loadId}"
    const val ADD_LOAD = "add_load"
    const val EDIT_LOAD = "edit_load/{loadId}"
    const val ADD_PAYCHECK = "add_paycheck"
    const val ADD_DIESEL = "add_diesel"
    const val TAX_TRACKER = "tax_tracker"
    const val FINANCIAL_ADVISOR = "financial_advisor"
    const val SETTINGS = "settings"

    fun loadDetail(loadId: String) = "load_detail/$loadId"
    fun editLoad(loadId: String) = "edit_load/$loadId"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun NavGraph(navController: androidx.navigation.NavHostController = rememberNavController()) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = currentDestination?.route in listOf(Routes.HOME, Routes.FINANCE, Routes.STATS, Routes.SETTINGS)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Column(Modifier.padding(0.dp)) {
                    HorizontalDivider(color = tc.Divider, thickness = 1.dp)
                    NavigationBar(
                        containerColor = tc.CardBackground,
                        contentColor = tc.TextPrimary,
                        tonalElevation = 0.dp
                    ) {
                        listOf(
                            Triple(Routes.FINANCE, Icons.Default.Wallet, stringResource(R.string.nav_dashboard)),
                            Triple(Routes.STATS, Icons.Default.BarChart, stringResource(R.string.nav_analytics)),
                            Triple(Routes.HOME, Icons.Default.Description, stringResource(R.string.nav_logbook)),
                            Triple(Routes.SETTINGS, Icons.Default.Person, stringResource(R.string.nav_settings))
                        ).forEach { (route, icon, label) ->
                            val selected = currentDestination?.hierarchy?.any { it.route == route } == true
                            NavigationBarItem(
                                modifier = Modifier.sizeIn(minWidth = 72.dp, minHeight = 56.dp),
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
                                    selectedIconColor = FinanceCockpitColors.ActiveHighlight,
                                    selectedTextColor = FinanceCockpitColors.ActiveHighlight,
                                    indicatorColor = FinanceCockpitColors.ActiveDateBackground,
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
            startDestination = Routes.FINANCE,
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
            composable(Routes.STATS) {
                StatsScreen(
                    onBack = { navController.popBackStack() },
                    onSettings = { navController.navigate(Routes.SETTINGS) },
                    onFinancialAdvisor = { navController.navigate(Routes.FINANCIAL_ADVISOR) },
                    onOpenMap = { navController.navigate(Routes.MAP) }
                )
            }
            composable(Routes.MAP) {
                MapScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.TAX_TRACKER) {
                TaxTrackerScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.FINANCIAL_ADVISOR) {
                FinancialAdvisorScreen(onBack = { navController.popBackStack() })
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
                val loadRepository = LocalLoadRepository.current
                val isBotConfigured = BuildConfig.TELEGRAM_BOT_TOKEN.isNotEmpty() && BuildConfig.CEREBRAS_API_KEY.isNotEmpty()
                val homeEntry = try { navController.getBackStackEntry(Routes.HOME) } catch (_: Exception) { null }
                val homeViewModel: HomeViewModel? = homeEntry?.let {
                    viewModel(it, factory = HomeViewModel.Factory(loadRepository, isBotConfigured, context))
                }
                AddLoadScreen(
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                    onOptimisticInsert = homeViewModel?.let { { load -> it.applyOptimisticUpdate(load) } },
                    onRevertOptimistic = homeViewModel?.let { { id -> it.revertOptimisticUpdate(id) } }
                )
            }
            composable(
                route = Routes.EDIT_LOAD,
                arguments = listOf(navArgument("loadId") { type = NavType.StringType })
            ) { editBackStackEntry ->
                val loadId = editBackStackEntry.arguments?.getString("loadId").orEmpty()
                val loadRepository = LocalLoadRepository.current
                val isBotConfigured = BuildConfig.TELEGRAM_BOT_TOKEN.isNotEmpty() && BuildConfig.CEREBRAS_API_KEY.isNotEmpty()
                val homeEntry = try { navController.getBackStackEntry(Routes.HOME) } catch (_: Exception) { null }
                val homeViewModel: HomeViewModel? = homeEntry?.let {
                    viewModel(it, factory = HomeViewModel.Factory(loadRepository, isBotConfigured, context))
                }
                EditLoadScreen(
                    loadId = loadId,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                    onOptimisticUpdate = homeViewModel?.let { { load -> it.applyOptimisticUpdate(load) } },
                    onRevertOptimistic = homeViewModel?.let { { id -> it.revertOptimisticUpdate(id) } }
                )
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

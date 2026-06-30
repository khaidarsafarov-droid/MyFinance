package com.truckerload.presentation.navigation

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckerload.data.preferences.TelegramTokenStore
import com.truckerload.presentation.di.LocalAuthStore
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.screens.home.HomeViewModel
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.truckerload.presentation.theme.tabEnterTransition
import com.truckerload.presentation.theme.tabExitTransition
import com.truckerload.presentation.components.AdaptiveScaffold
import com.truckerload.presentation.components.navigateToMainRoute
import com.truckerload.presentation.utils.AdaptiveScreenContainer
import com.truckerload.presentation.utils.isTablet
import com.truckerload.presentation.screens.add.AddDieselScreen
import com.truckerload.presentation.screens.add.AddLoadScreen
import com.truckerload.presentation.screens.add.AddPaycheckScreen
import com.truckerload.presentation.screens.detail.LoadDetailScreen
import com.truckerload.presentation.screens.edit.EditLoadScreen
import com.truckerload.presentation.screens.home.HomeScreen
import com.truckerload.presentation.screens.goal.WeeklyGoalScreen
import com.truckerload.presentation.screens.tax.TaxTrackerScreen
import com.truckerload.presentation.screens.advisor.FinancialAdvisorScreen
import com.truckerload.presentation.screens.map.MapScreen
import com.truckerload.presentation.navigation.AuthNavHost
import com.truckerload.presentation.screens.settings.SettingsScreen
import com.truckerload.presentation.screens.analytics.AnalyticsScreen
import com.truckerload.presentation.screens.stats.StatsScreen
import com.truckerload.widget.WidgetDeepLink

object Routes {
    const val HOME = "home"
    const val STATS = "stats"
    const val ANALYTICS = "analytics"
    const val ADVANCED_STATS = "advanced_stats"
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
fun NavGraph(
    navController: androidx.navigation.NavHostController = rememberNavController(),
    deepLinkRoute: String? = null,
    onDeepLinkHandled: () -> Unit = {}
) {
    val authStore = LocalAuthStore.current
    val isLoggedIn by authStore.isLoggedIn.collectAsState()
    var showMainContent by remember { mutableStateOf(true) }
    var hasShownAuth by remember { mutableStateOf(false) }
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            hasShownAuth = true
            showMainContent = false
        } else {
            if (hasShownAuth) {
                showMainContent = false
                delay(200)
            }
            showMainContent = true
        }
    }
    val context = LocalContext.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val tablet = isTablet()

    LaunchedEffect(deepLinkRoute, isLoggedIn, showMainContent) {
        if (!isLoggedIn || !showMainContent) return@LaunchedEffect
        when (deepLinkRoute) {
            Routes.HOME, WidgetDeepLink.ROUTE_HOME -> {
                navController.navigate(Routes.HOME) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
                onDeepLinkHandled()
            }
            Routes.ADD_LOAD, WidgetDeepLink.ROUTE_ADD_LOAD -> {
                navController.navigate(Routes.ADD_LOAD)
                onDeepLinkHandled()
            }
            WidgetDeepLink.ROUTE_STATS -> {
                navController.navigate(Routes.ANALYTICS) {
                    launchSingleTop = true
                }
                onDeepLinkHandled()
            }
            WidgetDeepLink.ROUTE_JOURNAL_THIS_WEEK -> {
                navController.navigate(Routes.HOME) {
                    launchSingleTop = true
                }
                onDeepLinkHandled()
            }
            WidgetDeepLink.ROUTE_WEEKLY_GOAL -> {
                navController.navigate(Routes.STATS) {
                    launchSingleTop = true
                }
                onDeepLinkHandled()
            }
        }
    }

    if (!isLoggedIn) {
        AuthNavHost(authStore = authStore, onLoginSuccess = { authStore.login() })
        return
    }
    if (!showMainContent) {
        return
    }
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route
    val phoneMainRoutes = listOf(Routes.HOME, Routes.STATS, Routes.ANALYTICS, Routes.SETTINGS)
    val showMainNavigation = if (tablet) {
        currentRoute != Routes.ADD_PAYCHECK && currentRoute != Routes.ADD_DIESEL
    } else {
        currentRoute in phoneMainRoutes
    }

    AdaptiveScaffold(
        showMainNavigation = showMainNavigation,
        currentRoute = currentRoute,
        onNavigate = { route -> navigateToMainRoute(route, navController) },
    ) { padding ->
        AdaptiveScreenContainer(modifier = Modifier.padding(padding)) {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(
                route = Routes.HOME,
                enterTransition = { tabEnterTransition() },
                exitTransition = { tabExitTransition() },
                popEnterTransition = { tabEnterTransition() },
                popExitTransition = { tabExitTransition() },
            ) {
                HomeScreen(
                    onLoadClick = { navController.navigate(Routes.loadDetail(it)) },
                    onAddLoad = { navController.navigate(Routes.ADD_LOAD) },
                    onStats = { navController.navigate(Routes.ADVANCED_STATS) },
                    onWeeklyGoal = { navController.navigate(Routes.STATS) },
                    onSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }
            composable(
                route = Routes.ANALYTICS,
                enterTransition = { tabEnterTransition() },
                exitTransition = { tabExitTransition() },
                popEnterTransition = { tabEnterTransition() },
                popExitTransition = { tabExitTransition() },
            ) {
                AnalyticsScreen(
                    onAdvancedStats = { navController.navigate(Routes.ADVANCED_STATS) },
                )
            }
            composable(Routes.ADVANCED_STATS) {
                StatsScreen(
                    onBack = { navController.popBackStack() },
                    showBack = !tablet,
                    onOpenMap = { navController.navigate(Routes.MAP) },
                    onFinancialAdvisor = { navController.navigate(Routes.FINANCIAL_ADVISOR) }
                )
            }
            composable(
                route = Routes.STATS,
                enterTransition = { tabEnterTransition() },
                exitTransition = { tabExitTransition() },
                popEnterTransition = { tabEnterTransition() },
                popExitTransition = { tabExitTransition() },
            ) {
                WeeklyGoalScreen()
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
            composable(
                route = Routes.SETTINGS,
                enterTransition = { tabEnterTransition() },
                exitTransition = { tabExitTransition() },
                popEnterTransition = { tabEnterTransition() },
                popExitTransition = { tabExitTransition() },
            ) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onTaxTracker = { navController.navigate(Routes.TAX_TRACKER) },
                    showBack = !tablet
                )
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
                val isBotConfigured = TelegramTokenStore(context).hasToken()
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
                val isBotConfigured = TelegramTokenStore(context).hasToken()
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
}

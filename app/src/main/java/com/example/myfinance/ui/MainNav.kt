package com.example.myfinance.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNav(
    viewModel: LogisticsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val app = LocalContext.current.applicationContext as android.app.Application
    val telegramVm = viewModel<TelegramViewModel>(
        factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                TelegramViewModel(app) as T
        }
    )
    val appData by viewModel.appData.collectAsState()
    val chatId by telegramVm.chatId.collectAsState(initial = null)
    val hasCompanies = appData.companies.isNotEmpty()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    if (!hasCompanies) {
        SetupCompanyScreen(viewModel = viewModel)
        return
    }

    androidx.compose.runtime.LaunchedEffect(chatId) {
        if (chatId != null) {
            telegramVm.startRealtimeSync(
                getCurrentCompanyId = { viewModel.getCurrentCompany()?.id },
                onAddWeeklyTotal = { viewModel.addWeeklyTotalFromParsed(it, viewModel.appData.value.companies) },
                onAddTrip = { viewModel.addTripFromParsed(it, viewModel.getCurrentCompany()?.id) }
            )
        } else {
            telegramVm.stopRealtimeSync()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            when (currentDestination?.route) {
                Nav.SUMMARY -> {
                    TopAppBar(
                        title = { Text("Logistics Tracker") },
                        actions = {
                            IconButton(onClick = { navController.navigate(Nav.ADD_WEEKLY_TOTAL) }) {
                                Icon(Icons.Default.Add, contentDescription = "Add weekly total")
                            }
                        }
                    )
                }
                Nav.ADD_WEEKLY_TOTAL, Nav.ADD_WEEKLY_TOTAL_FOR_COMPANY -> {
                    TopAppBar(
                        title = { Text("Add Weekly Total") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
                Nav.COMPANY -> {
                    val companyId = navBackStackEntry?.arguments?.getString("companyId")
                    val company = appData.companies.find { it.id == companyId }
                    TopAppBar(
                        title = { Text(company?.name ?: "Company") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            if (companyId != null) {
                                IconButton(onClick = { navController.navigate(Nav.ADD_WEEKLY_TOTAL_FOR_COMPANY.replace("{companyId}", companyId)) }) {
                                    Icon(Icons.Default.Add, contentDescription = "Add weekly total")
                                }
                            }
                        }
                    )
                }
                Nav.LOADS -> {
                    TopAppBar(
                        title = { Text("Loads") },
                        actions = {
                            IconButton(onClick = {
                                viewModel.syncAllTripsToCalendar { }
                            }) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = "Sync all to calendar")
                            }
                            IconButton(onClick = { navController.navigate(Nav.ADD_TRIP) }) {
                                Icon(Icons.Default.Add, contentDescription = "Add load")
                            }
                        }
                    )
                }
                Nav.EDIT_WEEKLY_TOTAL -> {
                    TopAppBar(
                        title = { Text("Edit weekly total") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
                Nav.EDIT_TRIP -> {
                    TopAppBar(
                        title = { Text("Edit load") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
                Nav.ADD_TRIP -> {
                    TopAppBar(
                        title = { Text("Add load") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
                Nav.ANALYTICS -> TopAppBar(title = { Text("Analytics") })
                Nav.AI_CHAT -> TopAppBar(title = { Text("AI Assistant") })
                Nav.SETTINGS -> TopAppBar(title = { Text("Settings") })
            }
        },
        floatingActionButton = {
            when (currentDestination?.route) {
                Nav.SUMMARY -> {
                    FloatingActionButton(
                        onClick = { navController.navigate(Nav.ADD_WEEKLY_TOTAL) },
                        shape = RoundedCornerShape(16.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add weekly total")
                    }
                }
                Nav.COMPANY -> {
                    val companyId = navBackStackEntry?.arguments?.getString("companyId")
                    if (companyId != null) {
                        FloatingActionButton(
                            onClick = { navController.navigate(Nav.ADD_WEEKLY_TOTAL_FOR_COMPANY.replace("{companyId}", companyId)) },
                            shape = RoundedCornerShape(16.dp),
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add weekly total")
                        }
                    }
                }
                else -> { }
            }
        },
        bottomBar = {
            if (currentDestination?.route in listOf(Nav.LOADS, Nav.AI_CHAT, Nav.ANALYTICS, Nav.SETTINGS)) {
                NavigationBar {
                    listOf(
                        Triple(Nav.LOADS, "Loads", Icons.Default.LocalShipping),
                        Triple(Nav.AI_CHAT, "Chat", Icons.Default.Chat),
                        Triple(Nav.ANALYTICS, "Analytics", Icons.Default.BarChart),
                        Triple(Nav.SETTINGS, "Settings", Icons.Default.Settings)
                    ).forEach { (route, label, icon) ->
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = null) },
                            label = { Text(label) },
                            selected = currentDestination?.hierarchy?.any { it.route == route } == true,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Nav.LOADS,
            modifier = Modifier.padding(padding)
        ) {
            composable(Nav.SUMMARY) {
                SummaryScreen(
                    viewModel = viewModel,
                    onAddWeeklyTotal = { navController.navigate(Nav.ADD_WEEKLY_TOTAL) },
                    onEditWeeklyTotal = { id -> navController.navigate(Nav.editWeeklyTotal(id)) },
                    onCompany = { id -> navController.navigate(Nav.company(id)) },
                    onAnalytics = { navController.navigate(Nav.ANALYTICS) },
                    onSettings = { navController.navigate(Nav.SETTINGS) }
                )
            }
            composable(Nav.ADD_WEEKLY_TOTAL) {
                AddWeeklyTotalScreen(
                    viewModel = viewModel,
                    companyId = null,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(
                route = Nav.ADD_WEEKLY_TOTAL_FOR_COMPANY,
                arguments = listOf(navArgument("companyId") { type = NavType.StringType })
            ) { backStackEntry ->
                val companyId = backStackEntry.arguments?.getString("companyId")
                AddWeeklyTotalScreen(
                    viewModel = viewModel,
                    companyId = companyId,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(
                route = Nav.EDIT_WEEKLY_TOTAL,
                arguments = listOf(navArgument("weeklyTotalId") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("weeklyTotalId")
                val wt = appData.weeklyTotals.find { it.id == id }
                EditWeeklyTotalScreen(
                    weeklyTotal = wt,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(
                route = Nav.EDIT_TRIP,
                arguments = listOf(navArgument("tripId") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("tripId")
                val trip = appData.trips.find { it.id == id }
                EditTripScreen(
                    trip = trip,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(
                route = Nav.COMPANY,
                arguments = listOf(navArgument("companyId") { type = NavType.StringType })
            ) { backStackEntry ->
                val companyId = backStackEntry.arguments?.getString("companyId") ?: return@composable
                val company = appData.companies.find { it.id == companyId }
                val companyWeeklyTotals = appData.weeklyTotals.filter { it.companyIds.contains(companyId) }
                if (company != null) {
                    CompanyScreen(
                        companyId = companyId,
                        companyName = company.name,
                        weeklyTotals = companyWeeklyTotals,
                        isCurrentCompany = company.isCurrent,
                        viewModel = viewModel,
                        onAddWeeklyTotal = { navController.navigate(Nav.ADD_WEEKLY_TOTAL_FOR_COMPANY.replace("{companyId}", companyId)) },
                        onEditWeeklyTotal = { id -> navController.navigate(Nav.editWeeklyTotal(id)) },
                        onSetAsCurrent = { viewModel.setCurrentCompany(companyId) }
                    )
                }
            }
            composable(Nav.ADD_TRIP) {
                AddTripScreen(
                    viewModel = viewModel,
                    companyId = null,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(Nav.LOADS) {
                LoadsScreen(
                    trips = appData.trips,
                    viewModel = viewModel,
                    onEditTrip = { id -> navController.navigate(Nav.editTrip(id)) }
                )
            }
            composable(Nav.ANALYTICS) {
                AnalyticsScreen(weeklyTotals = appData.weeklyTotals)
            }
            composable(Nav.AI_CHAT) {
                val app = LocalContext.current.applicationContext as android.app.Application
                val chatVm = viewModel<ChatViewModel>(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                            ChatViewModel(app) as T
                    }
                )
                val contextString = chatVm.buildContextFromAppData(appData)
                ChatScreen(
                    viewModel = chatVm,
                    appDataContext = contextString,
                    telegramViewModel = telegramVm,
                    logisticsViewModel = viewModel,
                    companies = appData.companies
                )
            }
            composable(Nav.SETTINGS) {
                SettingsScreen(
                    companies = appData.companies,
                    weeklyTotals = appData.weeklyTotals,
                    trips = appData.trips,
                    onOpenCompany = { id -> navController.navigate(Nav.company(id)) },
                    onAddCompany = { viewModel.addCompany(it) },
                    logisticsViewModel = viewModel,
                    telegramViewModel = telegramVm
                )
            }
        }
    }
}

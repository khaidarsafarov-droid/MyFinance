package com.truckerload.presentation.components

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.truckerload.presentation.navigation.Routes

enum class MainTabClickAction {
    NO_OP,
    POP_TO_TAB_ROOT,
    POP_ONCE,
    SWITCH_TAB,
}

fun shouldShowPhoneBottomBar(route: String?): Boolean =
    phoneTabForRoute(route) != null

fun isPhoneDestinationSelected(currentRoute: String?, targetRoute: String): Boolean =
    phoneTabForRoute(currentRoute) == targetRoute

/**
 * Bottom-tab owner for the current destination. Nested screens (map, load
 * detail) stay on the tab they were opened from so the pill highlight
 * still shows where the user is.
 */
fun phoneTabForRoute(currentRoute: String?): String? {
    val route = currentRoute ?: return null
    if (isImmersivePhoneRoute(route)) return null
    return when {
        isHomeTabRoute(route) -> Routes.HOME
        isStatsTabRoute(route) -> Routes.STATS
        isProfileTabRoute(route) -> Routes.PROFILE
        else -> null
    }
}

/**
 * Tapping the already-selected tab pops nested screens (map → statistics)
 * instead of trapping the user until system Back.
 */
fun resolveMainTabClick(
    currentRoute: String?,
    targetTab: String,
    tabRootInBackStack: Boolean,
): MainTabClickAction {
    val currentTab = phoneTabForRoute(currentRoute)
    if (currentTab != targetTab) return MainTabClickAction.SWITCH_TAB
    if (currentRoute == targetTab) return MainTabClickAction.NO_OP
    if (tabRootInBackStack) return MainTabClickAction.POP_TO_TAB_ROOT
    return MainTabClickAction.POP_ONCE
}

fun navigateToMainRoute(
    route: String,
    navController: NavHostController,
) {
    val current = navController.currentDestination?.route
    when (resolveMainTabClick(current, route, tabRootInBackStack = true)) {
        MainTabClickAction.NO_OP -> Unit
        MainTabClickAction.POP_TO_TAB_ROOT,
        MainTabClickAction.POP_ONCE,
        -> {
            if (!navController.popBackStack(route, inclusive = false)) {
                navController.popBackStack()
            }
        }
        MainTabClickAction.SWITCH_TAB -> {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
}

internal fun isImmersivePhoneRoute(route: String): Boolean =
    route == Routes.CAMERA ||
        route.startsWith("camera_load") ||
        route == Routes.SCANNER ||
        route.startsWith("scanner_load") ||
        route.startsWith("attach_pick") ||
        route == Routes.SCAN_GALLERY ||
        route == Routes.PHOTO_GALLERY ||
        route.startsWith("photo_detail") ||
        route == Routes.ADD_PAYCHECK ||
        route == Routes.ADD_DIESEL ||
        route == Routes.DIESEL ||
        route == Routes.VOICE_ASSISTANT

internal fun isHomeTabRoute(route: String): Boolean =
    route == Routes.HOME ||
        route.startsWith("load_detail") ||
        route.startsWith("edit_load") ||
        route == Routes.ADD_LOAD

internal fun isStatsTabRoute(route: String): Boolean =
    route == Routes.STATS ||
        route == Routes.ANALYTICS ||
        route == Routes.MAP ||
        route == Routes.FINANCIAL_ADVISOR

internal fun isProfileTabRoute(route: String): Boolean =
    route == Routes.PROFILE ||
        route == Routes.PROFILE_EDIT

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

fun shouldShowPhoneBottomBar(route: String?): Boolean {
    val current = route ?: return false
    if (isImmersivePhoneRoute(current)) return false
    return phoneTabForRoute(current) != null || isPhoneChromeToolRoute(current)
}

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
    val tabRootInBackStack = tabRootInBackStack(navController, route)
    when (resolveMainTabClick(current, route, tabRootInBackStack)) {
        MainTabClickAction.NO_OP -> Unit
        MainTabClickAction.POP_TO_TAB_ROOT,
        MainTabClickAction.POP_ONCE,
        -> {
            if (!navController.popBackStack(route, inclusive = false)) {
                navController.popBackStack()
            }
        }
        MainTabClickAction.SWITCH_TAB -> {
            // Profile/settings stacks: pop to tab root when it exists instead of
            // launchSingleTop leaving tool screens on top.
            if (navController.popBackStack(route, inclusive = false)) {
                return
            }
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
}

private fun tabRootInBackStack(navController: NavHostController, route: String): Boolean =
    navController.currentBackStack.value.any { it.destination.route == route }

internal fun isImmersivePhoneRoute(route: String): Boolean =
    route == Routes.CAMERA ||
        route.startsWith("camera_load") ||
        route == Routes.SCANNER ||
        route.startsWith("scanner_load") ||
        route.startsWith("attach_pick") ||
        route == Routes.SCAN_GALLERY ||
        route == Routes.PHOTO_GALLERY ||
        route.startsWith("photo_detail") ||
        route == Routes.PAYCHECK ||
        route == Routes.ADD_PAYCHECK ||
        route.startsWith(Routes.ADD_DIESEL) ||
        route == Routes.DIESEL ||
        route == Routes.MISC_EXPENSES

/** Non-immersive tool screens where phone users still need the bottom tab bar. */
internal fun isPhoneChromeToolRoute(route: String): Boolean =
    route == Routes.SETTINGS ||
        route == Routes.PRIVACY_SETTINGS ||
        route == Routes.ABOUT ||
        route == Routes.IMPROVE ||
        route == Routes.MAINTENANCE ||
        route == Routes.TAX_TRACKER

internal fun isHomeTabRoute(route: String): Boolean =
    route == Routes.HOME ||
        route.startsWith("load_detail") ||
        route.startsWith("edit_load") ||
        route == Routes.ADD_LOAD

internal fun isStatsTabRoute(route: String): Boolean =
    route == Routes.STATS ||
        route == Routes.ANALYTICS ||
        route == Routes.MAP

internal fun isProfileTabRoute(route: String): Boolean =
    route == Routes.PROFILE

package com.truckerload.presentation.components

import com.truckerload.R
import com.truckerload.presentation.navigation.Routes

/**
 * Permanent tablet rail items. Phone still uses [AppDrawerContent]; tablet puts
 * every drawer destination in the colored left rail so the overlay drawer is unused.
 */
internal data class TabletRailSpec(
    val drawer: DrawerDestination? = null,
    val route: String? = null,
    val labelRes: Int,
    val sectionLabelRes: Int? = null,
    val isLogout: Boolean = false,
)

internal val tabletRailPrimaryItems: List<TabletRailSpec> = listOf(
    TabletRailSpec(route = Routes.HOME, labelRes = R.string.nav_logbook),
    TabletRailSpec(route = Routes.STATS, labelRes = R.string.nav_weekly_goal),
    TabletRailSpec(route = Routes.PROFILE, labelRes = R.string.nav_profile),
)

internal val tabletRailToolItems: List<TabletRailSpec> = listOf(
    TabletRailSpec(drawer = DrawerDestination.SETTINGS, labelRes = R.string.nav_settings),
    TabletRailSpec(drawer = DrawerDestination.MAP, labelRes = R.string.drawer_map),
    TabletRailSpec(
        drawer = DrawerDestination.PAYCHECK,
        labelRes = R.string.paycheck_title,
        sectionLabelRes = R.string.drawer_section_finance,
    ),
    TabletRailSpec(drawer = DrawerDestination.DIESEL, labelRes = R.string.diesel_title),
    TabletRailSpec(drawer = DrawerDestination.MISC_EXPENSES, labelRes = R.string.misc_expense_title),
    TabletRailSpec(drawer = DrawerDestination.REPORTS, labelRes = R.string.drawer_reports),
    TabletRailSpec(drawer = DrawerDestination.TAX_TRACKER, labelRes = R.string.tax_title),
    TabletRailSpec(
        drawer = DrawerDestination.CAMERA,
        labelRes = R.string.camera,
        sectionLabelRes = R.string.drawer_section_data_entry,
    ),
    TabletRailSpec(drawer = DrawerDestination.SCANNER, labelRes = R.string.scanner),
    TabletRailSpec(
        drawer = DrawerDestination.MAINTENANCE,
        labelRes = R.string.maintenance_title,
        sectionLabelRes = R.string.drawer_section_maintenance,
    ),
    TabletRailSpec(drawer = DrawerDestination.DOCUMENTS, labelRes = R.string.drawer_documents),
    TabletRailSpec(drawer = DrawerDestination.IMPROVE, labelRes = R.string.drawer_improve),
    TabletRailSpec(drawer = DrawerDestination.ABOUT, labelRes = R.string.drawer_about),
    TabletRailSpec(isLogout = true, labelRes = R.string.drawer_logout),
)

/** Phone swipe-from-edge tools drawer. Off on tablet because the rail holds every destination. */
fun shouldEnableModalNavigationDrawer(
    showMainNavigation: Boolean,
    tabletChrome: Boolean,
): Boolean = showMainNavigation && !tabletChrome

internal fun isTabletRailItemSelected(
    currentRoute: String?,
    item: TabletRailSpec,
): Boolean {
    if (item.isLogout) return false
    item.route?.let { return isRailDestinationSelected(currentRoute, it) }
    item.drawer?.let { return isDrawerDestinationSelected(currentRoute, it) }
    return false
}

internal fun isRailDestinationSelected(currentRoute: String?, targetRoute: String): Boolean {
    if (currentRoute == null) return false
    return when (targetRoute) {
        Routes.HOME -> currentRoute == Routes.HOME ||
            currentRoute.startsWith("load_detail") ||
            currentRoute.startsWith("edit_load") ||
            currentRoute == Routes.ADD_LOAD
        Routes.STATS -> currentRoute == Routes.STATS
        Routes.PROFILE -> currentRoute == Routes.PROFILE
        else -> currentRoute == targetRoute
    }
}

internal fun isDrawerDestinationSelected(
    currentRoute: String?,
    destination: DrawerDestination,
): Boolean {
    if (currentRoute == null) return false
    return when (destination) {
        DrawerDestination.SETTINGS ->
            currentRoute == Routes.SETTINGS || currentRoute == Routes.PRIVACY_SETTINGS
        DrawerDestination.REPORTS -> currentRoute == Routes.ANALYTICS
        DrawerDestination.MAP -> currentRoute == Routes.MAP
        DrawerDestination.DOCUMENTS ->
            currentRoute == Routes.SCAN_GALLERY ||
                currentRoute == Routes.PHOTO_GALLERY ||
                currentRoute.startsWith("photo_detail")
        DrawerDestination.MAINTENANCE -> currentRoute == Routes.MAINTENANCE
        DrawerDestination.TAX_TRACKER -> currentRoute == Routes.TAX_TRACKER
        DrawerDestination.PAYCHECK ->
            currentRoute == Routes.PAYCHECK || currentRoute == Routes.ADD_PAYCHECK
        DrawerDestination.DIESEL ->
            currentRoute == Routes.DIESEL || currentRoute.startsWith("add_diesel")
        DrawerDestination.MISC_EXPENSES -> currentRoute == Routes.MISC_EXPENSES
        DrawerDestination.CAMERA ->
            currentRoute == Routes.CAMERA || currentRoute.startsWith("camera_load")
        DrawerDestination.SCANNER ->
            currentRoute == Routes.SCANNER || currentRoute.startsWith("scanner_load")
        DrawerDestination.ABOUT -> currentRoute == Routes.ABOUT
        DrawerDestination.IMPROVE -> currentRoute == Routes.IMPROVE
    }
}

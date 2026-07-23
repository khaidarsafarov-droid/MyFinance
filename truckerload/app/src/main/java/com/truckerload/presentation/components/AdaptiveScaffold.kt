package com.truckerload.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.truckerload.R
import com.truckerload.presentation.navigation.Routes
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.SoftUiColors
import com.truckerload.presentation.theme.SoftUiElevation
import com.truckerload.presentation.theme.SoftUiShapes
import com.truckerload.presentation.theme.UiDimens
import com.truckerload.presentation.utils.WindowSizeClass
import com.truckerload.presentation.utils.adaptiveVerticalPadding
import com.truckerload.presentation.utils.isFoldable
import com.truckerload.presentation.utils.isTablet
import com.truckerload.presentation.utils.rememberWindowSizeClass
import kotlinx.coroutines.launch

val LocalOpenDrawer = staticCompositionLocalOf<() -> Unit> { {} }

@Composable
fun AdaptiveScaffold(
    showMainNavigation: Boolean,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onDrawerNavigate: (DrawerDestination) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val openDrawer: () -> Unit = {
        scope.launch { drawerState.open() }
    }

    CompositionLocalProvider(LocalOpenDrawer provides openDrawer) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = showMainNavigation,
            drawerContent = {
                AppDrawerContent(
                    onNavigate = onDrawerNavigate,
                    onClose = { scope.launch { drawerState.close() } },
                )
            },
        ) {
            when {
                isTablet() && showMainNavigation -> {
                    TabletScaffold(
                        modifier = modifier,
                        currentRoute = currentRoute,
                        onNavigate = onNavigate,
                        onDrawerNavigate = onDrawerNavigate,
                        content = content,
                    )
                }
                else -> {
                    PhoneScaffold(
                        modifier = modifier,
                        showBottomBar = showMainNavigation && !isTablet(),
                        currentRoute = currentRoute,
                        onNavigate = onNavigate,
                        content = content,
                    )
                }
            }
        }
    }
}

@Composable
private fun TabletScaffold(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onDrawerNavigate: (DrawerDestination) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    Row(modifier = modifier.fillMaxSize()) {
        TruckLogNavigationRail(
            currentRoute = currentRoute,
            onNavigate = onNavigate,
            onDrawerNavigate = onDrawerNavigate,
        )
        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            thickness = 0.5.dp,
            color = BentoGlassTheme.CardBorderMuted,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(
                    horizontal = if (isFoldable()) 16.dp else 24.dp,
                    vertical = adaptiveVerticalPadding(),
                ),
        ) {
            content(PaddingValues(0.dp))
        }
    }
}

@Composable
private fun PhoneScaffold(
    showBottomBar: Boolean,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    val sizeClass = rememberWindowSizeClass()
    val extraHorizontal = when (sizeClass) {
        WindowSizeClass.MEDIUM -> 8.dp
        else -> 0.dp
    }

    Scaffold(
        modifier = modifier,
        containerColor = BentoGlassTheme.ScreenBackground,
        bottomBar = {
            if (showBottomBar) {
                SoftBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = onNavigate,
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = extraHorizontal),
        ) {
            content(PaddingValues(0.dp))
        }
    }
}

@Composable
private fun SoftBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    val pillShape = remember { RoundedCornerShape(16.dp) }

    Column(
        modifier = Modifier
            .background(SoftUiColors.Sage)
            .navigationBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SoftUiColors.Sage)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(UiDimens.NavBarHeight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BottomNavItem(
                    route = Routes.HOME,
                    icon = Icons.Outlined.LocalShipping,
                    labelRes = R.string.nav_logbook,
                    currentRoute = currentRoute,
                    onNavigate = onNavigate,
                    pillShape = pillShape,
                    modifier = Modifier.weight(1f),
                )
                BottomNavItem(
                    route = Routes.STATS,
                    icon = Icons.Outlined.Flag,
                    labelRes = R.string.nav_weekly_goal,
                    currentRoute = currentRoute,
                    onNavigate = onNavigate,
                    pillShape = pillShape,
                    modifier = Modifier.weight(1f),
                )
                BottomNavItem(
                    route = Routes.COMMUNITY,
                    icon = Icons.Outlined.Groups,
                    labelRes = R.string.nav_community,
                    currentRoute = currentRoute,
                    onNavigate = onNavigate,
                    pillShape = pillShape,
                    modifier = Modifier.weight(1f),
                )
                BottomNavItem(
                    route = Routes.PROFILE,
                    icon = Icons.Outlined.Person,
                    labelRes = R.string.nav_profile,
                    currentRoute = currentRoute,
                    onNavigate = onNavigate,
                    pillShape = pillShape,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    route: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    labelRes: Int,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    pillShape: RoundedCornerShape,
    modifier: Modifier = Modifier,
) {
    val selected = isPhoneDestinationSelected(currentRoute, route)
    val label = stringResource(labelRes)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .clip(pillShape)
            .then(
                if (selected) Modifier.background(SoftUiColors.SageHover) else Modifier,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onNavigate(route) },
            )
            .padding(horizontal = 2.dp, vertical = 6.dp),
    ) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(UiDimens.IconNavCompact),
            tint = if (selected) SoftUiColors.ForestPrimary else SoftUiColors.ForestMuted,
        )
        Text(
            text = label,
            color = if (selected) SoftUiColors.ForestPrimary else SoftUiColors.ForestMuted,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
        )
    }
}

private fun isPhoneDestinationSelected(currentRoute: String?, targetRoute: String): Boolean {
    if (currentRoute == null) return false
    return when (targetRoute) {
        Routes.HOME -> currentRoute == Routes.HOME ||
            currentRoute.startsWith("load_detail") ||
            currentRoute.startsWith("edit_load") ||
            currentRoute == Routes.ADD_LOAD
        Routes.STATS -> currentRoute == Routes.STATS
        Routes.COMMUNITY -> currentRoute == Routes.COMMUNITY ||
            currentRoute.startsWith("social_chat") ||
            currentRoute.startsWith("voice_room") ||
            currentRoute == Routes.VOICE_ROOMS ||
            currentRoute.startsWith("call") ||
            currentRoute == Routes.STATUS ||
            currentRoute == Routes.GROUPS ||
            currentRoute.startsWith("group_detail")
        Routes.PROFILE -> currentRoute == Routes.PROFILE ||
            currentRoute == Routes.PROFILE_EDIT ||
            currentRoute.startsWith("profile_peer")
        Routes.ANALYTICS -> currentRoute == Routes.ANALYTICS
        Routes.SETTINGS -> currentRoute == Routes.SETTINGS ||
            currentRoute == Routes.TAX_TRACKER
        else -> currentRoute == targetRoute
    }
}

fun navigateToMainRoute(
    route: String,
    navController: NavHostController,
) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

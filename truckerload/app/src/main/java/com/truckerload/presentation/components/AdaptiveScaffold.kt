package com.truckerload.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.truckerload.R
import com.truckerload.presentation.navigation.Routes
import com.truckerload.presentation.theme.AppElevation
import com.truckerload.presentation.theme.AppShapes
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.DarkGlassGradients
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens
import com.truckerload.presentation.utils.WindowSizeClass
import com.truckerload.presentation.utils.adaptiveVerticalPadding
import com.truckerload.presentation.utils.isFoldable
import com.truckerload.presentation.utils.isTablet
import com.truckerload.presentation.utils.rememberWindowSizeClass

@Composable
fun AdaptiveScaffold(
    showMainNavigation: Boolean,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onCameraClick: () -> Unit,
    onScannerClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    when {
        isTablet() && showMainNavigation -> {
            TabletScaffold(
                modifier = modifier,
                currentRoute = currentRoute,
                onNavigate = onNavigate,
                onCameraClick = onCameraClick,
                onScannerClick = onScannerClick,
                content = content,
            )
        }
        else -> {
            PhoneScaffold(
                modifier = modifier,
                showBottomBar = showMainNavigation && !isTablet(),
                currentRoute = currentRoute,
                onNavigate = onNavigate,
                onCameraClick = onCameraClick,
                onScannerClick = onScannerClick,
                content = content,
            )
        }
    }
}

@Composable
private fun TabletScaffold(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onCameraClick: () -> Unit,
    onScannerClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    Row(modifier = modifier.fillMaxSize()) {
        TruckLogNavigationRail(
            currentRoute = currentRoute,
            onNavigate = onNavigate,
            onCameraClick = onCameraClick,
            onScannerClick = onScannerClick,
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
    onCameraClick: () -> Unit,
    onScannerClick: () -> Unit,
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
                    onCameraClick = onCameraClick,
                onScannerClick = onScannerClick,
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
    onCameraClick: () -> Unit,
    onScannerClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val pillShape = remember { RoundedCornerShape(20.dp) }
    val leftItems = listOf(
        Triple(Routes.HOME, Icons.Outlined.Home, R.string.nav_logbook),
        Triple(Routes.STATS, Icons.Outlined.Flag, R.string.nav_weekly_goal),
        Triple(Routes.COMMUNITY, Icons.Outlined.Groups, R.string.nav_community),
    )
    val rightItems = listOf(
        Triple(Routes.SETTINGS, Icons.Outlined.Settings, R.string.nav_settings),
    )

    Column(modifier = Modifier.navigationBarsPadding()) {
        HorizontalDivider(color = BentoGlassTheme.CardBorderMuted, thickness = 0.5.dp)
        Box(
            modifier = AppElevation
                .navShadow(Modifier.fillMaxWidth())
                .background(cs.surface, AppShapes.NavTop)
                .padding(horizontal = 8.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leftItems.forEach { (route, icon, labelRes) ->
                    BottomNavItem(
                        route = route,
                        icon = icon,
                        labelRes = labelRes,
                        currentRoute = currentRoute,
                        onNavigate = onNavigate,
                        pillShape = pillShape,
                        compact = true,
                    )
                }
                Box(
                    modifier = Modifier.offset(y = (-12).dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CameraButton(onClick = onCameraClick)
                        ScannerButton(onClick = onScannerClick)
                    }
                }
                rightItems.forEach { (route, icon, labelRes) ->
                    BottomNavItem(
                        route = route,
                        icon = icon,
                        labelRes = labelRes,
                        currentRoute = currentRoute,
                        onNavigate = onNavigate,
                        pillShape = pillShape,
                        compact = true,
                    )
                }
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
    compact: Boolean = false,
) {
    val cs = MaterialTheme.colorScheme
    val tc = LocalTruckColors.current
    val selected = isPhoneDestinationSelected(currentRoute, route)
    val label = stringResource(labelRes)
    val iconSize = if (compact) UiDimens.IconNavCompact else UiDimens.IconNav
    val selectedIconSize = if (compact) UiDimens.IconNavSelectedCompact else UiDimens.IconNav
    val horizontalPad = if (compact) 8.dp else 14.dp
    val selectedBackground = tc.AccentPrimary.copy(alpha = 0.18f)
    if (selected) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(pillShape)
                .background(selectedBackground)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onNavigate(route) },
                )
                .padding(horizontal = horizontalPad, vertical = if (compact) 4.dp else 8.dp),
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(selectedIconSize),
                tint = tc.AccentPrimary,
            )
            Text(
                text = label,
                color = tc.AccentPrimary,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .sizeIn(minWidth = if (compact) 44.dp else 48.dp, minHeight = if (compact) 44.dp else 48.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onNavigate(route) },
                )
                .padding(vertical = 4.dp),
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(iconSize),
                tint = cs.onSurfaceVariant,
            )
            Text(
                text = label,
                color = cs.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
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
            currentRoute == Routes.PROFILE ||
            currentRoute == Routes.PROFILE_EDIT ||
            currentRoute.startsWith("profile_peer") ||
            currentRoute == Routes.STATUS ||
            currentRoute == Routes.GROUPS ||
            currentRoute.startsWith("group_detail")
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

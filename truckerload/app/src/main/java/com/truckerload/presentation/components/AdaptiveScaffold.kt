package com.truckerload.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.FinanceCockpitColors
import com.truckerload.presentation.theme.LocalTruckColors
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
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    when {
        isTablet() && showMainNavigation -> {
            TabletScaffold(
                modifier = modifier,
                currentRoute = currentRoute,
                onNavigate = onNavigate,
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

@Composable
private fun TabletScaffold(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    Row(modifier = modifier.fillMaxSize()) {
        TruckLogNavigationRail(
            currentRoute = currentRoute,
            onNavigate = onNavigate,
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
    val tc = LocalTruckColors.current
    val pillShape = remember { RoundedCornerShape(20.dp) }
    val items = listOf(
        Triple(Routes.HOME, Icons.Outlined.Home, R.string.nav_logbook),
        Triple(Routes.STATS, Icons.Outlined.Flag, R.string.nav_weekly_goal),
        Triple(Routes.ANALYTICS, Icons.Outlined.BarChart, R.string.nav_analytics),
        Triple(Routes.SETTINGS, Icons.Outlined.Settings, R.string.nav_settings),
    )

    Column {
        HorizontalDivider(color = BentoGlassTheme.CardBorderMuted, thickness = 0.5.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            tc.Background.copy(alpha = 0.92f),
                            tc.Background,
                        ),
                    ),
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { (route, icon, labelRes) ->
                    val selected = isPhoneDestinationSelected(currentRoute, route)
                    val label = stringResource(labelRes)
                    if (selected) {
                        Row(
                            modifier = Modifier
                                .clip(pillShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(tc.AccentPrimary, tc.AccentSecondary),
                                    ),
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onNavigate(route) },
                                )
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                icon,
                                contentDescription = label,
                                modifier = Modifier.size(22.dp),
                                tint = tc.OnAccent,
                            )
                            Text(
                                text = label,
                                color = tc.OnAccent,
                                fontWeight = FontWeight.SemiBold,
                                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onNavigate(route) },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                icon,
                                contentDescription = label,
                                modifier = Modifier.size(24.dp),
                                tint = tc.TextSecondary,
                            )
                        }
                    }
                }
            }
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

package com.truckerload.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.utils.WindowSizeClass
import com.truckerload.presentation.utils.adaptiveHorizontalPadding
import com.truckerload.presentation.utils.adaptiveVerticalPadding
import com.truckerload.presentation.utils.rememberWindowSizeClass
import com.truckerload.presentation.utils.useNavigationRail
import com.truckerload.presentation.utils.useWideTabletSidebar
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
    val tabletChrome = useNavigationRail()
    val useDrawer = shouldEnableModalNavigationDrawer(
        showMainNavigation = showMainNavigation,
        tabletChrome = tabletChrome,
    )
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val body: @Composable () -> Unit = {
        AdaptiveScaffoldBody(
            modifier = modifier,
            showMainNavigation = showMainNavigation,
            tabletChrome = tabletChrome,
            currentRoute = currentRoute,
            onNavigate = onNavigate,
            onDrawerNavigate = onDrawerNavigate,
            content = content,
        )
    }

    if (useDrawer) {
        CompositionLocalProvider(
            LocalOpenDrawer provides { scope.launch { drawerState.open() } },
        ) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = true,
                drawerContent = {
                    AppDrawerContent(
                        onNavigate = onDrawerNavigate,
                        onClose = { scope.launch { drawerState.close() } },
                    )
                },
            ) {
                body()
            }
        }
    } else {
        CompositionLocalProvider(LocalOpenDrawer provides {}) {
            body()
        }
    }
}

@Composable
private fun AdaptiveScaffoldBody(
    showMainNavigation: Boolean,
    tabletChrome: Boolean,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onDrawerNavigate: (DrawerDestination) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    val sizeClass = rememberWindowSizeClass()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BentoGlassTheme.ScreenBackground),
    ) {
        when {
            tabletChrome && showMainNavigation -> {
                TabletScaffold(
                    modifier = modifier,
                    sizeClass = sizeClass,
                    currentRoute = currentRoute,
                    onNavigate = onNavigate,
                    onDrawerNavigate = onDrawerNavigate,
                    content = content,
                )
            }
            else -> {
                PhoneScaffold(
                    modifier = modifier,
                    showBottomBar = showMainNavigation && !tabletChrome,
                    currentRoute = currentRoute,
                    onNavigate = onNavigate,
                    content = content,
                )
            }
        }
    }
}

@Composable
private fun TabletScaffold(
    sizeClass: WindowSizeClass,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onDrawerNavigate: (DrawerDestination) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    val horizontalPad = when (sizeClass) {
        WindowSizeClass.MEDIUM -> 16.dp
        WindowSizeClass.EXPANDED -> adaptiveHorizontalPadding()
        WindowSizeClass.COMPACT -> 16.dp
    }
    Row(modifier = modifier.fillMaxSize()) {
        TruckLogNavigationRail(
            currentRoute = currentRoute,
            onNavigate = onNavigate,
            onDrawerNavigate = onDrawerNavigate,
            compact = !useWideTabletSidebar(),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(BentoGlassTheme.ScreenBackground)
                .padding(
                    horizontal = horizontalPad,
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
        Box(modifier = Modifier.padding(padding)) {
            content(PaddingValues(0.dp))
        }
    }
}

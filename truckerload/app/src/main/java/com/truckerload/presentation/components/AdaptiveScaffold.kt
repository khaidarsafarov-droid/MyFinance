package com.truckerload.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
    val sizeClass = rememberWindowSizeClass()
    val openDrawer: () -> Unit = {
        scope.launch { drawerState.open() }
    }

    CompositionLocalProvider(LocalOpenDrawer provides openDrawer) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            // Edge-swipe opens the tools drawer when main chrome is visible.
            gesturesEnabled = showMainNavigation,
            drawerContent = {
                AppDrawerContent(
                    onNavigate = onDrawerNavigate,
                    onClose = { scope.launch { drawerState.close() } },
                )
            },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BentoGlassTheme.ScreenBackground),
            ) {
                when {
                    useNavigationRail() && showMainNavigation -> {
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
                            showBottomBar = showMainNavigation && !useNavigationRail(),
                            currentRoute = currentRoute,
                            onNavigate = onNavigate,
                            content = content,
                        )
                    }
                }
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

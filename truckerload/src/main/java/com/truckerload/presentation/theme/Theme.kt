package com.truckerload.presentation.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = TruckLightColors.AccentPrimary,
    onPrimary = Color.White,
    primaryContainer = TruckLightColors.SurfaceSecondary,
    onPrimaryContainer = TruckLightColors.TextPrimary,
    secondary = TruckLightColors.AccentInfo,
    onSecondary = Color.White,
    tertiary = TruckLightColors.AccentWarning,
    error = TruckLightColors.AccentExpense,
    onError = Color.White,
    background = TruckLightColors.Background,
    onBackground = TruckLightColors.TextPrimary,
    surface = TruckLightColors.CardBackground,
    onSurface = TruckLightColors.TextPrimary,
    surfaceVariant = TruckLightColors.SurfaceSecondary,
    onSurfaceVariant = TruckLightColors.TextSecondary,
    outline = TruckLightColors.Divider,
    outlineVariant = TruckLightColors.TextLabel
)

private val DarkColorScheme = darkColorScheme(
    primary = TruckDarkColors.AccentPrimary,
    onPrimary = Color.Black,
    primaryContainer = TruckDarkColors.SurfaceSecondary,
    onPrimaryContainer = TruckDarkColors.TextPrimary,
    secondary = TruckDarkColors.AccentInfo,
    onSecondary = Color.Black,
    tertiary = TruckDarkColors.AccentWarning,
    error = TruckDarkColors.AccentExpense,
    onError = Color.White,
    background = TruckDarkColors.Background,
    onBackground = TruckDarkColors.TextPrimary,
    surface = TruckDarkColors.CardBackground,
    onSurface = TruckDarkColors.TextPrimary,
    surfaceVariant = TruckDarkColors.SurfaceSecondary,
    onSurfaceVariant = TruckDarkColors.TextSecondary,
    outline = TruckDarkColors.Divider,
    outlineVariant = TruckDarkColors.TextLabel
)

@Composable
fun TruckerLoadTheme(
    content: @Composable () -> Unit
) {
    val darkTheme = false
    val colorScheme = LightColorScheme
    val truckColors = TruckLightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = truckColors.Background.toArgb()
            window.navigationBarColor = truckColors.Background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
        }
    }

    CompositionLocalProvider(LocalTruckColors provides truckColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

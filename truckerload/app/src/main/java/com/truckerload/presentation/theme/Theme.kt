package com.truckerload.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.truckerload.data.preferences.AppThemeMode

private fun lightColorScheme() = lightColorScheme(
    primary = TruckLightColors.AccentPrimary,
    onPrimary = TruckLightColors.OnAccent,
    primaryContainer = TruckLightColors.SurfaceSecondary,
    onPrimaryContainer = TruckLightColors.TextPrimary,
    secondary = TruckLightColors.AccentWarning,
    onSecondary = TruckLightColors.OnAccent,
    tertiary = TruckLightColors.AccentInfo,
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

private fun darkColorScheme() = darkColorScheme(
    primary = TruckDarkColors.AccentPrimary,
    onPrimary = TruckDarkColors.OnAccent,
    primaryContainer = TruckDarkColors.SurfaceSecondary,
    onPrimaryContainer = TruckDarkColors.TextPrimary,
    secondary = TruckDarkColors.AccentWarning,
    onSecondary = TruckDarkColors.OnAccent,
    tertiary = TruckDarkColors.AccentInfo,
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

/**
 * Truck Log Airy Soft UI theme — pastel sky, floating cards, blue–purple accents.
 */
@Composable
fun TruckerLoadTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    val truckColors = if (darkTheme) TruckDarkColors else TruckLightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            window.statusBarColor = truckColors.Background.toArgb()
            window.navigationBarColor = truckColors.Background.toArgb()
            val lightBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = lightBars
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = lightBars
        }
    }

    CompositionLocalProvider(
        LocalTruckColors provides truckColors,
        LocalAppThemeMode provides themeMode,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

val LocalAppThemeMode = compositionLocalOf { AppThemeMode.SYSTEM }

package com.truckerload.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.truckerload.data.preferences.AppThemeMode

/** Mindwell Forest static scheme (Gemini Truck Log prototype). */
private fun staticLightColorScheme(): ColorScheme = lightColorScheme(
    primary = SoftUiColors.ForestAccent,
    onPrimary = Color.White,
    primaryContainer = SoftUiColors.Sage,
    onPrimaryContainer = SoftUiColors.ForestPrimary,
    secondary = SoftUiColors.ForestMuted,
    onSecondary = Color.White,
    secondaryContainer = SoftUiColors.ShellBg,
    onSecondaryContainer = SoftUiColors.ForestPrimary,
    tertiary = SoftUiColors.ForestSoft,
    onTertiary = SoftUiColors.ForestPrimary,
    tertiaryContainer = SoftUiColors.Sage,
    onTertiaryContainer = SoftUiColors.ForestPrimary,
    error = Color(0xFFB42318),
    onError = Color.White,
    background = SoftUiColors.ContentBg,
    onBackground = SoftUiColors.ForestPrimary,
    surface = SoftUiColors.SurfaceLight,
    onSurface = SoftUiColors.ForestPrimary,
    surfaceVariant = SoftUiColors.ShellBg,
    onSurfaceVariant = SoftUiColors.ForestMuted,
    outline = SoftUiColors.CardBorder,
    outlineVariant = SoftUiColors.SageBorder,
)

private fun staticDarkColorScheme(): ColorScheme = darkColorScheme(
    primary = SoftUiColors.ForestAccent,
    onPrimary = Color.White,
    primaryContainer = SoftUiColors.SurfaceMutedDark,
    onPrimaryContainer = SoftUiColors.Sage,
    secondary = SoftUiColors.ForestSoft,
    onSecondary = SoftUiColors.BackgroundDark,
    secondaryContainer = SoftUiColors.SurfaceMutedDark,
    onSecondaryContainer = SoftUiColors.Sage,
    tertiary = SoftUiColors.ForestSoft,
    onTertiary = SoftUiColors.BackgroundDark,
    tertiaryContainer = SoftUiColors.SurfaceMutedDark,
    onTertiaryContainer = SoftUiColors.Sage,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    background = SoftUiColors.BackgroundDark,
    onBackground = SoftUiColors.TextPrimaryDark,
    surface = SoftUiColors.SurfaceDark,
    onSurface = SoftUiColors.TextPrimaryDark,
    surfaceVariant = SoftUiColors.SurfaceMutedDark,
    onSurfaceVariant = SoftUiColors.TextSecondaryDark,
    outline = Color(0x33FFFFFF),
    outlineVariant = Color(0x1AFFFFFF),
)

@Composable
private fun resolveColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
): ColorScheme {
    val context = LocalContext.current
    return when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> staticDarkColorScheme()
        else -> staticLightColorScheme()
    }
}

/**
 * App-level Compose theme that applies Material 3 colors, typography, and TruckLoad semantic colors.
 */
@Composable
fun TruckerLoadTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = resolveColorScheme(darkTheme, dynamicColor)
    val truckColors = truckPaletteFrom(colorScheme)
    val effectiveReduceMotion = rememberEffectiveReduceMotion(reduceMotion)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            MotionPreferences.reduceMotion = effectiveReduceMotion
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            val barColor = if (darkTheme) {
                SoftUiColors.BackgroundDark
            } else {
                SoftUiColors.Sage
            }
            if (Build.VERSION.SDK_INT < 35) {
                @Suppress("DEPRECATION")
                window.statusBarColor = barColor.toArgb()
                @Suppress("DEPRECATION")
                window.navigationBarColor = barColor.toArgb()
            }
            // Light icons on dark bars; dark icons on sage/light bars.
            val lightBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = lightBars
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = lightBars
        }
    }

    CompositionLocalProvider(
        LocalTruckColors provides truckColors,
        LocalAppThemeMode provides themeMode,
        LocalDynamicColorEnabled provides dynamicColor,
        LocalReduceMotion provides effectiveReduceMotion,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content,
        )
    }
}

val LocalAppThemeMode = compositionLocalOf { AppThemeMode.SYSTEM }

val LocalDynamicColorEnabled = compositionLocalOf { true }

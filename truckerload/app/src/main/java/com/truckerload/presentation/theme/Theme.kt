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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.truckerload.data.preferences.AppThemeMode

/** One UI light fallback when Material You is off or API < 31. */
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

private fun staticDarkColorScheme(): ColorScheme {
    val background = SoftUiColors.BackgroundDark
    val surface = SoftUiColors.SurfaceDark
    val surfaceVariant = SoftUiColors.SurfaceMutedDark
    return darkColorScheme(
        primary = SoftUiColors.ForestAccent,
        onPrimary = Color.White,
        primaryContainer = surfaceVariant,
        onPrimaryContainer = SoftUiColors.Sage,
        secondary = SoftUiColors.ForestSoft,
        onSecondary = background,
        secondaryContainer = surfaceVariant,
        onSecondaryContainer = SoftUiColors.Sage,
        tertiary = SoftUiColors.ForestSoft,
        onTertiary = background,
        tertiaryContainer = surfaceVariant,
        onTertiaryContainer = SoftUiColors.Sage,
        error = Color(0xFFF87171),
        onError = Color(0xFF450A0A),
        background = background,
        onBackground = SoftUiColors.TextPrimaryDark,
        surface = surface,
        onSurface = SoftUiColors.TextPrimaryDark,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = SoftUiColors.TextSecondaryDark,
        outline = Color(0x33FFFFFF),
        outlineVariant = Color(0x1AFFFFFF),
    )
}

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
 * App-level Compose theme: One UI shapes, Material You accents, and OLED-safe dark mode.
 */
@Composable
fun TruckerLoadTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    oledDark: Boolean = false,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val systemReduced = MotionPolicy.isSystemReducedMotion(LocalContext.current)
    val effectiveReduceMotion = reduceMotion || systemReduced
    val colorScheme = overlayOledIfNeeded(
        resolveColorScheme(darkTheme, dynamicColor),
        oled = oledDark && darkTheme,
    )
    val truckColors = truckPaletteFrom(colorScheme)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            if (Build.VERSION.SDK_INT < 35) {
                @Suppress("DEPRECATION")
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                @Suppress("DEPRECATION")
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
            }
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
        LocalOledDarkEnabled provides (oledDark && darkTheme),
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

val LocalOledDarkEnabled = compositionLocalOf { false }

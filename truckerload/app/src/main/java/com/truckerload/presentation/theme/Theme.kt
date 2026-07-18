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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.truckerload.data.preferences.AppThemeMode
import com.truckerload.presentation.theme.SoftUiColors

/** Samsung One UI–inspired fallback when dynamic colors are unavailable (API &lt; 31). */
private fun staticLightColorScheme(): ColorScheme = lightColorScheme(
    primary = SoftUiColors.PurpleStart,
    onPrimary = Color.White,
    primaryContainer = SoftUiColors.PurpleLight,
    onPrimaryContainer = Color(0xFF3D3280),
    secondary = Color(0xFF6B7280),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8EAF0),
    onSecondaryContainer = Color(0xFF1A1C2E),
    tertiary = Color(0xFF34D399),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF064E3B),
    error = Color(0xFFDC2626),
    onError = Color.White,
    background = SoftUiColors.BackgroundLight,
    onBackground = SoftUiColors.TextPrimaryLight,
    surface = SoftUiColors.SurfaceLight,
    onSurface = SoftUiColors.TextPrimaryLight,
    surfaceVariant = SoftUiColors.SurfaceMuted,
    onSurfaceVariant = SoftUiColors.TextSecondaryLight,
    outline = Color(0xFFE5E7EB),
    outlineVariant = Color(0xFFF0F1F6),
)

private fun staticDarkColorScheme(): ColorScheme = darkColorScheme(
    primary = SoftUiColors.PurpleMuted,
    onPrimary = Color(0xFF1A1033),
    primaryContainer = Color(0xFF3D3280),
    onPrimaryContainer = SoftUiColors.PurpleLight,
    secondary = Color(0xFF9CA3AF),
    onSecondary = Color(0xFF1F2937),
    secondaryContainer = SoftUiColors.SurfaceMutedDark,
    onSecondaryContainer = Color(0xFFE5E7EB),
    tertiary = Color(0xFF6EE7B7),
    onTertiary = Color(0xFF064E3B),
    tertiaryContainer = Color(0xFF065F46),
    onTertiaryContainer = Color(0xFFD1FAE5),
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
 * Truck Log theme — Material 3 + Material You dynamic colors (One UI style on Samsung).
 */
@Composable
fun TruckerLoadTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = resolveColorScheme(darkTheme, dynamicColor)
    val truckColors = truckPaletteFrom(colorScheme)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            if (Build.VERSION.SDK_INT < 35) {
                @Suppress("DEPRECATION")
                window.statusBarColor = colorScheme.surface.toArgb()
                @Suppress("DEPRECATION")
                window.navigationBarColor = colorScheme.surface.toArgb()
            }
            val lightBars = colorScheme.surface.luminance() > 0.5f
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = lightBars
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = lightBars
        }
    }

    CompositionLocalProvider(
        LocalTruckColors provides truckColors,
        LocalAppThemeMode provides themeMode,
        LocalDynamicColorEnabled provides dynamicColor,
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

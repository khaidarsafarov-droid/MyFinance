package com.truckerload.widget

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.toArgb
import com.truckerload.data.preferences.AppThemeMode
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.presentation.theme.forestDarkColorScheme
import com.truckerload.presentation.theme.forestLightColorScheme

/**
 * Resolved widget paint tokens. Kit light plate by default; indigo cabin in
 * dark theme; Material You when Settings → Dynamic colors is on (API 31+).
 */
data class WidgetCabinColors(
    val bg: Int,
    val text: Int,
    val muted: Int,
    val accent: Int,
    val ring: Int,
    val ringTrack: Int,
    val actionBg: Int,
    val actionLabel: Int,
    val divider: Int,
    val dayFilled: Int,
    val dayToday: Int,
    val dayOutline: Int,
    val dayFutureLetter: Int,
    val dayEmptyCaption: Int,
    val onFilled: Int,
    val dynamic: Boolean = false,
) {
    companion object {
        val ForestLight = WidgetCabinColors(
            bg = WidgetCabinPalette.BG,
            text = WidgetCabinPalette.TEXT,
            muted = WidgetCabinPalette.MUTED,
            accent = WidgetCabinPalette.ACCENT,
            ring = WidgetCabinPalette.RING,
            ringTrack = WidgetCabinPalette.RING_TRACK,
            actionBg = WidgetCabinPalette.ACTION_BG,
            actionLabel = WidgetCabinPalette.ACTION_LABEL,
            divider = WidgetCabinPalette.DIVIDER,
            dayFilled = WidgetCabinPalette.DAY_FILLED,
            dayToday = WidgetCabinPalette.DAY_TODAY,
            dayOutline = WidgetCabinPalette.DAY_OUTLINE,
            dayFutureLetter = WidgetCabinPalette.DAY_FUTURE_LETTER,
            dayEmptyCaption = WidgetCabinPalette.DAY_EMPTY_CAPTION,
            onFilled = WidgetCabinPalette.ON_FILLED,
            dynamic = false,
        )

        val ForestDark = WidgetCabinColors(
            bg = WidgetCabinPalette.Dark.BG,
            text = WidgetCabinPalette.Dark.TEXT,
            muted = WidgetCabinPalette.Dark.MUTED,
            accent = WidgetCabinPalette.Dark.ACCENT,
            ring = WidgetCabinPalette.Dark.RING,
            ringTrack = WidgetCabinPalette.Dark.RING_TRACK,
            actionBg = WidgetCabinPalette.Dark.ACTION_BG,
            actionLabel = WidgetCabinPalette.Dark.ACTION_LABEL,
            divider = WidgetCabinPalette.Dark.DIVIDER,
            dayFilled = WidgetCabinPalette.Dark.DAY_FILLED,
            dayToday = WidgetCabinPalette.Dark.DAY_TODAY,
            dayOutline = WidgetCabinPalette.Dark.DAY_OUTLINE,
            dayFutureLetter = WidgetCabinPalette.Dark.DAY_FUTURE_LETTER,
            dayEmptyCaption = WidgetCabinPalette.Dark.DAY_EMPTY_CAPTION,
            onFilled = WidgetCabinPalette.Dark.ON_FILLED,
            dynamic = false,
        )

        /** Default plate used by Glance previews — light kit. */
        val Forest = ForestLight

        fun fromScheme(scheme: ColorScheme, dynamic: Boolean = true) = WidgetCabinColors(
            bg = scheme.surface.toArgb(),
            text = scheme.onSurface.toArgb(),
            muted = scheme.onSurfaceVariant.toArgb(),
            accent = scheme.primary.toArgb(),
            ring = scheme.primary.toArgb(),
            ringTrack = scheme.surfaceVariant.toArgb(),
            actionBg = scheme.primary.toArgb(),
            actionLabel = scheme.onPrimary.toArgb(),
            divider = scheme.outlineVariant.toArgb(),
            dayFilled = scheme.primary.toArgb(),
            dayToday = scheme.tertiaryContainer.toArgb(),
            dayOutline = scheme.outline.toArgb(),
            dayFutureLetter = scheme.onSurfaceVariant.toArgb(),
            dayEmptyCaption = scheme.outline.toArgb(),
            onFilled = scheme.onPrimary.toArgb(),
            dynamic = dynamic,
        )

        fun isDarkTheme(context: Context, mode: AppThemeMode): Boolean = when (mode) {
            AppThemeMode.LIGHT -> false
            AppThemeMode.DARK -> true
            AppThemeMode.SYSTEM -> {
                val night = context.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK
                night == Configuration.UI_MODE_NIGHT_YES
            }
        }

        suspend fun resolve(context: Context): Pair<ColorScheme, WidgetCabinColors> {
            return runCatching {
                val app = context.applicationContext
                val settings = SettingsDataStore(app)
                val wantDynamic = settings.getDynamicColorOnce() &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                val dark = isDarkTheme(app, settings.getThemeModeOnce())
                val scheme = when {
                    wantDynamic && dark -> dynamicDarkColorScheme(app)
                    wantDynamic && !dark -> dynamicLightColorScheme(app)
                    dark -> forestDarkColorScheme()
                    else -> forestLightColorScheme()
                }
                val colors = when {
                    wantDynamic -> fromScheme(scheme, dynamic = true)
                    dark -> ForestDark
                    else -> ForestLight
                }
                scheme to colors
            }.getOrElse { forestLightColorScheme() to ForestLight }
        }
    }
}

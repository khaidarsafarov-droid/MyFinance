package com.truckerload.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.truckerload.presentation.theme.BrandPalette
import com.truckerload.presentation.theme.BrandPalettes
import com.truckerload.presentation.theme.SemanticColors
import com.truckerload.presentation.theme.forestDarkColorScheme
import com.truckerload.presentation.theme.forestLightColorScheme
import com.truckerload.data.preferences.AppThemeMode
import com.truckerload.data.preferences.SettingsDataStore

/**
 * Widget paint from [BrandPalettes]. Dynamic color does not recolor the plate.
 */
data class WidgetCabinColors(
    val bg: Int,
    val text: Int,
    val muted: Int,
    val accent: Int,
    val brand: Int,
    val ring: Int,
    val ringTrack: Int,
    val progressStart: Int,
    val progressEnd: Int,
    val progressLabel: Int,
    val actionBg: Int,
    val actionChip: Int,
    val actionStroke: Int,
    val divider: Int,
    val dayFilled: Int,
    val dayToday: Int,
    val dayOutline: Int,
    val dayFutureLetter: Int,
    val dayEmptyCaption: Int,
    val onFilled: Int,
    val dynamic: Boolean = false,
    val headerStart: Int = progressStart,
    val headerEnd: Int = progressEnd,
    val rpmFg: Int = 0xFF3D6A55.toInt(),
    val rpmBg: Int = 0xFFE5EDE8.toInt(),
    val onHeader: Int = Color.White.toArgb(),
) {
    companion object {
        fun fromBrand(brand: BrandPalette, dark: Boolean = false): WidgetCabinColors {
            val status = if (dark) SemanticColors.Dark else SemanticColors.Light
            return WidgetCabinColors(
                bg = brand.card.toArgb(),
                text = brand.textPrimary.toArgb(),
                muted = brand.textSecondary.toArgb(),
                accent = brand.primary.toArgb(),
                brand = brand.primary.toArgb(),
                ring = brand.primary.toArgb(),
                ringTrack = brand.container.toArgb(),
                progressStart = brand.heroStart.toArgb(),
                progressEnd = brand.heroEnd.toArgb(),
                progressLabel = brand.onPrimary.toArgb(),
                actionBg = brand.primary.toArgb(),
                actionChip = brand.container.toArgb(),
                actionStroke = brand.cardBorder.toArgb(),
                divider = brand.cardBorder.toArgb(),
                dayFilled = brand.container.toArgb(),
                dayToday = brand.primary.toArgb(),
                dayOutline = brand.container.toArgb(),
                dayFutureLetter = brand.textSecondary.toArgb(),
                dayEmptyCaption = brand.textTertiary.toArgb(),
                onFilled = brand.onPrimary.toArgb(),
                dynamic = false,
                headerStart = brand.heroStart.toArgb(),
                headerEnd = brand.heroEnd.toArgb(),
                rpmFg = status.success.toArgb(),
                rpmBg = status.successContainer.toArgb(),
                onHeader = Color.White.toArgb(),
            )
        }

        val ForestLight = fromBrand(BrandPalettes.Light)

        val ForestDark = fromBrand(BrandPalettes.Dark, dark = true)

        /** Default plate used by Glance previews — brand light, not dynamic color. */
        val Forest = ForestLight

        fun fromScheme(scheme: ColorScheme, dynamic: Boolean = true) = WidgetCabinColors(
            bg = scheme.surface.toArgb(),
            text = scheme.onSurface.toArgb(),
            muted = scheme.onSurfaceVariant.toArgb(),
            accent = scheme.primary.toArgb(),
            brand = scheme.primary.toArgb(),
            ring = scheme.primary.toArgb(),
            ringTrack = scheme.surfaceVariant.toArgb(),
            progressStart = scheme.primaryContainer.toArgb(),
            progressEnd = scheme.tertiary.toArgb(),
            progressLabel = scheme.onPrimaryContainer.toArgb(),
            actionBg = scheme.primaryContainer.toArgb(),
            actionChip = scheme.secondaryContainer.toArgb(),
            actionStroke = scheme.outline.toArgb(),
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

        /**
         * Brand palette only. Wallpaper dynamic color does not recolor the widget.
         */
        suspend fun resolve(context: Context): Pair<ColorScheme, WidgetCabinColors> {
            return runCatching {
                val app = context.applicationContext
                val settings = SettingsDataStore(app)
                val dark = isDarkTheme(app, settings.getThemeModeOnce())
                if (dark) {
                    forestDarkColorScheme() to ForestDark
                } else {
                    forestLightColorScheme() to ForestLight
                }
            }.getOrElse { forestLightColorScheme() to ForestLight }
        }
    }
}

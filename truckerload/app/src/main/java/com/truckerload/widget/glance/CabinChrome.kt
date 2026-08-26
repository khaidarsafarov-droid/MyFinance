package com.truckerload.widget.glance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.unit.ColorProvider
import com.truckerload.widget.WidgetCabinColors
import com.truckerload.widget.WidgetCabinPalette

internal val LocalCabinColors = staticCompositionLocalOf { WidgetCabinColors.Forest }

@Composable
internal fun cabinColor(argb: Int): ColorProvider = ColorProvider(Color(argb))

/** Paint from resolved tokens so the plate follows the app theme, not only system night. */
internal fun GlanceModifier.cabinPlate(colors: WidgetCabinColors): GlanceModifier =
    background(ColorProvider(Color(colors.bg)))
        .cornerRadius(WidgetCabinPalette.CORNER_DP.dp)

internal fun GlanceModifier.cabinActionFill(colors: WidgetCabinColors): GlanceModifier =
    background(ColorProvider(Color(colors.actionBg)))
        .cornerRadius(50.dp)

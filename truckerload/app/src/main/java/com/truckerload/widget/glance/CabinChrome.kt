package com.truckerload.widget.glance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.unit.ColorProvider
import com.truckerload.R
import com.truckerload.widget.WidgetCabinColors
import com.truckerload.widget.WidgetCabinPalette

internal val LocalCabinColors = staticCompositionLocalOf { WidgetCabinColors.Forest }

@Composable
internal fun cabinColor(argb: Int): ColorProvider = ColorProvider(Color(argb))

internal fun GlanceModifier.cabinPlate(colors: WidgetCabinColors): GlanceModifier =
    if (colors.dynamic) {
        background(ColorProvider(Color(colors.bg)))
            .cornerRadius(WidgetCabinPalette.CORNER_DP.dp)
    } else {
        background(ImageProvider(R.drawable.widget_cabin_plate))
    }

internal fun GlanceModifier.cabinActionFill(colors: WidgetCabinColors): GlanceModifier =
    if (colors.dynamic) {
        background(ColorProvider(Color(colors.actionBg)))
            .cornerRadius(50.dp)
    } else {
        background(ImageProvider(R.drawable.widget_cabin_action_btn))
    }

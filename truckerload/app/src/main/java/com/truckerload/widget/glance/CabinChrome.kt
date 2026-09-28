package com.truckerload.widget.glance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.padding
import androidx.glance.unit.ColorProvider
import com.truckerload.widget.WidgetCabinColors
import com.truckerload.widget.WidgetCabinPalette
import com.truckerload.widget.WidgetSizeMode

internal val LocalCabinColors = staticCompositionLocalOf { WidgetCabinColors.Forest }

internal val LocalWidgetSizeMode = staticCompositionLocalOf { WidgetSizeMode.AUTO }

@Composable
internal fun cabinColor(argb: Int): ColorProvider = ColorProvider(Color(argb))

/** Paint from resolved tokens so the plate follows the app theme, not only system night. */
internal fun GlanceModifier.cabinPlate(colors: WidgetCabinColors): GlanceModifier =
    background(ColorProvider(Color(colors.bg)))
        .cornerRadius(WidgetCabinPalette.CORNER_DP.dp)

/** Soft circular chip — enough to say “tap me”, not a brand-purple blob. */
internal fun GlanceModifier.cabinActionChip(colors: WidgetCabinColors): GlanceModifier =
    background(ColorProvider(Color(colors.actionChip)))
        .cornerRadius(999.dp)

internal fun GlanceModifier.cabinActionDivider(colors: WidgetCabinColors): GlanceModifier =
    background(ColorProvider(Color(colors.divider)))

/**
 * One weighted spacer. Glance columns keep only the first 10 children, so the
 * minimum gap lives in [sectionBelow] on the block above this spacer.
 */
@Composable
internal fun ColumnScope.cabinSectionGap() {
    Spacer(modifier = GlanceModifier.defaultWeight())
}

/** Floor between sections. Extra cell height is shared by [cabinSectionGap]. */
internal fun GlanceModifier.sectionBelow(gap: Dp): GlanceModifier = this.padding(bottom = gap)

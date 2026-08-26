package com.truckerload.widget

/** Visual kind for a Sun–Sat chip on the forest cabin widget. */
object WidgetDayChipStyle {
    enum class Kind { FILLED, TODAY, OUTLINE }

    fun kind(
        hasLoad: Boolean,
        isToday: Boolean,
        selected: Boolean,
    ): Kind = when {
        isToday -> Kind.TODAY
        selected || hasLoad -> Kind.FILLED
        else -> Kind.OUTLINE
    }

    fun fillColor(
        kind: Kind,
        colors: WidgetCabinColors = WidgetCabinColors.Forest,
    ): Int? = when (kind) {
        Kind.FILLED -> colors.dayFilled
        Kind.TODAY -> colors.dayToday
        Kind.OUTLINE -> null
    }

    fun letterColor(
        kind: Kind,
        colors: WidgetCabinColors = WidgetCabinColors.Forest,
    ): Int = when (kind) {
        Kind.OUTLINE -> colors.dayFutureLetter
        Kind.FILLED -> colors.onFilled
        // Today uses a light lavender plate — body text, not white-on-primary.
        Kind.TODAY -> colors.text
    }
}

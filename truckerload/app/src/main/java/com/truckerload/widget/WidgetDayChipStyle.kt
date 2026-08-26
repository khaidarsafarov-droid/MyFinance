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

    fun fillColor(kind: Kind): Int? = when (kind) {
        Kind.FILLED -> WidgetCabinPalette.DAY_FILLED
        Kind.TODAY -> WidgetCabinPalette.DAY_TODAY
        Kind.OUTLINE -> null
    }

    fun letterColor(kind: Kind): Int = when (kind) {
        Kind.OUTLINE -> WidgetCabinPalette.DAY_FUTURE_LETTER
        Kind.FILLED, Kind.TODAY -> WidgetCabinPalette.ON_FILLED
    }
}

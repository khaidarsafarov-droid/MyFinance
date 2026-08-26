package com.truckerload.widget

/**
 * Home-screen widget tokens — dark forest cabin, aligned with [com.truckerload.presentation.theme.SoftUiColors]
 * and the TruckoRig widget mockup. Accent is reserved for RPM and action icons.
 */
object WidgetCabinPalette {
    /** Plate fill — mockup `#12251c`, same graphite-green family as app `BackgroundDark`. */
    const val BG = 0xFF12251C.toInt()
    const val TEXT = 0xFFEAFAF0.toInt()
    const val MUTED = 0xFF8FAE9C.toInt()
    /** Semantic success on dark (`#5EE0A0`) nudged to the mockup. */
    const val ACCENT = 0xFF5EDB97.toInt()
    const val RING = 0xFF2F6B4A.toInt()
    const val RING_TRACK = 0xFF1C3527.toInt()
    const val ACTION_BG = 0xFF1C3527.toInt()
    const val ACTION_LABEL = 0xFFC4DCCD.toInt()
    const val DIVIDER = 0xFF233C2C.toInt()
    const val DAY_FILLED = 0xFF2F6B4A.toInt()
    const val DAY_TODAY = 0xFF3A3F38.toInt()
    const val DAY_OUTLINE = 0xFF3D4A41.toInt()
    const val DAY_FUTURE_LETTER = 0xFF5C7266.toInt()
    const val DAY_EMPTY_CAPTION = 0xFF3D4A41.toInt()
    const val ON_FILLED = TEXT

    /** Mockup ring is 6px on a 92px circle. */
    const val RING_STROKE_RATIO = 6f / 92f

    const val CORNER_DP = 20
}

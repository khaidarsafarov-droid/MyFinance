package com.truckerload.widget

/**
 * Home-screen widget tokens — forest cabin, aligned with
 * [com.truckerload.presentation.theme.SoftUiColors] but lifted a step so the
 * plate is readable on a home screen (not near-black).
 * Accent is reserved for RPM and action icons.
 */
object WidgetCabinPalette {
    /** Plate fill — brighter forest than the old `#12251C` cabin. */
    const val BG = 0xFF1E3D2E.toInt()
    const val TEXT = 0xFFEAFAF0.toInt()
    const val MUTED = 0xFF9CBEAC.toInt()
    const val ACCENT = 0xFF5EDB97.toInt()
    const val RING = 0xFF3A7D56.toInt()
    const val RING_TRACK = 0xFF2C4F40.toInt()
    const val ACTION_BG = 0xFF2C4F40.toInt()
    const val ACTION_LABEL = 0xFFC4DCCD.toInt()
    const val DIVIDER = 0xFF3A5A4A.toInt()
    const val DAY_FILLED = 0xFF3A7D56.toInt()
    const val DAY_TODAY = 0xFF44564C.toInt()
    const val DAY_OUTLINE = 0xFF4E6358.toInt()
    const val DAY_FUTURE_LETTER = 0xFF6B8578.toInt()
    const val DAY_EMPTY_CAPTION = 0xFF4E6358.toInt()
    const val ON_FILLED = TEXT

    /** Mockup ring is 6px on a 92px circle. */
    const val RING_STROKE_RATIO = 6f / 92f

    const val CORNER_DP = 20
}

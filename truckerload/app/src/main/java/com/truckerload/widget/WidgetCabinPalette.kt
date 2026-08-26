package com.truckerload.widget

/**
 * Home-screen widget tokens — Daily Task Tracker kit, aligned with
 * [com.truckerload.presentation.theme.SoftUiColors]. Light plate by default
 * so body text stays near-black; dark indigo tokens for night / Dark theme.
 */
object WidgetCabinPalette {
    /** Light kit plate — brighter than the old `#12251C` cabin. */
    const val BG = 0xFFF8F9FE.toInt()
    const val TEXT = 0xFF1A1A1A.toInt()
    const val MUTED = 0xFF5C5C5C.toInt()
    const val ACCENT = 0xFF5B54E6.toInt()
    const val RING = 0xFF5B54E6.toInt()
    const val RING_TRACK = 0xFFEEEDFF.toInt()
    /** Progress bar gradient end — kit mint / cyan. */
    const val PROGRESS_END = 0xFF48C9B0.toInt()
    /** Brand title on mockup plate. */
    const val BRAND = 0xFF5B54E6.toInt()
    const val ACTION_BG = 0xFF5B54E6.toInt()
    const val ACTION_STROKE = 0xFF4844C4.toInt()
    const val ACTION_LABEL = 0xFF5C5C5C.toInt()
    const val DIVIDER = 0xFFE8E7F4.toInt()
    const val DAY_FILLED = 0xFF5B54E6.toInt()
    const val DAY_TODAY = 0xFFEEEDFF.toInt()
    const val DAY_OUTLINE = 0xFFD4D2E8.toInt()
    const val DAY_FUTURE_LETTER = 0xFF5C5C5C.toInt()
    const val DAY_EMPTY_CAPTION = 0xFF5C5C5C.toInt()
    const val ON_FILLED = 0xFFFFFFFF.toInt()

    object Dark {
        /** Navy cabin plate from home-screen mockup. */
        const val BG = 0xFF1E2238.toInt()
        const val TEXT = 0xFFF4F3FA.toInt()
        const val MUTED = 0xFF9BA3C7.toInt()
        const val ACCENT = 0xFFB4AFFF.toInt()
        const val BRAND = 0xFFA29BFE.toInt()
        const val RING = 0xFF5B54E6.toInt()
        const val RING_TRACK = 0xFF2A3050.toInt()
        const val PROGRESS_END = 0xFF48C9B0.toInt()
        const val ACTION_BG = 0xFF5B54E6.toInt()
        const val ACTION_STROKE = 0xFF4844C4.toInt()
        const val ACTION_LABEL = 0xFFC8C6D8.toInt()
        const val DIVIDER = 0xFF353148.toInt()
        const val DAY_FILLED = 0xFF5B54E6.toInt()
        const val DAY_TODAY = 0xFF2A2640.toInt()
        const val DAY_OUTLINE = 0xFF4A4660.toInt()
        const val DAY_FUTURE_LETTER = 0xFFC8C6D8.toInt()
        const val DAY_EMPTY_CAPTION = 0xFF4A4660.toInt()
        const val ON_FILLED = 0xFFFFFFFF.toInt()
    }

    /** Mockup ring is 6px on a 92px circle. */
    const val RING_STROKE_RATIO = 6f / 92f

    const val CORNER_DP = 20
}

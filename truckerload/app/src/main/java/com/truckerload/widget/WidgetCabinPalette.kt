package com.truckerload.widget

/**
 * Home-screen widget tokens — SoftUI purple kit (#5B54E6), aligned with
 * [com.truckerload.presentation.theme.SoftUiColors].
 *
 * Light: soft lavender plate + near-black body text (readable at a glance).
 * Dark: indigo cabin + solid brand-purple action buttons that pop.
 */
object WidgetCabinPalette {
    /** SoftUI lavender plate — branded, not flat white. */
    const val BG = 0xFFEEEDFF.toInt()
    /** Near-black for WCAG AA on the lavender plate. */
    const val TEXT = 0xFF12121A.toInt()
    /** Darker muted gray-violet — readable secondary labels. */
    const val MUTED = 0xFF4A4868.toInt()
    /** SoftUI brand purple. */
    const val ACCENT = 0xFF5B54E6.toInt()
    const val RING = 0xFF5B54E6.toInt()
    const val RING_TRACK = 0xFFDCD9FF.toInt()
    /** Progress bar gradient start — bright lavender. */
    const val PROGRESS_START = 0xFF9B93F5.toInt()
    /** Progress bar gradient end — SoftUI mint. */
    const val PROGRESS_END = 0xFF48C9B0.toInt()
    const val BRAND = 0xFF5B54E6.toInt()
    /** Solid brand fill so camera / scanner / diesel buttons catch the eye. */
    const val ACTION_BG = 0xFF5B54E6.toInt()
    const val ACTION_STROKE = 0xFF3F3AC0.toInt()
    /** Brand-tinted captions under action buttons. */
    const val ACTION_LABEL = 0xFF3F3AC0.toInt()
    const val DIVIDER = 0xFFD4D2F0.toInt()
    const val DAY_FILLED = 0xFF5B54E6.toInt()
    const val DAY_TODAY = 0xFFDCD9FF.toInt()
    const val DAY_OUTLINE = 0xFFC4C1E8.toInt()
    const val DAY_FUTURE_LETTER = 0xFF4A4868.toInt()
    const val DAY_EMPTY_CAPTION = 0xFF4A4868.toInt()
    const val ON_FILLED = 0xFFFFFFFF.toInt()
    const val PROGRESS_LABEL = 0xFF1A1A2E.toInt()

    object Dark {
        /** Deep indigo SoftUI cabin. */
        const val BG = 0xFF1A1830.toInt()
        const val TEXT = 0xFFF4F3FA.toInt()
        const val MUTED = 0xFFB8B5D4.toInt()
        const val ACCENT = 0xFFB4AFFF.toInt()
        const val BRAND = 0xFFC4BFFF.toInt()
        const val RING = 0xFF5B54E6.toInt()
        const val RING_TRACK = 0xFF2A2748.toInt()
        const val PROGRESS_END = 0xFF48C9B0.toInt()
        const val PROGRESS_START = 0xFFC4B5FD.toInt()
        /** Keep solid brand purple so actions stay visible on dark cabin. */
        const val ACTION_BG = 0xFF5B54E6.toInt()
        const val ACTION_STROKE = 0xFF7B75F0.toInt()
        const val ACTION_LABEL = 0xFFE0DEFF.toInt()
        const val DIVIDER = 0xFF35314A.toInt()
        const val DAY_FILLED = 0xFF5B54E6.toInt()
        const val DAY_TODAY = 0xFF2A2640.toInt()
        const val DAY_OUTLINE = 0xFF4A4660.toInt()
        const val DAY_FUTURE_LETTER = 0xFFC8C6D8.toInt()
        const val DAY_EMPTY_CAPTION = 0xFF6A6680.toInt()
        const val ON_FILLED = 0xFFFFFFFF.toInt()
        const val PROGRESS_LABEL = 0xFFF4F3FA.toInt()
    }

    /** Mockup ring is 6px on a 92px circle. */
    const val RING_STROKE_RATIO = 6f / 92f

    const val CORNER_DP = 22
}

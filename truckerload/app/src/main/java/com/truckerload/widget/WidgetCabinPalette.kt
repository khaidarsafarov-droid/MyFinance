package com.truckerload.widget

/**
 * Configure-preview and legacy bitmap tokens for the home-screen widget.
 * Midnight & Gold, aligned with [com.truckerload.presentation.theme.BrandPalettes].
 * Live Glance widgets paint through [WidgetCabinColors] and do not read these constants.
 *
 * Light: warm paper plate, ink text, indigo accent, gold progress end.
 * Dark: midnight cabin, gold accent on the ring and filled days.
 */
object WidgetCabinPalette {
    /** Warm paper plate. */
    const val BG = 0xFFFDFCFA.toInt()
    /** Ink for WCAG AA on the paper plate. */
    const val TEXT = 0xFF16182B.toInt()
    /** Muted secondary labels. */
    const val MUTED = 0xFF585A6E.toInt()
    /** Midnight indigo accent. */
    const val ACCENT = 0xFF3F3D9E.toInt()
    const val RING = 0xFF3F3D9E.toInt()
    const val RING_TRACK = 0xFFEAE9F4.toInt()
    /** Progress bar gradient start — deep midnight. */
    const val PROGRESS_START = 0xFF1E2150.toInt()
    /** Progress bar gradient end — gold. */
    const val PROGRESS_END = 0xFFC8A867.toInt()
    const val BRAND = 0xFF3F3D9E.toInt()
    /** Solid accent fill leftover for older XML chrome. */
    const val ACTION_BG = 0xFF3F3D9E.toInt()
    const val ACTION_STROKE = 0xFF2F2D7E.toInt()
    /** Soft chip so outline icons still read as tap targets. */
    const val ACTION_CHIP = 0xFFEAE9F4.toInt()
    /** Mockup action tints: QR dusty rose, camera gold, diesel terracotta. */
    const val ACTION_SCANNER = 0xFFC98BA4.toInt()
    const val ACTION_CAMERA = 0xFFD4B44A.toInt()
    const val ACTION_DIESEL = 0xFFD4783A.toInt()
    const val DIVIDER = 0xFFE6E2D8.toInt()
    const val DAY_FILLED = 0xFF3F3D9E.toInt()
    const val DAY_TODAY = 0xFFEAE9F4.toInt()
    const val DAY_OUTLINE = 0xFFC9C6DC.toInt()
    const val DAY_FUTURE_LETTER = 0xFF585A6E.toInt()
    const val DAY_EMPTY_CAPTION = 0xFF585A6E.toInt()
    const val ON_FILLED = 0xFFFFFFFF.toInt()
    const val PROGRESS_LABEL = 0xFF16182B.toInt()

    object Dark {
        /** Midnight cabin. */
        const val BG = 0xFF161929.toInt()
        const val TEXT = 0xFFEEEBE3.toInt()
        const val MUTED = 0xFFA7A6B4.toInt()
        const val ACCENT = 0xFFD6BC85.toInt()
        const val BRAND = 0xFFD6BC85.toInt()
        const val RING = 0xFFD6BC85.toInt()
        const val RING_TRACK = 0xFF262A4F.toInt()
        const val PROGRESS_END = 0xFFE4D2A1.toInt()
        const val PROGRESS_START = 0xFF1E2250.toInt()
        /** Gold fill so actions stay visible on the dark cabin. */
        const val ACTION_BG = 0xFFD6BC85.toInt()
        const val ACTION_STROKE = 0xFF34384F.toInt()
        const val ACTION_CHIP = 0xFF262A4F.toInt()
        const val DIVIDER = 0xFF262A3E.toInt()
        const val DAY_FILLED = 0xFFD6BC85.toInt()
        const val DAY_TODAY = 0xFF262A4F.toInt()
        const val DAY_OUTLINE = 0xFF34384F.toInt()
        const val DAY_FUTURE_LETTER = 0xFFA7A6B4.toInt()
        const val DAY_EMPTY_CAPTION = 0xFF8E8D9C.toInt()
        const val ON_FILLED = 0xFF1D1708.toInt()
        const val PROGRESS_LABEL = 0xFFEEEBE3.toInt()
    }

    /** Mockup ring is 6px on a 92px circle. */
    const val RING_STROKE_RATIO = 6f / 92f

    const val CORNER_DP = 22
}

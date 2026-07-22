package com.truckerload.presentation.components

import androidx.compose.ui.graphics.Color
import com.truckerload.presentation.theme.TruckColorPalette
import org.junit.Assert.assertEquals
import org.junit.Test

class RpmColorConfigTest {

    private val palette = TruckColorPalette(
        Background = Color(0xFF000001),
        BackgroundBottom = Color(0xFF000002),
        CardBackground = Color(0xFF000003),
        SurfaceSecondary = Color(0xFF000004),
        Divider = Color(0xFF000005),
        AccentPrimary = Color(0xFF000006),
        AccentSecondary = Color(0xFF000007),
        AccentExpense = Color(0xFFFF0000),
        AccentInfo = Color(0xFF000008),
        AccentWarning = Color(0xFFFFFF00),
        AccentProfit = Color(0xFF00FF00),
        TextPrimary = Color(0xFF000009),
        TextSecondary = Color(0xFF888888),
        TextLabel = Color(0xFF00000A),
        TextNumbers = Color(0xFF00000B),
        TextGold = Color(0xFF00000C),
        OnAccent = Color(0xFF00000D),
        GlassBorder = Color(0xFF00000E),
        ProgressTrack = Color(0xFF00000F),
        HeroBackground = Color(0xFF000010),
        CreamBackground = Color(0xFF000011),
    )

    @Test
    fun nullRpm_usesSecondaryText() {
        assertEquals(
            palette.TextSecondary,
            getRpmColor(null, palette, minThreshold = 2.0, targetThreshold = 2.5),
        )
    }

    @Test
    fun belowMin_expense() {
        assertEquals(
            palette.AccentExpense,
            getRpmColor(1.5, palette, minThreshold = 2.0, targetThreshold = 2.5),
        )
    }

    @Test
    fun betweenMinAndTarget_warning() {
        assertEquals(
            palette.AccentWarning,
            getRpmColor(2.2, palette, minThreshold = 2.0, targetThreshold = 2.5),
        )
    }

    @Test
    fun atOrAboveTarget_profit() {
        assertEquals(
            palette.AccentProfit,
            getRpmColor(2.5, palette, minThreshold = 2.0, targetThreshold = 2.5),
        )
        assertEquals(
            palette.AccentProfit,
            getRpmColor(3.0, palette, minThreshold = 2.0, targetThreshold = 2.5),
        )
    }
}

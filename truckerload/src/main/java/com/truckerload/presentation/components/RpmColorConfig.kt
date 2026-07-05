package com.truckerload.presentation.components

import androidx.compose.ui.graphics.Color
import com.truckerload.presentation.theme.TruckColorPalette

/** Цвет RPM по значению и порогам. null = "—" (нейтральный серый). */
fun getRpmColor(
    rpm: Double?,
    tc: TruckColorPalette,
    minThreshold: Double,
    targetThreshold: Double
): Color {
    return when {
        rpm == null -> tc.TextSecondary
        rpm < minThreshold -> tc.AccentExpense
        rpm < targetThreshold -> tc.AccentWarning
        else -> tc.AccentProfit
    }
}

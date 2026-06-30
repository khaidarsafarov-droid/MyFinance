package com.truckerload.presentation.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.truckerload.R

/** Premium serif headlines — Playfair-style (system serif fallback). */
val PlayfairFontFamily = FontFamily.Serif

/** Watch-style monospace figures. */
val SpaceMonoFontFamily = FontFamily(
    Font(R.font.space_mono_bold, FontWeight.Bold),
    Font(R.font.space_mono_bold, FontWeight.Medium),
)

/** Body copy — clean sans (system). */
val InterFontFamily = FontFamily.SansSerif

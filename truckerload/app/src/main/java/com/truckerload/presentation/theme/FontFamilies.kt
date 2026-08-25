package com.truckerload.presentation.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.truckerload.R

/** Brand sans — DM Sans (Mindwell Forest / TruckoRig canvas). */
val DmSansFontFamily = FontFamily(
    Font(R.font.dm_sans_regular, FontWeight.Normal),
    Font(R.font.dm_sans_medium, FontWeight.Medium),
    Font(R.font.dm_sans_semibold, FontWeight.SemiBold),
    Font(R.font.dm_sans_bold, FontWeight.Bold),
    Font(R.font.dm_sans_bold, FontWeight.ExtraBold),
)

/** @deprecated Prefer [DmSansFontFamily]. Kept as alias for older call sites. */
@Deprecated("Use DmSansFontFamily", ReplaceWith("DmSansFontFamily"))
val InterFontFamily = DmSansFontFamily

/** Watch-style monospace figures (trip IDs, etc.). */
val SpaceMonoFontFamily = FontFamily(
    Font(R.font.space_mono_bold, FontWeight.Bold),
    Font(R.font.space_mono_bold, FontWeight.Medium),
)

/** Optional serif accent — system serif fallback. */
val PlayfairFontFamily = FontFamily.Serif

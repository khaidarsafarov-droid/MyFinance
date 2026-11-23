package com.truckerload.presentation.components

import androidx.compose.runtime.staticCompositionLocalOf

/** Load ids just imported from the share sheet. Cards with these ids draw a gold border. */
val LocalShareHighlightLoadIds = staticCompositionLocalOf { emptySet<String>() }

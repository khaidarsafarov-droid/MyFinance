package com.truckerload.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha

/**
 * Static skeleton opacity — avoids infinite Compose transitions that keep the
 * main thread busy during cold start / first paint.
 */
@Composable
fun Modifier.shimmerPulse(): Modifier = alpha(0.42f)

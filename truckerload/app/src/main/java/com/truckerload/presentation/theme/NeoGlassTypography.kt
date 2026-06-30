package com.truckerload.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Neo-Glass text presets — use with [LocalTruckColors]. */
object NeoGlassTypography {

  @Composable
  fun ScreenTitle(
      text: String,
      modifier: Modifier = Modifier,
  ) {
      val tc = LocalTruckColors.current
      androidx.compose.material3.Text(
          text = text,
          modifier = modifier,
          style = MaterialTheme.typography.headlineMedium.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp,
              color = tc.TextPrimary,
          ),
      )
  }

  @Composable
  fun MetricValue(
      text: String,
      modifier: Modifier = Modifier,
      glow: Boolean = true,
  ) {
      val tc = LocalTruckColors.current
      val style = MaterialTheme.typography.displayLarge.copy(
          fontWeight = FontWeight.ExtraBold,
          color = tc.AccentPrimary,
          shadow = if (glow) {
              Shadow(color = tc.AccentPrimary.copy(alpha = 0.3f), blurRadius = 20f)
          } else {
              null
          },
      )
      androidx.compose.material3.Text(text = text, modifier = modifier, style = style)
  }

  @Composable
  fun MetricValueMedium(
      text: String,
      modifier: Modifier = Modifier,
      color: androidx.compose.ui.graphics.Color? = null,
  ) {
      val tc = LocalTruckColors.current
      androidx.compose.material3.Text(
          text = text,
          modifier = modifier,
          style = MaterialTheme.typography.displaySmall.copy(
              fontWeight = FontWeight.Bold,
              color = color ?: tc.AccentProfit,
          ),
      )
  }

  @Composable
  fun Label(
      text: String,
      modifier: Modifier = Modifier,
  ) {
      val tc = LocalTruckColors.current
      androidx.compose.material3.Text(
          text = text,
          modifier = modifier,
          style = MaterialTheme.typography.bodyMedium.copy(
              fontWeight = FontWeight.Medium,
              color = tc.TextSecondary,
          ),
      )
  }

  @Composable
  fun Hint(
      text: String,
      modifier: Modifier = Modifier,
  ) {
      val tc = LocalTruckColors.current
      androidx.compose.material3.Text(
          text = text,
          modifier = modifier,
          style = MaterialTheme.typography.bodySmall.copy(
              fontWeight = FontWeight.Normal,
              color = tc.TextLabel,
          ),
      )
  }
}

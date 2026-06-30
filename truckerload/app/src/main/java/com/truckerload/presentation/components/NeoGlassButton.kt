package com.truckerload.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.truckerload.presentation.theme.SoftGradients
import com.truckerload.presentation.theme.LocalTruckColors

@Composable
fun NeoGlassPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val tc = LocalTruckColors.current
    val shape = remember { RoundedCornerShape(20.dp) }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .shadow(
                elevation = if (enabled) 6.dp else 0.dp,
                shape = shape,
                ambientColor = tc.AccentPrimary.copy(alpha = 0.2f),
                spotColor = tc.AccentSecondary.copy(alpha = 0.25f),
            )
            .clip(shape)
            .background(
                if (enabled) SoftGradients.horizontal
                else androidx.compose.ui.graphics.Brush.linearGradient(listOf(tc.Divider, tc.Divider)),
            )
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(PaddingValues(horizontal = 24.dp, vertical = 14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = if (enabled) tc.OnAccent else tc.TextSecondary,
        )
    }
}

package com.truckerload.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.truckerload.presentation.theme.SoftUiColors
import com.truckerload.presentation.theme.SoftUiElevation
import com.truckerload.presentation.theme.SoftUiShapes

private val PrimaryGradient: Brush
    @Composable get() {
        val primary = MaterialTheme.colorScheme.primary
        return Brush.horizontalGradient(listOf(primary, primary))
    }

private val DisabledGradient: Brush
    @Composable get() = Brush.horizontalGradient(
        listOf(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.surfaceVariant,
        ),
    )

/** Primary action — forest accent, soft shadow, rounded-2xl. */
@Composable
fun TlButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = SoftUiShapes.Button,
    colors: ButtonColors? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
    content: @Composable RowScope.() -> Unit,
) {
    if (colors != null) {
        androidx.compose.material3.Button(
            onClick = onClick,
            modifier = modifier.defaultMinSize(minHeight = 48.dp),
            enabled = enabled,
            shape = shape,
            colors = colors,
            elevation = ButtonDefaults.buttonElevation(0.dp, 2.dp, 0.dp, 0.dp, 0.dp),
            contentPadding = contentPadding,
            content = content,
        )
        return
    }
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .then(
                if (enabled) {
                    Modifier.shadow(
                        elevation = SoftUiElevation.Button,
                        shape = shape,
                        ambientColor = SoftUiColors.ShadowTint,
                        spotColor = SoftUiColors.ShadowNeutral,
                    )
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(if (enabled) PrimaryGradient else DisabledGradient),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        contentPadding = contentPadding,
        content = content,
    )
}

/** Text-only action — primary label, rounded hit area. */
@Composable
fun TlTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        enabled = enabled,
        shape = SoftUiShapes.Chip,
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        content = content,
    )
}

/**
 * Secondary action that still has to be pressed: tinted fill, solid accent
 * border, and a semibold label so it does not disappear into the card.
 */
@Composable
fun TlOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = SoftUiShapes.Button
    val accent = MaterialTheme.colorScheme.primary
    val borderColor = if (enabled) accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val fill = if (enabled) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .shadow(
                elevation = if (enabled) 2.dp else 0.dp,
                shape = shape,
                ambientColor = SoftUiColors.ShadowTint,
                spotColor = SoftUiColors.ShadowNeutral,
            ),
        enabled = enabled,
        shape = shape,
        border = BorderStroke(1.5.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = fill,
            disabledContainerColor = fill,
            contentColor = accent,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        content = {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            ) {
                content()
            }
        },
    )
}

/** Small pill chip — selected uses gradient, inactive uses soft outline. */
@Composable
fun TlChipButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
) {
    if (selected) {
        TlButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = SoftUiShapes.Chip,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(text, fontWeight = FontWeight.SemiBold)
        }
    } else {
        TlOutlinedButton(onClick = onClick, modifier = modifier, enabled = enabled) {
            Text(text, fontWeight = FontWeight.Medium)
        }
    }
}

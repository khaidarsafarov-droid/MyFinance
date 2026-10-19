package com.truckerload.presentation.theme

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.truckerload.presentation.icons.AppIcons

/**
 * Option 3 pill. Selected fill is brand primary (#3F3D9E / #D6BC85) with
 * onPrimary (#FFFFFF / #1D1708) — both clear WCAG AA. The leading icon
 * crossfades to a check. Unselected is the card surface with a 1dp border.
 * Visual height is 40dp; the outer box is 48dp so the touch target holds.
 */
data class ChipChrome(
    val container: Color,
    val content: Color,
    val border: Color,
)

private val LocalChipEmphasized = compositionLocalOf { false }

@Composable
fun rememberChipChrome(selected: Boolean): ChipChrome {
    val brand = LocalBrandPalette.current
    val reduceMotion = LocalReduceMotion.current
    val spec: AnimationSpec<Color> = if (reduceMotion) {
        snap()
    } else {
        tween(durationMillis = 180, easing = MotionEasingEmphasizedDecelerate)
    }
    val container by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) brand.primary else brand.card,
        animationSpec = spec,
        label = "chipContainer",
    )
    val content by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) brand.onPrimary else brand.textPrimary,
        animationSpec = spec,
        label = "chipContent",
    )
    val border by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) brand.primary else brand.cardBorder,
        animationSpec = spec,
        label = "chipBorder",
    )
    return ChipChrome(container, content, border)
}

@Composable
fun AppFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    val chrome = rememberChipChrome(selected)
    val reduceMotion = LocalReduceMotion.current
    val fadeSpec = if (reduceMotion) snap<Float>() else tween<Float>(durationMillis = 160)
    val labelStyle = MaterialTheme.typography.labelLarge.copy(
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        color = Color.Unspecified,
    )
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .height(48.dp)
            .semantics {
                role = Role.Button
                this.selected = selected
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.height(40.dp),
            shape = RoundedCornerShape(50),
            color = chrome.container,
            contentColor = chrome.content,
            border = BorderStroke(1.dp, chrome.border),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (selected || leadingIcon != null) {
                    AnimatedContent(
                        targetState = selected,
                        transitionSpec = { fadeIn(fadeSpec) togetherWith fadeOut(fadeSpec) },
                        label = "chipIcon",
                    ) { on ->
                        if (on) {
                            Icon(
                                imageVector = AppIcons.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        } else if (leadingIcon != null) {
                            leadingIcon()
                        }
                    }
                }
                CompositionLocalProvider(
                    LocalContentColor provides chrome.content,
                    LocalTextStyle provides labelStyle,
                    LocalChipEmphasized provides selected,
                ) {
                    label()
                }
                if (trailingIcon != null) {
                    trailingIcon()
                }
            }
        }
    }
}

/** Single-line chip label. Selected copy is bold. Never breaks a word. */
@Composable
fun AppChipLabel(text: String, modifier: Modifier = Modifier) {
    val emphasized = LocalChipEmphasized.current
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
        color = LocalContentColor.current,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
    )
}

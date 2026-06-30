package com.truckerload.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassClickableCard
import com.truckerload.presentation.theme.BentoGlassTheme

/** Neo-Glass card — thin facade over Bento-Glass primitives. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cornerRadius: Dp = BentoGlassTheme.CardRadius,
    borderColor: Color? = null,
    useHeroGradient: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        BentoGlassCard(
            modifier = modifier.clickable(onClick = onClick),
            cornerRadius = cornerRadius,
            borderColor = borderColor,
            useHeroGradient = useHeroGradient,
            content = {
                Column(modifier = Modifier.padding(16.dp), content = content)
            }
        )
    } else {
        BentoGlassCard(
            modifier = modifier,
            cornerRadius = cornerRadius,
            borderColor = borderColor,
            useHeroGradient = useHeroGradient,
            content = content
        )
    }
}

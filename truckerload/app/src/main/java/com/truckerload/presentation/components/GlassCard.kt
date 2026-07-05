package com.truckerload.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.truckerload.presentation.theme.BentoGlassTheme

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cornerRadius: Dp = BentoGlassTheme.CardRadius,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    solidBackground: Boolean = false,
    useCream: Boolean = false,
    useHero: Boolean = false,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape: Shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val cs = MaterialTheme.colorScheme
    val containerColor = when {
        useHero -> cs.primary
        useCream -> cs.secondaryContainer
        else -> cs.surface
    }
    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = containerColor,
        tonalElevation = if (useHero) 0.dp else 1.dp,
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

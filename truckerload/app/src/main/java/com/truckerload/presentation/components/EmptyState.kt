package com.truckerload.presentation.components

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.isActive
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.LocalReduceMotion
import com.truckerload.presentation.theme.TruckerLoadTheme

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    primaryAction: @Composable () -> Unit = {},
    secondaryAction: @Composable () -> Unit = {},
    tertiaryAction: @Composable () -> Unit = {},
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 280.dp)
            .padding(horizontal = 8.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (leading != null) {
            leading()
        } else {
            EmptyStateIllustration(icon = icon)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = cs.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = cs.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            primaryAction()
            secondaryAction()
            tertiaryAction()
        }
    }
}

@Composable
private fun EmptyStateIllustration(icon: ImageVector) {
    val cs = MaterialTheme.colorScheme
    val reduceMotion = LocalReduceMotion.current
    val floatAnim = remember { Animatable(0f) }
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            floatAnim.snapTo(0f)
            return@LaunchedEffect
        }
        while (isActive) {
            floatAnim.animateTo(6f, tween(durationMillis = 1800, easing = LinearEasing))
            floatAnim.animateTo(-6f, tween(durationMillis = 1800, easing = LinearEasing))
        }
    }
    val drift = floatAnim.value
    Box(
        modifier = Modifier.size(180.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(cs.primary.copy(alpha = 0.08f)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 28.dp, top = 36.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(cs.primary),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 36.dp, top = 48.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(cs.tertiary),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 18.dp)
                .size(12.dp)
                .clip(CircleShape)
                .background(cs.primary.copy(alpha = 0.55f)),
        )
        Box(
            modifier = Modifier
                .graphicsLayer { translationY = drift }
                .size(96.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(cs.surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = cs.primary,
                modifier = Modifier.size(48.dp),
            )
        }
    }
}

@Composable
fun EmptyStateButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    TlButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(end = 8.dp).size(20.dp))
        }
        Text(label)
    }
}

@Composable
fun EmptyStateSecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    TlOutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(end = 8.dp).size(20.dp))
        }
        Text(label)
    }
}

@Composable
fun EmptyStateTextAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp)) {
        Text(label)
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EmptyStatePreview() {
    TruckerLoadTheme(darkTheme = isSystemInDarkTheme(), dynamicColor = false) {
        EmptyState(
            icon = AppIcons.LocalShipping,
            title = stringResource(R.string.home_empty_all_title),
            message = stringResource(R.string.home_empty_filtered_body),
            primaryAction = {
                EmptyStateButton(
                    label = stringResource(R.string.empty_add_first_load),
                    onClick = {},
                )
            },
        )
    }
}

@Preview(name = "Dynamic", showBackground = true)
@Composable
private fun EmptyStateDynamicPreview() {
    TruckerLoadTheme(darkTheme = false, dynamicColor = true) {
        EmptyState(
            icon = AppIcons.LocalGasStation,
            title = stringResource(R.string.diesel_empty_week),
            message = stringResource(R.string.empty_diesel_detail),
            primaryAction = {
                EmptyStateButton(label = stringResource(R.string.empty_log_diesel), onClick = {})
            },
        )
    }
}

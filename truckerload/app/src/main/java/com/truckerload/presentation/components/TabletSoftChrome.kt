package com.truckerload.presentation.components

import com.truckerload.presentation.icons.AppIcons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.SoftUiColors
import com.truckerload.presentation.theme.SoftUiElevation
import com.truckerload.presentation.theme.SoftUiShapes
import com.truckerload.presentation.theme.UiDimens
import com.truckerload.presentation.utils.useNavigationRail

/**
 * App-wide page chrome: One UI large title on phone, oversized header on tablet.
 */
@Composable
fun SoftAppPageScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    showBack: Boolean = false,
    onBack: (() -> Unit)? = null,
    showPhoneMenu: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val tc = LocalTruckColors.current
    val tabletChrome = useNavigationRail()
    val openDrawer = LocalOpenDrawer.current

    Scaffold(
        modifier = modifier,
        containerColor = BentoGlassTheme.ScreenBackground,
        snackbarHost = snackbarHost,
        topBar = {
            if (tabletChrome) {
                SoftTabletPageHeader(
                    title = title,
                    subtitle = subtitle,
                    showBack = showBack,
                    onBack = onBack,
                    actions = actions,
                )
            } else {
                OneUiLargeTitleHeader(
                    title = title,
                    subtitle = subtitle,
                    navigationIcon = {
                        when {
                            showBack && onBack != null -> OneUiBackButton(onBack = onBack)
                            showPhoneMenu -> {
                                IconButton(onClick = openDrawer) {
                                    Icon(
                                        AppIcons.Menu,
                                        contentDescription = stringResource(R.string.common_menu),
                                        tint = tc.TextPrimary,
                                    )
                                }
                            }
                        }
                    },
                    actions = actions,
                )
            }
        },
        content = content,
    )
}

@Composable
fun SoftTabletPageHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    showBack: Boolean = false,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val tc = LocalTruckColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showBack && onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(UiDimens.ToolbarTouchTarget),
            ) {
                Icon(
                    AppIcons.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                    tint = tc.TextPrimary,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = tc.TextPrimary,
            )
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = tc.TextSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            content = actions,
        )
    }
}

/** Soft elevated white/surface card used across tablet pages. */
@Composable
fun SoftSurfaceCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .shadow(
                elevation = SoftUiElevation.Card,
                shape = SoftUiShapes.Card,
                ambientColor = SoftUiColors.ShadowTint,
                spotColor = SoftUiColors.ShadowNeutral,
            )
            .clip(SoftUiShapes.Card)
            .background(MaterialTheme.colorScheme.surface)
            .padding(18.dp),
        content = content,
    )
}

/** Two-column row that collapses to a single column on phone. */
@Composable
fun SoftTabletTwoPane(
    modifier: Modifier = Modifier,
    startWeight: Float = 1.2f,
    endWeight: Float = 1f,
    spacing: androidx.compose.ui.unit.Dp = 16.dp,
    start: @Composable () -> Unit,
    end: @Composable () -> Unit,
) {
    if (useNavigationRail()) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
        ) {
            Box(modifier = Modifier.weight(startWeight)) { start() }
            Box(modifier = Modifier.weight(endWeight)) { end() }
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            start()
            end()
        }
    }
}

@Composable
fun SoftActionChip(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(UiDimens.ToolbarTouchTarget)
            .clip(SoftUiShapes.Chip)
            .background(SoftUiColors.Sage.copy(alpha = 0.65f)),
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tc.TextPrimary)
    }
}

@Composable
fun SoftEmptyFill(
    message: String,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(message, color = tc.TextSecondary, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun SoftSectionSpacer(modifier: Modifier = Modifier) {
    Spacer(modifier = modifier.height(8.dp))
}

package com.truckerload.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.LocalBrandPalette
import com.truckerload.presentation.theme.UiDimens

private val HeroShape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
private val CardShape = RoundedCornerShape(24.dp)
private val TileShape = RoundedCornerShape(12.dp)

/**
 * Full-bleed indigo hero used by every Dashboard page.
 * The gradient paints into the status bar; the title row stays below it.
 */
@Composable
fun DashboardHero(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    showBack: Boolean = false,
    onBack: () -> Unit = {},
    showMenu: Boolean = false,
    onMenu: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val brand = LocalBrandPalette.current
    val onHero = Color.White
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(HeroShape)
            .background(Brush.horizontalGradient(listOf(brand.heroStart, brand.heroEnd)))
            .statusBarsPadding()
            .padding(bottom = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                showBack -> {
                    IconButton(onClick = onBack, modifier = Modifier.size(UiDimens.ToolbarTouchTarget)) {
                        Icon(AppIcons.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = onHero)
                    }
                }
                showMenu -> {
                    IconButton(onClick = onMenu, modifier = Modifier.size(UiDimens.ToolbarTouchTarget)) {
                        Icon(AppIcons.Menu, contentDescription = stringResource(R.string.common_menu), tint = onHero)
                    }
                }
            }
            Text(
                text = title,
                color = onHero,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp, end = 8.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically, content = actions)
        }
        subtitle?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                color = Color.White.copy(alpha = 0.86f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 2.dp),
            )
        }
        content()
    }
}

@Composable
fun DashboardSectionHeader(title: String, modifier: Modifier = Modifier) {
    val brand = LocalBrandPalette.current
    Text(
        text = title,
        color = brand.textSecondary,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
fun DashboardCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val brand = LocalBrandPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(brand.card)
            .border(1.dp, brand.cardBorder, CardShape),
        content = content,
    )
}

@Composable
fun DashboardIconTile(icon: ImageVector, modifier: Modifier = Modifier, gold: Boolean = false) {
    val brand = LocalBrandPalette.current
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(TileShape)
            .background(if (gold) brand.goldTagBg else brand.container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (gold) brand.goldTagText else brand.primary,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
fun DashboardRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    goldIcon: Boolean = false,
) {
    val brand = LocalBrandPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DashboardIconTile(icon = icon, gold = goldIcon)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 8.dp),
        ) {
            Text(
                text = title,
                color = brand.textPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = brand.textTertiary,
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (!value.isNullOrBlank()) {
            Text(
                text = value,
                color = brand.textPrimary,
                style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold,
            )
        }
        trailing?.invoke()
        if (onClick != null && trailing == null) {
            Icon(
                AppIcons.ChevronRight,
                contentDescription = null,
                tint = brand.textTertiary,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(20.dp),
            )
        }
    }
}

@Composable
fun DashboardPremiumTag(locked: Boolean, modifier: Modifier = Modifier) {
    val brand = LocalBrandPalette.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(brand.goldTagBg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            if (locked) AppIcons.Lock else AppIcons.WorkspacePremium,
            contentDescription = null,
            tint = brand.goldTagText,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = stringResource(R.string.dashboard_premium_tag),
            color = brand.goldTagText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun DashboardEmpty(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit = {},
) {
    val brand = LocalBrandPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            color = brand.textPrimary,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            color = brand.textSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        action()
    }
}

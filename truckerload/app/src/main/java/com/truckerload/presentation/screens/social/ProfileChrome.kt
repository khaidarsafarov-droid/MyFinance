package com.truckerload.presentation.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.LocalBrandPalette

private val CardShape = RoundedCornerShape(24.dp)
private val TileShape = RoundedCornerShape(12.dp)

@Composable
internal fun ProfileSectionHeader(title: String, modifier: Modifier = Modifier) {
    val brand = LocalBrandPalette.current
    Text(
        text = title,
        color = brand.textSecondary,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp),
    )
}

@Composable
internal fun ProfileCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val brand = LocalBrandPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(CardShape)
            .background(brand.card)
            .border(1.dp, brand.cardBorder, CardShape)
            .padding(vertical = 4.dp),
        content = content,
    )
}

@Composable
internal fun ProfileNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    action: String? = null,
    premiumLocked: Boolean? = null,
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
        ProfileIconTile(icon = icon, gold = goldIcon)
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
        if (premiumLocked != null) {
            ProfilePremiumTag(locked = premiumLocked)
        }
        if (!action.isNullOrBlank()) {
            Text(
                text = action,
                color = brand.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (onClick != null) {
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
private fun ProfilePremiumTag(locked: Boolean) {
    val brand = LocalBrandPalette.current
    Row(
        modifier = Modifier
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
            text = stringResource(R.string.profile_premium_tag),
            color = brand.goldTagText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ProfileIconTile(icon: ImageVector, gold: Boolean) {
    val brand = LocalBrandPalette.current
    val bg = if (gold) brand.goldTagBg else brand.container
    val tint = if (gold) brand.goldTagText else brand.primary
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(TileShape)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

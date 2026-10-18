package com.truckerload.presentation.screens.settings

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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.AppSwitchDefaults
import com.truckerload.presentation.theme.LocalBrandPalette
import com.truckerload.presentation.theme.LocalSemanticColors

private val CardShape = RoundedCornerShape(24.dp)
private val TileShape = RoundedCornerShape(12.dp)

@Composable
internal fun SettingsSectionHeader(title: String, modifier: Modifier = Modifier) {
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
internal fun SettingsCard(
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
internal fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    mark: SettingsPremiumMark = SettingsPremiumMark.NONE,
    danger: Boolean = false,
) {
    val brand = LocalBrandPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconTile(icon = icon, danger = danger)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 8.dp),
        ) {
            Text(
                text = title,
                color = if (danger) LocalSemanticColors.current.danger else brand.textPrimary,
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
        if (mark != SettingsPremiumMark.NONE) {
            SettingsPremiumTag(mark)
        }
        if (!value.isNullOrBlank()) {
            Text(
                text = value,
                color = brand.textSecondary,
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp),
                maxLines = 1,
            )
        }
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

@Composable
internal fun SettingsInfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    value: String,
    modifier: Modifier = Modifier,
) {
    val brand = LocalBrandPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconTile(icon = icon, danger = false)
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
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = value,
            color = brand.textSecondary,
            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
internal fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val brand = LocalBrandPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconTile(icon = icon, danger = false)
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
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = AppSwitchDefaults.colors(),
        )
    }
}

@Composable
internal fun SettingsPremiumTag(mark: SettingsPremiumMark) {
    val brand = LocalBrandPalette.current
    val icon = if (mark == SettingsPremiumMark.LOCK) AppIcons.Lock else AppIcons.WorkspacePremium
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(brand.goldTagBg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = brand.goldTagText, modifier = Modifier.size(12.dp))
        Text(
            text = stringResource(R.string.settings_premium_tag),
            color = brand.goldTagText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
internal fun SettingsIconTile(icon: ImageVector, danger: Boolean) {
    val brand = LocalBrandPalette.current
    val semantic = LocalSemanticColors.current
    val bg = if (danger) semantic.dangerContainer else brand.container
    val tint = if (danger) semantic.danger else brand.primary
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

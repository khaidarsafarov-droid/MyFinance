package com.truckerload.presentation.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.premium.PremiumFeature
import com.truckerload.presentation.components.DashboardCard
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.LocalBrandPalette
import com.truckerload.presentation.theme.OneUiTokens

/** Gold price chip on the indigo hero. Copy states the trial and the US catalog price. */
@Composable
internal fun PaywallPricePill(modifier: Modifier = Modifier) {
    val brand = LocalBrandPalette.current
    Text(
        text = stringResource(R.string.premium_price_pill),
        color = brand.goldOnGold,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(listOf(brand.goldStart, brand.goldEnd)))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/**
 * Paid rows follow [PremiumFeature] declaration order — the same gates the app enforces.
 * Free journal tools sit under the list as a compact chip row.
 */
@Composable
internal fun PaywallIncludedSection(modifier: Modifier = Modifier) {
    val brand = LocalBrandPalette.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(OneUiTokens.CardGap),
    ) {
        Text(
            text = stringResource(R.string.premium_included_title),
            color = brand.textPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        DashboardCard {
            Column(modifier = Modifier.fillMaxWidth()) {
                PremiumFeature.entries.forEachIndexed { index, feature ->
                    if (index > 0) {
                        HorizontalDivider(color = brand.cardBorder)
                    }
                    PaywallFeatureRow(feature)
                }
            }
        }
        PaywallFreeForever()
    }
}

@Composable
private fun PaywallFeatureRow(feature: PremiumFeature) {
    val brand = LocalBrandPalette.current
    val row = feature.row()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(brand.goldTagBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = row.icon,
                contentDescription = null,
                tint = brand.goldTagText,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(
            modifier = Modifier.padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(row.title),
                color = brand.textPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(row.benefit),
                color = brand.textSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaywallFreeForever() {
    val brand = LocalBrandPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.premium_free_title),
            color = brand.textSecondary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                R.string.premium_free_loads,
                R.string.premium_free_diesel,
                R.string.premium_free_pay,
                R.string.premium_free_deadhead,
            ).forEach { label ->
                Text(
                    text = stringResource(label),
                    color = brand.onContainer,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(brand.container)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}

private data class PaywallFeatureCopy(
    val icon: ImageVector,
    val title: Int,
    val benefit: Int,
)

private fun PremiumFeature.row(): PaywallFeatureCopy = when (this) {
    PremiumFeature.TELEGRAM -> PaywallFeatureCopy(
        AppIcons.Send,
        R.string.premium_feature_telegram_title,
        R.string.premium_feature_telegram_benefit,
    )
    PremiumFeature.ANALYTICS -> PaywallFeatureCopy(
        AppIcons.Insights,
        R.string.premium_feature_analytics_title,
        R.string.premium_feature_analytics_benefit,
    )
    PremiumFeature.WEEKLY_GOAL -> PaywallFeatureCopy(
        AppIcons.Flag,
        R.string.premium_feature_goal_title,
        R.string.premium_feature_goal_benefit,
    )
    PremiumFeature.MAP -> PaywallFeatureCopy(
        AppIcons.Map,
        R.string.premium_feature_map_title,
        R.string.premium_feature_map_benefit,
    )
    PremiumFeature.TAX -> PaywallFeatureCopy(
        AppIcons.Receipt,
        R.string.premium_feature_tax_title,
        R.string.premium_feature_tax_benefit,
    )
    PremiumFeature.SCANNER -> PaywallFeatureCopy(
        AppIcons.DocumentScanner,
        R.string.premium_feature_scanner_title,
        R.string.premium_feature_scanner_benefit,
    )
    PremiumFeature.CAMERA -> PaywallFeatureCopy(
        AppIcons.PhotoCamera,
        R.string.premium_feature_camera_title,
        R.string.premium_feature_camera_benefit,
    )
    PremiumFeature.EXPORT -> PaywallFeatureCopy(
        AppIcons.FileDownload,
        R.string.premium_feature_export_title,
        R.string.premium_feature_export_benefit,
    )
    PremiumFeature.DRIVE -> PaywallFeatureCopy(
        AppIcons.CloudStorage,
        R.string.premium_feature_drive_title,
        R.string.premium_feature_drive_benefit,
    )
}

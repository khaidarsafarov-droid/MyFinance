package com.truckerload.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.BrandPalettes
import com.truckerload.presentation.theme.LocalBrandPalette
import com.truckerload.presentation.theme.LocalSemanticColors
import com.truckerload.presentation.theme.UiDimens

@Composable
internal fun SettingsPremiumHero(
    model: SettingsPremiumHeroModel,
    showBack: Boolean,
    onBack: () -> Unit,
    purchasing: Boolean,
    onManage: () -> Unit,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val brand = LocalBrandPalette.current
    val onHero = Color.White
    val muted = Color.White.copy(alpha = 0.86f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Brush.horizontalGradient(listOf(brand.heroStart, brand.heroEnd)))
            .padding(bottom = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 8.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showBack) {
                IconButton(onClick = onBack, modifier = Modifier.size(UiDimens.ToolbarTouchTarget)) {
                    Icon(
                        AppIcons.ArrowBack,
                        contentDescription = stringResource(R.string.common_back),
                        tint = onHero,
                    )
                }
            }
            Text(
                text = stringResource(R.string.settings_title),
                color = onHero,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(brand.goldStart, brand.goldEnd)))
                    .border(1.5.dp, brand.goldOnHero, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    AppIcons.WorkspacePremium,
                    contentDescription = null,
                    tint = brand.goldOnGold,
                    modifier = Modifier.size(28.dp),
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                if (model.phase == SettingsPremiumPhase.ACTIVE) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.premium_title),
                            color = onHero,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        PremiumPill(modifier = Modifier.padding(start = 8.dp))
                    }
                } else {
                    PremiumPill()
                }
                StatusLine(model = model, muted = muted, modifier = Modifier.padding(top = 6.dp))
            }
        }
        if (model.phase != SettingsPremiumPhase.ACTIVE) {
            Text(
                text = heroTitle(model),
                color = onHero,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp),
            )
        }
        FeatureGrid(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp))
        HeroAction(
            model = model,
            purchasing = purchasing,
            onManage = onManage,
            onPurchase = onPurchase,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp),
        )
        if (model.phase != SettingsPremiumPhase.ACTIVE && model.price != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.settings_premium_then_price, model.price),
                    color = muted,
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.settings_premium_restore_short),
                    color = onHero,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clickable(role = Role.Button, onClick = onRestore)
                        .padding(horizontal = 8.dp, vertical = 14.dp),
                )
            }
        }
    }
}

@Composable
private fun PremiumPill(modifier: Modifier = Modifier) {
    val brand = LocalBrandPalette.current
    Text(
        text = stringResource(R.string.settings_premium_pill),
        color = brand.goldOnHero,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(BrandPalettes.Dark.goldTagBg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun StatusLine(model: SettingsPremiumHeroModel, muted: Color, modifier: Modifier = Modifier) {
    val semantic = LocalSemanticColors.current
    val active = model.phase == SettingsPremiumPhase.ACTIVE
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(if (active) semantic.successContainer else Color.White.copy(alpha = 0.16f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (active) AppIcons.Check else AppIcons.Lock,
                contentDescription = null,
                tint = if (active) semantic.success else Color.White,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = stringResource(
                    if (active) R.string.settings_premium_active_chip else R.string.settings_premium_inactive_chip,
                ),
                color = if (active) semantic.success else Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        if (active) {
            Text(
                text = stringResource(R.string.settings_premium_renews),
                color = muted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun heroTitle(model: SettingsPremiumHeroModel): String = when (model.phase) {
    SettingsPremiumPhase.ACTIVE -> stringResource(R.string.premium_title)
    SettingsPremiumPhase.FREE_MONTH -> stringResource(R.string.settings_premium_trial_title)
    SettingsPremiumPhase.PAID -> stringResource(R.string.premium_subscribe_paid, model.price.orEmpty())
    SettingsPremiumPhase.UNKNOWN -> stringResource(R.string.premium_price_loading)
}

@Composable
private fun FeatureGrid(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SettingsPremiumGroup.entries.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { group ->
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            AppIcons.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(SettingsPremiumState.featureLabelRes(group)),
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroAction(
    model: SettingsPremiumHeroModel,
    purchasing: Boolean,
    onManage: () -> Unit,
    onPurchase: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val brand = LocalBrandPalette.current
    val active = model.phase == SettingsPremiumPhase.ACTIVE
    val label = when (model.phase) {
        SettingsPremiumPhase.ACTIVE -> stringResource(R.string.settings_premium_manage_short)
        SettingsPremiumPhase.FREE_MONTH -> stringResource(R.string.settings_premium_try_free)
        SettingsPremiumPhase.PAID -> stringResource(R.string.premium_subscribe_paid, model.price.orEmpty())
        SettingsPremiumPhase.UNKNOWN -> stringResource(R.string.premium_price_loading)
    }
    val gold = model.phase == SettingsPremiumPhase.FREE_MONTH || model.phase == SettingsPremiumPhase.PAID
    val container = if (gold) {
        Brush.horizontalGradient(listOf(brand.goldStart, brand.goldEnd))
    } else {
        Brush.horizontalGradient(listOf(Color.White, Color.White))
    }
    val content = if (gold) brand.goldOnGold else brand.primary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .clickable(
                role = Role.Button,
                enabled = !purchasing && model.phase != SettingsPremiumPhase.UNKNOWN,
                onClick = { if (active) onManage() else onPurchase() },
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (gold) {
            Icon(AppIcons.WorkspacePremium, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = label,
            color = content,
            style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        if (active) {
            Spacer(Modifier.width(8.dp))
            Icon(AppIcons.OpenInNew, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        }
    }
}

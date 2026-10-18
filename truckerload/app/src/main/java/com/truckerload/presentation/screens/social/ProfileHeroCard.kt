package com.truckerload.presentation.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.components.LocalOpenDrawer
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.LocalBrandPalette
import com.truckerload.presentation.theme.LocalSemanticColors
import com.truckerload.presentation.theme.UiDimens
import com.truckerload.presentation.utils.MoneyFormat
import com.truckerload.utils.AnalyticsOwnerName
import java.util.Locale

@Composable
internal fun ProfileHero(
    givenName: String,
    familyName: String,
    avatarUrl: String?,
    subscribed: Boolean,
    totalLoads: Int,
    totalRevenue: Double,
    averageRpm: Double,
    redThreshold: Double,
    greenThreshold: Double,
    showMenu: Boolean,
    showBack: Boolean,
    onBack: () -> Unit,
    onEditName: () -> Unit,
    onAddLoad: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val brand = LocalBrandPalette.current
    val onHero = Color.White
    val muted = Color.White.copy(alpha = 0.86f)
    val display = AnalyticsOwnerName.display(givenName, familyName)
    val initials = ProfileHeroState.initials(givenName, familyName)
    val empty = ProfileHeroState.isEmpty(totalLoads)
    val chip = ProfileHeroState.rpmChip(averageRpm, redThreshold, greenThreshold)

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
                    Icon(AppIcons.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = onHero)
                }
            } else if (showMenu) {
                IconButton(
                    onClick = LocalOpenDrawer.current,
                    modifier = Modifier.size(UiDimens.ToolbarTouchTarget),
                ) {
                    Icon(AppIcons.Menu, contentDescription = stringResource(R.string.common_menu), tint = onHero)
                }
            }
            Text(
                text = stringResource(R.string.profile),
                color = onHero,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
            )
            IconButton(onClick = onEditName, modifier = Modifier.size(UiDimens.ToolbarTouchTarget)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        AppIcons.Edit,
                        contentDescription = stringResource(R.string.profile_edit_name),
                        tint = onHero,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val avatar = @Composable {
                ProfileAvatar(avatarUrl = avatarUrl, initials = initials, onHero = true)
            }
            if (subscribed) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(brand.goldStart, brand.goldEnd)))
                        .padding(3.dp),
                ) { avatar() }
            } else {
                avatar()
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = display.ifBlank { stringResource(R.string.profile_name_placeholder) },
                    color = onHero,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                if (subscribed) {
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.profile_premium_tag).uppercase(Locale.ROOT),
                            color = brand.goldOnGold,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Brush.horizontalGradient(listOf(brand.goldStart, brand.goldEnd)))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                        Text(
                            text = stringResource(R.string.profile_premium_short),
                            color = muted,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.profile_free_version),
                        color = muted,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
        if (empty) {
            Text(
                text = stringResource(R.string.profile_empty_title),
                color = onHero,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp),
            )
            Text(
                text = stringResource(R.string.profile_empty_hint),
                color = muted,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .clickable(role = Role.Button, onClick = onAddLoad)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(AppIcons.Add, contentDescription = null, tint = brand.primary, modifier = Modifier.size(20.dp))
                Text(
                    text = stringResource(R.string.home_add_load_button),
                    color = brand.primary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        } else {
            Text(
                text = stringResource(R.string.profile_lifetime_income),
                color = muted,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp),
            )
            Text(
                text = MoneyFormat.formatCurrency(totalRevenue, decimals = 0),
                color = onHero,
                style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 2.dp),
            )
            Row(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.profile_rpm_line, MoneyFormat.formatRpmShort(averageRpm)),
                    color = onHero,
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                )
                RpmHeroChip(
                    chip = chip,
                    greenThreshold = greenThreshold,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun RpmHeroChip(chip: ProfileRpmChip, greenThreshold: Double, modifier: Modifier = Modifier) {
    val semantic = LocalSemanticColors.current
    val (bg, fg) = when (chip) {
        ProfileRpmChip.ABOVE -> semantic.successContainer to semantic.success
        ProfileRpmChip.MID -> semantic.warningContainer to semantic.warning
        ProfileRpmChip.BELOW -> semantic.dangerContainer to semantic.danger
    }
    val label = when (chip) {
        ProfileRpmChip.ABOVE -> stringResource(
            R.string.profile_rpm_above,
            MoneyFormat.formatCurrency(greenThreshold, decimals = 2),
        )
        ProfileRpmChip.MID -> stringResource(R.string.rpm_badge_ok)
        ProfileRpmChip.BELOW -> stringResource(R.string.rpm_badge_low)
    }
    Text(
        text = label,
        color = fg,
        style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

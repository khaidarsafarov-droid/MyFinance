package com.truckerload.presentation.screens.social

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.geo.CountryCatalog
import com.truckerload.domain.social.EnhancedDriverProfile
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassMetricCell
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.MoneyFormat
import java.util.Locale

@Composable
internal fun PremiumProfileHeader(
    profile: EnhancedDriverProfile,
    isUploadingAvatar: Boolean = false,
    onAvatarClick: (() -> Unit)? = null,
    onNameClick: (() -> Unit)? = null,
) {
    val tc = LocalTruckColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(tc.AccentPrimary.copy(alpha = 0.55f), tc.SurfaceSecondary),
                    ),
                ),
        )
        BentoGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset(y = (-36).dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileAvatar(
                        avatarUrl = profile.avatarUrl,
                        isUploading = isUploadingAvatar,
                        onClick = onAvatarClick,
                    )
                    Column {
                        Text(
                            profile.displayName.ifBlank { stringResource(R.string.profile_name_placeholder) },
                            style = AppTypography.CardTitle,
                            color = tc.TextPrimary,
                            modifier = if (onNameClick != null) {
                                Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onNameClick,
                                )
                            } else {
                                Modifier
                            },
                        )
                        if (onAvatarClick != null) {
                            Text(
                                stringResource(R.string.profile_change_photo),
                                style = AppTypography.Caption,
                                color = tc.AccentPrimary,
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onAvatarClick,
                                ),
                            )
                        }
                        val truckMeta = buildList {
                            if (profile.truckType != com.truckerload.domain.social.TruckType.OTHER) {
                                add("${profile.truckType.emoji} ${profile.truckType.label}")
                            }
                            if (profile.experienceYears > 0) {
                                add("${profile.experienceYears} ${stringResource(R.string.experience_years)}")
                            }
                        }
                        if (truckMeta.isNotEmpty()) {
                            Text(
                                truckMeta.joinToString(" · "),
                                style = AppTypography.Subtitle,
                                color = tc.TextSecondary,
                            )
                        }
                        val isRussian = Locale.getDefault().language.equals("ru", ignoreCase = true)
                        val locationLabel = CountryCatalog.byIso2(profile.homeState)
                            ?.let { "${it.iso2} · ${it.displayName(isRussian)}" }
                            ?: profile.homeState
                        if (locationLabel.isNotBlank() || profile.status != com.truckerload.domain.social.DriverStatus.OFFLINE) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                if (locationLabel.isNotBlank()) {
                                    Icon(
                                        imageVector = Icons.Outlined.LocationOn,
                                        contentDescription = null,
                                        tint = tc.TextSecondary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                                Text(
                                    text = buildString {
                                        if (locationLabel.isNotBlank()) append(locationLabel)
                                        if (locationLabel.isNotBlank()) append(" · ")
                                        append(profile.status.label.substringAfter(' ', profile.status.label))
                                    },
                                    style = AppTypography.Subtitle,
                                    color = tc.TextSecondary,
                                )
                            }
                        }
                    }
                }
                if (profile.ratingCount > 0) {
                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = tc.AccentPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "${"%.1f".format(profile.rating)} (${profile.ratingCount}) ·",
                            style = AppTypography.Subtitle,
                            color = tc.AccentPrimary,
                        )
                        Icon(
                            imageVector = Icons.Outlined.WorkspacePremium,
                            contentDescription = null,
                            tint = tc.AccentPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "${profile.reputation} ${stringResource(R.string.social_reputation_short)}",
                            style = AppTypography.Subtitle,
                            color = tc.AccentPrimary,
                        )
                    }
                }
                profile.currentRoute?.takeIf { it.isNotBlank() }?.let { route ->
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Route,
                            contentDescription = null,
                            tint = tc.TextSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(route, style = AppTypography.Subtitle, color = tc.TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
internal fun PremiumStatsRow(profile: EnhancedDriverProfile) {
    val tc = LocalTruckColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BentoGlassMetricCell(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.social_stat_loads),
                value = profile.totalLoads.toString(),
                accent = tc.AccentPrimary,
            )
            BentoGlassMetricCell(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.social_stat_miles),
                value = "%,d".format(profile.totalMiles),
                accent = tc.AccentProfit,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BentoGlassMetricCell(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.social_stat_revenue),
                value = MoneyFormat.formatCurrency(profile.totalRevenue),
                accent = tc.AccentPrimary,
            )
            BentoGlassMetricCell(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.social_stat_rpm),
                value = MoneyFormat.formatCurrency(profile.averageRpm),
                accent = tc.AccentProfit,
            )
        }
    }
}

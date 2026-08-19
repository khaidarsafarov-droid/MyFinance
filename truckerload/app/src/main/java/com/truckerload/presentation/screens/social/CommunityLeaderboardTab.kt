package com.truckerload.presentation.screens.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.crowd.CrowdRpmSnapshot
import com.truckerload.domain.social.LeaderboardCategory
import com.truckerload.domain.social.LeaderboardEntry
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassClickableCard
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.MoneyFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LeaderboardTabContent(
    entries: List<LeaderboardEntry>,
    crowdRpm: CrowdRpmSnapshot,
    showRanking: Boolean,
    onCategoryChange: (LeaderboardCategory) -> Unit,
    onPeerClick: (String) -> Unit,
) {
    val tc = LocalTruckColors.current
    var categoryIndex by remember { mutableIntStateOf(0) }
    val categories = LeaderboardCategory.entries
    val category = categories[categoryIndex]
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            CrowdRpmCard(snapshot = crowdRpm)
        }
        if (!showRanking) {
            item {
                Text(
                    text = stringResource(R.string.community_ranking_locked),
                    style = AppTypography.Subtitle,
                    color = tc.TextSecondary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        } else {
        item {
            PrimaryScrollableTabRow(
                selectedTabIndex = categoryIndex,
                edgePadding = 0.dp,
            ) {
                categories.forEachIndexed { index, item ->
                    Tab(
                        selected = categoryIndex == index,
                        onClick = {
                            categoryIndex = index
                            onCategoryChange(item)
                        },
                        text = {
                            Text(
                                text = stringResource(item.shortLabelRes()),
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                            )
                        },
                    )
                }
            }
        }
        item {
            LeaderboardColumnHeader(category = category)
        }
        items(entries, key = { "${it.rank}_${it.displayName}" }) { entry ->
            val peerId = entry.userId
            val clickable = !entry.isMe && !peerId.isNullOrBlank()
            BentoGlassClickableCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { peerId?.takeIf { clickable }?.let(onPeerClick) },
            ) {
                LeaderboardEntryRow(entry = entry, category = category)
            }
        }
        }
    }
}

@Composable
private fun LeaderboardColumnHeader(category: LeaderboardCategory) {
    val tc = LocalTruckColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.leaderboard_col_player),
            style = AppTypography.Subtitle,
            color = tc.TextSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.leaderboard_col_trust),
            style = AppTypography.Subtitle,
            color = tc.TextSecondary,
            textAlign = TextAlign.End,
            modifier = Modifier.width(72.dp),
        )
        Text(
            text = stringResource(category.scoreColumnRes()),
            style = AppTypography.Subtitle,
            color = tc.TextSecondary,
            textAlign = TextAlign.End,
            modifier = Modifier.width(96.dp),
        )
    }
}

@Composable
private fun LeaderboardEntryRow(
    entry: LeaderboardEntry,
    category: LeaderboardCategory,
) {
    val tc = LocalTruckColors.current
    val trustLabel = stringResource(R.string.trust_rating_value, "%.1f".format(entry.rating))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (entry.rank <= 3) {
                Icon(
                    imageVector = Icons.Outlined.EmojiEvents,
                    contentDescription = "#${entry.rank}",
                    tint = when (entry.rank) {
                        1 -> tc.AccentPrimary
                        2 -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.tertiary
                    },
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Text(
                    text = "#${entry.rank}",
                    style = AppTypography.CardTitle,
                    color = if (entry.isMe) tc.AccentPrimary else tc.TextPrimary,
                )
            }
            Text(
                text = entry.displayName,
                style = AppTypography.CardTitle,
                color = if (entry.isMe) tc.AccentPrimary else tc.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            modifier = Modifier
                .width(72.dp)
                .semantics { contentDescription = trustLabel },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = tc.TextSecondary,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = "%.1f".format(entry.rating),
                style = AppTypography.Subtitle,
                color = tc.TextSecondary,
            )
        }
        Text(
            text = "${formatLeaderboardScore(category, entry.score)} ${entry.trend}",
            style = AppTypography.Subtitle,
            color = tc.TextSecondary,
            textAlign = TextAlign.End,
            modifier = Modifier.width(96.dp),
        )
    }
}

internal fun LeaderboardCategory.shortLabelRes(): Int = when (this) {
    LeaderboardCategory.OVERALL -> R.string.leaderboard_tab_overview
    LeaderboardCategory.LOADS -> R.string.leaderboard_tab_loads
    LeaderboardCategory.REVENUE -> R.string.leaderboard_tab_revenue
    LeaderboardCategory.RPM -> R.string.leaderboard_tab_rpm
}

internal fun LeaderboardCategory.scoreColumnRes(): Int = when (this) {
    LeaderboardCategory.OVERALL -> R.string.leaderboard_col_miles
    LeaderboardCategory.LOADS -> R.string.leaderboard_col_loads
    LeaderboardCategory.REVENUE -> R.string.leaderboard_col_revenue
    LeaderboardCategory.RPM -> R.string.leaderboard_col_rpm
}

internal fun formatLeaderboardScore(category: LeaderboardCategory, score: Double): String =
    when (category) {
        LeaderboardCategory.OVERALL -> "${MoneyFormat.formatNumber(score)} mi"
        LeaderboardCategory.LOADS -> MoneyFormat.formatNumber(score)
        LeaderboardCategory.REVENUE -> MoneyFormat.formatCurrency(score)
        LeaderboardCategory.RPM -> MoneyFormat.formatRpmShort(score)
    }

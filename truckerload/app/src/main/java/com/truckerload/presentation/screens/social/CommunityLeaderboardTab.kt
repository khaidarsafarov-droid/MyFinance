package com.truckerload.presentation.screens.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.truckerload.domain.social.LeaderboardCategory
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassClickableCard
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.MoneyFormat

@Composable
internal fun LeaderboardTabContent(
    entries: List<com.truckerload.domain.social.LeaderboardEntry>,
    onCategoryChange: (LeaderboardCategory) -> Unit,
    onPeerClick: (String) -> Unit,
) {
    val tc = LocalTruckColors.current
    var categoryIndex by remember { mutableIntStateOf(0) }
    val categories = LeaderboardCategory.entries
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            PrimaryTabRow(selectedTabIndex = categoryIndex) {
                categories.forEachIndexed { index, category ->
                    Tab(
                        selected = categoryIndex == index,
                        onClick = {
                            categoryIndex = index
                            onCategoryChange(category)
                        },
                        text = { Text(category.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
        }
        items(entries, key = { "${it.rank}_${it.displayName}" }) { entry ->
            val peerId = entry.userId
            val clickable = !entry.isMe && !peerId.isNullOrBlank()
            BentoGlassClickableCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { peerId?.takeIf { clickable }?.let(onPeerClick) },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (entry.rank <= 3) {
                            Icon(
                                imageVector = Icons.Outlined.EmojiEvents,
                                contentDescription = null,
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
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = tc.TextSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "${"%.1f".format(entry.rating)}  ${MoneyFormat.formatNumber(entry.score)} ${entry.trend}",
                            style = AppTypography.Subtitle,
                            color = tc.TextSecondary,
                        )
                    }
                }
            }
        }
    }
}

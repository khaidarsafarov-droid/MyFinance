package com.truckerload.presentation.screens.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.social.Challenge
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.MoneyFormat
import com.truckerload.presentation.components.TlButton as Button

@Composable
internal fun ChallengesTabContent(
    challenge: Challenge,
    joined: Boolean,
    isJoining: Boolean,
    showRanking: Boolean,
    onJoin: () -> Unit,
) {
    val tc = LocalTruckColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.weekly_challenge), style = AppTypography.CardTitle, color = tc.TextPrimary)
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EmojiEvents,
                        contentDescription = null,
                        tint = tc.AccentPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(challenge.title, style = AppTypography.Subtitle)
                }
                Text(challenge.description, style = AppTypography.Subtitle, color = tc.TextSecondary)
                Text(
                    text = if (showRanking && challenge.myPosition != null) {
                        stringResource(
                            R.string.challenge_ranked_position,
                            challenge.myPosition,
                            MoneyFormat.formatNumber(challenge.myScore),
                        )
                    } else {
                        stringResource(
                            R.string.challenge_personal_progress,
                            MoneyFormat.formatNumber(challenge.myScore),
                        )
                    },
                    style = AppTypography.Subtitle,
                    color = tc.AccentPrimary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        if (showRanking) {
            challenge.leaderboard.forEach { entry ->
                BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${entry.rank}. ${entry.displayName} — ${MoneyFormat.formatNumber(entry.score)} mi",
                        modifier = Modifier.padding(16.dp),
                        style = AppTypography.Subtitle,
                    )
                }
            }
        } else {
            Text(
                text = stringResource(R.string.community_ranking_locked),
                style = AppTypography.Subtitle,
                color = tc.TextSecondary,
            )
        }
        Text(
            text = if (joined) {
                stringResource(R.string.social_challenge_joined)
            } else {
                stringResource(R.string.social_join_challenge)
            },
            style = AppTypography.Subtitle,
            color = tc.TextSecondary,
        )
        if (!joined) {
            Button(
                onClick = onJoin,
                enabled = !isJoining,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.social_join_challenge))
            }
        }
    }
}

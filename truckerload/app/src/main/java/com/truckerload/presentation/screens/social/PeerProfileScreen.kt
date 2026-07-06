package com.truckerload.presentation.screens.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.truckerload.presentation.components.TlButton as Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckerload.R
import com.truckerload.presentation.di.LocalSocialRepository
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassMetricCell
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.MoneyFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeerProfileScreen(
    peerId: String,
    onBack: () -> Unit,
    viewModel: PeerProfileViewModel = viewModel(
        key = peerId,
        factory = PeerProfileViewModel.Factory(peerId, LocalSocialRepository.current),
    ),
) {
    val uiState by viewModel.uiState.collectAsState()
    val peer = uiState.peer
    val tc = LocalTruckColors.current

    Scaffold(
        containerColor = BentoGlassTheme.ScreenBackground,
        topBar = {
            TopAppBar(
                title = { Text(peer?.displayName ?: stringResource(R.string.profile)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BentoGlassTheme.ScreenBackground),
            )
        },
    ) { padding ->
        if (peer == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.social_loading), color = tc.TextSecondary)
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(peer.displayName, style = AppTypography.CardTitle, color = tc.TextPrimary)
                    Text(
                        "⭐ ${"%.1f".format(peer.rating)}",
                        style = AppTypography.Subtitle,
                        color = tc.TextSecondary,
                    )
                    Button(
                        onClick = viewModel::toggleFollow,
                        enabled = !uiState.isUpdatingFollow,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (uiState.isFollowing) {
                                stringResource(R.string.social_unfollow)
                            } else {
                                stringResource(R.string.social_follow)
                            },
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BentoGlassMetricCell(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.social_stat_miles),
                    value = MoneyFormat.formatNumber(peer.weeklyMiles),
                    accent = tc.AccentPrimary,
                )
                BentoGlassMetricCell(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.social_stat_loads),
                    value = peer.weeklyLoads.toString(),
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
                    value = MoneyFormat.formatCurrency(peer.weeklyRevenue),
                    accent = tc.AccentPrimary,
                )
                BentoGlassMetricCell(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.social_stat_rpm),
                    value = MoneyFormat.formatCurrency(peer.weeklyRpm),
                    accent = tc.AccentProfit,
                )
            }
        }
    }
}

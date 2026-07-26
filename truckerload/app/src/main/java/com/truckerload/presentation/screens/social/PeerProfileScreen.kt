package com.truckerload.presentation.screens.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import com.truckerload.presentation.components.TlButton as Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
    onOpenChat: (String) -> Unit = {},
    onStartCall: (String, String) -> Unit = { _, _ -> },
    viewModel: PeerProfileViewModel = viewModel(
        key = peerId,
        factory = PeerProfileViewModel.Factory(peerId, LocalSocialRepository.current),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val peer = uiState.peer
    val tc = LocalTruckColors.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = BentoGlassTheme.ScreenBackground,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(peer?.displayName ?: stringResource(R.string.profile)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            "%.1f".format(peer.rating),
                            style = AppTypography.Subtitle,
                            color = tc.TextSecondary,
                        )
                    }
                    Button(
                        onClick = viewModel::toggleFollow,
                        enabled = !uiState.isUpdatingFollow && !uiState.isBlocked,
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { viewModel.startPrivateChat(onOpenChat) },
                            enabled = !uiState.isBlocked,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.social_message_peer))
                        }
                        Button(
                            onClick = { peer?.let { onStartCall(peerId, it.displayName) } },
                            enabled = !uiState.isBlocked,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.social_call_peer))
                        }
                    }
                    Button(
                        onClick = viewModel::toggleBlock,
                        enabled = !uiState.isBlocking,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (uiState.isBlocked) {
                                stringResource(R.string.social_unblock_user)
                            } else {
                                stringResource(R.string.social_block_user)
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

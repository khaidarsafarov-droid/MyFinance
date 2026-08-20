package com.truckerload.presentation.screens.social

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonRemove
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
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
    viewModel: PeerProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val peer = uiState.peer
    val tc = LocalTruckColors.current
    val snackbarHostState = remember { SnackbarHostState() }
    val reportSent = stringResource(R.string.social_report_sent)

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            val text = if (message == "reported") reportSent else message
            snackbarHostState.showSnackbar(text)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = BentoGlassTheme.ScreenBackground,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { },
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
        var showReport by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PeerIdentityHeader(displayName = peer.displayName, rating = peer.rating)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = { viewModel.startPrivateChat(onOpenChat) },
                    enabled = !uiState.isBlocked,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.social_message_peer))
                }
                if (!uiState.isBlocked) {
                    Button(
                        onClick = { onStartCall(peerId, peer.displayName) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            Icons.Outlined.Call,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 6.dp),
                        )
                        Text(stringResource(R.string.social_call_peer))
                    }
                }
            }
            PeerActionsCard {
                PeerActionRow(
                    icon = if (uiState.isFollowing) Icons.Outlined.PersonRemove else Icons.Outlined.PersonAdd,
                    label = stringResource(
                        if (uiState.isFollowing) R.string.social_unfollow else R.string.social_follow,
                    ),
                    enabled = !uiState.isUpdatingFollow && !uiState.isBlocked,
                    onClick = viewModel::toggleFollow,
                )
                PeerActionRow(
                    icon = Icons.Outlined.Block,
                    label = stringResource(
                        if (uiState.isBlocked) R.string.social_unblock_user else R.string.social_block_user,
                    ),
                    enabled = !uiState.isBlocking,
                    onClick = viewModel::toggleBlock,
                )
                PeerActionRow(
                    icon = Icons.Outlined.Flag,
                    label = stringResource(R.string.social_report_user),
                    enabled = !uiState.isBlocked,
                    onClick = { showReport = true },
                )
            }
            if (showReport) {
                ReportReasonDialog(
                    onDismiss = { showReport = false },
                    onPick = { reason ->
                        showReport = false
                        viewModel.reportPeer(reason)
                    },
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
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

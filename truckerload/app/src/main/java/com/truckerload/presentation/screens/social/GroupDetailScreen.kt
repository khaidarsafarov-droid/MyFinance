package com.truckerload.presentation.screens.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckerload.R
import com.truckerload.presentation.di.LocalSocialRepository
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    chatId: String,
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    viewModel: GroupDetailViewModel = viewModel(
        key = chatId,
        factory = GroupDetailViewModel.Factory(chatId, LocalSocialRepository.current),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val chat = uiState.chat
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
                title = { Text(chat?.title ?: stringResource(R.string.group_chat)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BentoGlassTheme.ScreenBackground),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(chat?.description.orEmpty(), color = tc.TextSecondary)
                        if (!chat?.inviteCode.isNullOrBlank()) {
                            Text(
                                stringResource(R.string.social_invite_code_label, chat?.inviteCode.orEmpty()),
                                color = tc.AccentPrimary,
                            )
                        }
                        Button(
                            onClick = { onOpenChat(chatId) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.social_open_chat))
                        }
                        Button(
                            onClick = { viewModel.leaveGroup(onBack) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.social_leave_group))
                        }
                    }
                }
            }
            item {
                Text(stringResource(R.string.social_members), style = MaterialTheme.typography.titleMedium, color = tc.TextPrimary)
            }
            items(uiState.members, key = { it.userId }) { member ->
                BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (member.isMe) "${member.displayName} (${stringResource(R.string.social_you)})" else member.displayName,
                        modifier = Modifier.padding(16.dp),
                        color = if (member.isMe) tc.AccentPrimary else tc.TextPrimary,
                    )
                }
            }
        }
    }
}

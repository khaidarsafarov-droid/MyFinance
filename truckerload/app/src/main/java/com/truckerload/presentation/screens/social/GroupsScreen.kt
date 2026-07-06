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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckerload.R
import com.truckerload.domain.social.SocialChat
import com.truckerload.presentation.di.LocalSocialRepository
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassClickableCard
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    onBack: () -> Unit,
    onOpenGroup: (String) -> Unit,
    onOpenChat: (String) -> Unit,
    viewModel: GroupsViewModel = viewModel(
        factory = GroupsViewModel.Factory(LocalSocialRepository.current),
    ),
    profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.Factory(LocalSocialRepository.current),
    ),
) {
    val uiState by viewModel.uiState.collectAsState()
    val profileState by profileViewModel.uiState.collectAsState()
    val displayName = profileState.profile?.displayName.orEmpty()
    val tc = LocalTruckColors.current

    Scaffold(
        containerColor = BentoGlassTheme.ScreenBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.social_groups)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
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
                        Text(stringResource(R.string.social_join_by_code), color = tc.TextPrimary)
                        OutlinedTextField(
                            value = uiState.inviteCode,
                            onValueChange = viewModel::setInviteCode,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(R.string.social_invite_code_hint)) },
                            singleLine = true,
                        )
                        Button(
                            onClick = { viewModel.joinByCode(displayName) { onOpenChat(it) } },
                            enabled = uiState.inviteCode.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.social_join_group))
                        }
                    }
                }
            }
            item {
                Text(stringResource(R.string.social_public_groups), style = MaterialTheme.typography.titleMedium, color = tc.TextPrimary)
            }
            items(uiState.publicGroups, key = { it.id }) { group ->
                GroupDiscoverCard(
                    group = group,
                    onJoin = {
                        viewModel.joinGroup(group.id, displayName) { onOpenGroup(group.id) }
                    },
                    onOpen = { onOpenGroup(group.id) },
                )
            }
        }
    }
}

@Composable
private fun GroupDiscoverCard(
    group: SocialChat,
    onJoin: () -> Unit,
    onOpen: () -> Unit,
) {
    val tc = LocalTruckColors.current
    BentoGlassClickableCard(modifier = Modifier.fillMaxWidth(), onClick = onOpen) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${group.avatarEmoji} ${group.title}", style = MaterialTheme.typography.titleMedium, color = tc.TextPrimary)
            Text(group.description, style = MaterialTheme.typography.bodySmall, color = tc.TextSecondary)
            Text(
                "👥 ${group.participantCount} · ⭐ ${"%.1f".format(group.rating)}",
                style = MaterialTheme.typography.labelMedium,
                color = tc.TextSecondary,
            )
            if (!group.isMember) {
                Button(onClick = onJoin, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.social_join_group))
                }
            } else {
                Text(stringResource(R.string.social_challenge_joined), color = tc.AccentPrimary)
            }
        }
    }
}

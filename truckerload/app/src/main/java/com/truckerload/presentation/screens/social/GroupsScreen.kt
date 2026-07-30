package com.truckerload.presentation.screens.social

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Star
import com.truckerload.presentation.components.TlButton as Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.truckerload.R
import com.truckerload.domain.social.SocialChat
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
    viewModel: GroupsViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val displayName = profileState.profile?.displayName.orEmpty()
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
                title = { Text(stringResource(R.string.social_groups)) },
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
            if (uiState.recommendedGroups.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.social_recommended_groups),
                        style = MaterialTheme.typography.titleMedium,
                        color = tc.TextPrimary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(uiState.recommendedGroups, key = { "rec_${it.id}" }) { group ->
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.People,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(group.title, style = MaterialTheme.typography.titleMedium, color = tc.TextPrimary)
            }
            Text(group.description, style = MaterialTheme.typography.bodySmall, color = tc.TextSecondary)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.People,
                    contentDescription = null,
                    tint = tc.TextSecondary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    group.participantCount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = tc.TextSecondary,
                )
                Text("·", style = MaterialTheme.typography.labelMedium, color = tc.TextSecondary)
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = tc.TextSecondary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    "%.1f".format(group.rating),
                    style = MaterialTheme.typography.labelMedium,
                    color = tc.TextSecondary,
                )
            }
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

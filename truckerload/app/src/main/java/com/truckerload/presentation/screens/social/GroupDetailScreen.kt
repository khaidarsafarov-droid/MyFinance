package com.truckerload.presentation.screens.social

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlTextButton as TextButton
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.social.ChatMember
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.OneUiTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    chatId: String,
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    viewModel: GroupDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val chat = uiState.chat
    val tc = LocalTruckColors.current
    val snackbarHostState = remember { SnackbarHostState() }
    var descriptionDraft by remember(chat?.id, chat?.description) {
        mutableStateOf(chat?.description.orEmpty())
    }
    var showDelete by remember { mutableStateOf(false) }

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
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(OneUiTokens.CardGap),
                    ) {
                        if (uiState.isManager) {
                            OutlinedTextField(
                                value = descriptionDraft,
                                onValueChange = { descriptionDraft = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.social_group_description)) },
                                minLines = 2,
                                maxLines = 4,
                            )
                            Button(
                                onClick = { viewModel.updateDescription(descriptionDraft) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = descriptionDraft != chat?.description.orEmpty(),
                            ) {
                                Text(stringResource(R.string.common_save))
                            }
                        } else if (chat?.description.orEmpty().isNotBlank()) {
                            Text(chat?.description.orEmpty(), color = tc.TextSecondary)
                        }
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
                        if (uiState.isCreator) {
                            Button(
                                onClick = { showDelete = true },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.social_group_delete))
                            }
                        }
                    }
                }
            }
            item {
                Text(stringResource(R.string.social_members), style = MaterialTheme.typography.titleMedium, color = tc.TextPrimary)
            }
            items(uiState.members, key = { it.userId }) { member ->
                GroupMemberRow(
                    member = member,
                    canAssignModerator = uiState.isCreator && !member.isMe && member.role != "OWNER",
                    onMakeModerator = { viewModel.setModerator(member.userId) },
                )
            }
        }
    }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(stringResource(R.string.social_group_delete_title)) },
            text = { Text(stringResource(R.string.social_group_delete_message, chat?.title.orEmpty())) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDelete = false
                        viewModel.deleteGroup(onBack)
                    },
                ) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun GroupMemberRow(
    member: ChatMember,
    canAssignModerator: Boolean,
    onMakeModerator: () -> Unit,
) {
    val tc = LocalTruckColors.current
    BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (member.isMe) "${member.displayName} (${stringResource(R.string.social_you)})" else member.displayName,
                    modifier = Modifier.weight(1f),
                    color = if (member.isMe) tc.AccentPrimary else tc.TextPrimary,
                )
                val roleLabel = when (member.role) {
                    "OWNER" -> stringResource(R.string.social_group_owner)
                    "MODERATOR" -> stringResource(R.string.social_group_moderator)
                    else -> null
                }
                if (roleLabel != null) {
                    Text(roleLabel, color = tc.AccentPrimary, style = MaterialTheme.typography.labelSmall)
                }
            }
            if (canAssignModerator && member.role != "MODERATOR") {
                TextButton(onClick = onMakeModerator) {
                    Text(stringResource(R.string.social_group_make_moderator))
                }
            }
        }
    }
}

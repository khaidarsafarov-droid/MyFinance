package com.truckerload.presentation.screens.social.friends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.presentation.screens.social.friends.map.FriendRequestsSection
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton

@Composable
fun FriendsDirectoryPanel(
    viewModel: FriendsDirectoryViewModel,
    modifier: Modifier = Modifier,
    showFriendsList: Boolean = true,
    onOpenMap: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tc = LocalTruckColors.current
    BentoGlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!uiState.supabaseReady) {
                Text(
                    text = stringResource(R.string.friends_live_need_supabase),
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.AccentPrimary,
                )
            }
            FriendsAddByNicknameForm(
                searchQuery = uiState.searchQuery,
                searchBusy = uiState.searchBusy,
                searchHit = uiState.searchHit,
                searchNotFound = uiState.searchNotFound,
                statusMessage = uiState.statusMessage,
                onQueryChange = viewModel::setSearchQuery,
                onSearch = viewModel::searchFriend,
                onAdd = viewModel::addSearchedFriend,
            )
            FriendRequestsSection(
                incoming = uiState.incomingRequests,
                outgoing = uiState.outgoingRequests,
                onAccept = viewModel::acceptFriendRequest,
                onDecline = viewModel::declineFriendRequest,
                onCancel = viewModel::cancelFriendRequest,
            )
            if (showFriendsList) {
                Text(
                    text = stringResource(R.string.friends_accepted_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = tc.TextPrimary,
                )
                val friends = uiState.acceptedFriends
                if (friends.isEmpty()) {
                    Text(
                        text = stringResource(R.string.friends_accepted_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = tc.TextSecondary,
                    )
                } else {
                    friends.forEach { (_, label) ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = tc.TextPrimary,
                        )
                    }
                }
            }
            if (onOpenMap != null) {
                OutlinedButton(onClick = onOpenMap, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.friends_open_map))
                }
            }
        }
    }
}

package com.truckerload.presentation.screens.social.friends.map

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.friends.FriendShareLink
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.theme.AppFilterChipDefaults
import com.truckerload.presentation.theme.AppSwitchDefaults
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.LocalTruckColors

@Composable
fun FriendsMapManageSection(
    uiState: FriendsMapUiState,
    viewModel: FriendsMapViewModel,
    addFriendExpanded: Boolean,
    onAddFriendExpandedChange: (Boolean) -> Unit,
) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current

    // Collapse the add panel after a successful add.
    LaunchedEffect(uiState.statusMessage) {
        if (uiState.statusMessage in setOf("added", "request_sent", "accepted")) {
            onAddFriendExpandedChange(false)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!uiState.supabaseReady) {
            Text(
                text = stringResource(R.string.friends_live_need_supabase),
                style = MaterialTheme.typography.bodySmall,
                color = tc.AccentPrimary,
            )
        }

        AnimatedVisibility(visible = addFriendExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.friends_add_by_nickname_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = tc.TextPrimary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::setSearchQuery,
                        label = { Text(stringResource(R.string.friends_search_nickname_label)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = AppTextFieldDefaults.outlined(),
                    )
                    Button(
                        onClick = { viewModel.searchFriend() },
                        enabled = !uiState.searchBusy,
                    ) {
                        if (uiState.searchBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.PersonAdd, contentDescription = null)
                        }
                    }
                }
                uiState.searchHit?.let { hit ->
                    Text(
                        text = stringResource(R.string.friends_found, hit.nickname),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tc.TextPrimary,
                    )
                    Button(
                        onClick = { viewModel.addSearchedFriend() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.friends_request_send_button))
                    }
                }
                FriendRequestStatusText(uiState.statusMessage)
                if (uiState.searchNotFound || uiState.statusMessage == "not_found") {
                    Text(
                        text = stringResource(R.string.friends_not_in_app),
                        style = MaterialTheme.typography.bodySmall,
                        color = tc.TextSecondary,
                    )
                    OutlinedButton(
                        onClick = {
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    context.getString(R.string.friends_invite_share_text),
                                )
                            }
                            context.startActivity(
                                Intent.createChooser(
                                    share,
                                    context.getString(R.string.friends_invite_share_title),
                                ),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 6.dp),
                        )
                        Text(stringResource(R.string.friends_invite_share_button))
                    }
                }
            }
        }

        FriendRequestsSection(
            incoming = uiState.incomingRequests,
            outgoing = uiState.outgoingRequests,
            onAccept = viewModel::acceptFriendRequest,
            onDecline = viewModel::declineFriendRequest,
            onCancel = viewModel::cancelFriendRequest,
        )

        OutlinedButton(onClick = { viewModel.setShowOverlapsPanel(!uiState.showOverlapsPanel) }) {
            Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
            Text(stringResource(R.string.friends_overlap_button))
        }
        if (uiState.showOverlapsPanel) {
            if (uiState.overlaps.isEmpty()) {
                Text(
                    text = stringResource(R.string.friends_overlap_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary,
                )
            } else {
                uiState.overlaps.forEach { match ->
                    Text(
                        text = "${match.friendDisplayName}: ${match.reason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = tc.TextSecondary,
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.friends_sharing_list_title),
            style = MaterialTheme.typography.titleSmall,
            color = tc.TextPrimary,
        )
        if (uiState.shareLinks.isEmpty()) {
            Text(
                text = stringResource(R.string.friends_sharing_list_empty),
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
            )
        }
        uiState.shareLinks.forEach { link ->
            FriendShareRow(
                link = link,
                editing = uiState.editingFriendId == link.friendUserId,
                onEdit = { viewModel.setEditingFriend(link.friendUserId) },
                onCloseEdit = { viewModel.setEditingFriend(null) },
                onSavePrefs = { loc, route ->
                    viewModel.updateSharePrefs(link.friendUserId, loc, route)
                },
                onDelete = { viewModel.removeFriend(link.friendUserId) },
                onFocusMap = {
                    viewModel.selectFriend(link.friendUserId)
                    viewModel.toggleShowPath(link.friendUserId)
                },
            )
        }

        Text(
            text = stringResource(R.string.friends_list_title),
            style = MaterialTheme.typography.titleSmall,
            color = tc.TextPrimary,
        )
        if (uiState.friends.isEmpty()) {
            Text(
                text = stringResource(R.string.friends_list_empty),
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
            )
        }
        uiState.friends.forEach { friend ->
            val selected = friend.presence.userId == uiState.selectedFriendId
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = friend.presence.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selected) tc.AccentPrimary else tc.TextPrimary,
                )
                friend.route?.let { route ->
                    Text(
                        text = "${route.originLabel} → ${route.destinationLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = tc.TextSecondary,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = friend.showPath,
                        onClick = { viewModel.toggleShowPath(friend.presence.userId) },
                        label = {
                            Text(
                                if (friend.showPath) {
                                    stringResource(R.string.friends_hide_path)
                                } else {
                                    stringResource(R.string.friends_show_path)
                                },
                            )
                        },
                        colors = AppFilterChipDefaults.colors(),
                    )
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.selectFriend(friend.presence.userId) },
                        label = { Text(stringResource(R.string.friends_focus)) },
                        colors = AppFilterChipDefaults.colors(),
                    )
                }
            }
        }
    }
}

/** Compact + / close control for the manage-section header. */
@Composable
fun FriendsAddFriendHeaderButton(
    expanded: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = if (expanded) Icons.Default.Close else Icons.Default.Add,
            contentDescription = stringResource(
                if (expanded) R.string.friends_add_friend_close_cd else R.string.friends_add_friend_cd,
            ),
            tint = LocalTruckColors.current.TextPrimary,
        )
    }
}

@Composable
fun FriendShareRow(
    link: FriendShareLink,
    editing: Boolean,
    onEdit: () -> Unit,
    onCloseEdit: () -> Unit,
    onSavePrefs: (Boolean, Boolean) -> Unit,
    onDelete: () -> Unit,
    onFocusMap: () -> Unit,
) {
    val tc = LocalTruckColors.current
    var shareLoc by remember(link.friendUserId, link.shareMyLocation) { mutableStateOf(link.shareMyLocation) }
    var shareRoute by remember(link.friendUserId, link.shareMyRoute) { mutableStateOf(link.shareMyRoute) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "@${link.friendNickname.ifBlank { "Driver" }}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = tc.TextPrimary,
                )
                Text(
                    text = stringResource(
                        R.string.friends_share_summary,
                        if (link.shareMyLocation) "✓" else "—",
                        if (link.shareMyRoute) "✓" else "—",
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = tc.TextSecondary,
                )
            }
            IconButton(onClick = onFocusMap) {
                Icon(Icons.Default.Groups, contentDescription = stringResource(R.string.friends_show_path))
            }
            IconButton(onClick = { if (editing) onCloseEdit() else onEdit() }) {
                Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.friends_edit_share))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.friends_remove))
            }
        }
        if (editing) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.friends_pref_show_me), color = tc.TextPrimary)
                Switch(
                    checked = shareLoc,
                    onCheckedChange = { shareLoc = it },
                    colors = AppSwitchDefaults.colors(),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.friends_pref_show_route), color = tc.TextPrimary)
                Switch(
                    checked = shareRoute,
                    onCheckedChange = { shareRoute = it },
                    colors = AppSwitchDefaults.colors(),
                )
            }
            Button(
                onClick = {
                    onSavePrefs(shareLoc, shareRoute)
                    onCloseEdit()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.friends_prefs_save))
            }
        }
    }
}

@Composable
private fun FriendRequestStatusText(status: String?) {
    val message = when (status) {
        "request_sent" -> stringResource(R.string.friends_request_sent)
        "already_sent" -> stringResource(R.string.friends_request_already_sent)
        "already_friends" -> stringResource(R.string.friends_request_already_friends)
        "accepted" -> stringResource(R.string.friends_request_accepted)
        "blocked" -> stringResource(R.string.social_user_blocked)
        "added" -> stringResource(R.string.friends_added_ok)
        else -> return
    }
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = LocalTruckColors.current.AccentPrimary,
    )
}

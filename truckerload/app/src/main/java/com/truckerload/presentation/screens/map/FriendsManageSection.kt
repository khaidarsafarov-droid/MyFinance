package com.truckerload.presentation.screens.map

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.data.remote.SupabaseFriendsRealtimeService
import com.truckerload.domain.friends.FriendShareLink
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.theme.AppFilterChipDefaults
import com.truckerload.presentation.theme.AppSwitchDefaults
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.LocalTruckColors

@Composable
internal fun FriendsSharePathToggleRow(
    sharePathEnabled: Boolean,
    hasLocationPermission: Boolean,
    onSharePathChange: (Boolean) -> Unit,
    onRequestLocationPermission: () -> Unit,
) {
    val tc = LocalTruckColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.friends_share_path_toggle),
            style = MaterialTheme.typography.titleSmall,
            color = tc.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = sharePathEnabled,
            onCheckedChange = { enabled ->
                if (enabled && !hasLocationPermission) {
                    onRequestLocationPermission()
                }
                onSharePathChange(enabled)
            },
            colors = AppSwitchDefaults.colors(),
        )
    }
    if (!hasLocationPermission) {
        Text(
            text = stringResource(R.string.friends_need_location_permission),
            style = MaterialTheme.typography.bodySmall,
            color = tc.AccentPrimary,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
internal fun FriendsManageSectionHeader(
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val tc = LocalTruckColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.friends_manage_section),
                style = MaterialTheme.typography.titleSmall,
                color = tc.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = tc.TextSecondary,
            )
        }
    }
}

@Composable
internal fun FriendsManageSection(
    uiState: FriendsLiveMapUiState,
    viewModel: FriendsLiveMapViewModel,
    context: android.content.Context,
) {
    val tc = LocalTruckColors.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!uiState.supabaseReady) {
            Text(
                text = stringResource(R.string.friends_live_need_supabase),
                style = MaterialTheme.typography.bodySmall,
                color = tc.AccentPrimary,
            )
        }

        Text(
            text = stringResource(R.string.friends_my_nickname_title),
            style = MaterialTheme.typography.titleSmall,
            color = tc.TextPrimary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = uiState.nicknameDraft,
                onValueChange = viewModel::setNicknameDraft,
                label = { Text(stringResource(R.string.friends_nickname_label)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = AppTextFieldDefaults.outlined(),
            )
            Button(onClick = { viewModel.saveNickname() }) {
                Text(stringResource(R.string.friends_nickname_save))
            }
        }
        when (uiState.nicknameMessage) {
            "invalid" -> Text(
                stringResource(R.string.friends_nickname_invalid),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
            "saved", "saved_local" -> Text(
                stringResource(R.string.friends_nickname_saved),
                color = tc.AccentPrimary,
                style = MaterialTheme.typography.bodySmall,
            )
            SupabaseFriendsRealtimeService.ERROR_NICKNAME_SCHEMA_MISSING -> Text(
                stringResource(R.string.friends_nickname_schema_missing),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

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
                text = stringResource(R.string.friends_found, hit.displayName, hit.nickname),
                style = MaterialTheme.typography.bodyMedium,
                color = tc.TextPrimary,
            )
            Button(onClick = { viewModel.addSearchedFriend() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.friends_add_button))
            }
        }
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
                        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.friends_invite_share_text))
                    }
                    context.startActivity(
                        Intent.createChooser(share, context.getString(R.string.friends_invite_share_title)),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                Text(stringResource(R.string.friends_invite_share_button))
            }
        }

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

@Composable
private fun FriendShareRow(
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
                    text = "@${link.friendNickname}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = tc.TextPrimary,
                )
                Text(
                    text = link.friendDisplayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary,
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

package com.truckerload.presentation.screens.social.friends.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.friends.FriendLiveStatus
import com.truckerload.domain.friends.sortShareLinksByLiveStatus
import com.truckerload.presentation.theme.LocalTruckColors

@Composable
fun FriendsMapManageSection(
    uiState: FriendsMapUiState,
    viewModel: FriendsMapViewModel,
    manageExpanded: Boolean,
    onManageExpandedChange: (Boolean) -> Unit,
    onFocusFriend: (String) -> Unit,
) {
    val tc = LocalTruckColors.current
    val nowMillis = uiState.lastRefreshAt.takeIf { it > 0L } ?: System.currentTimeMillis()
    val overlayById = remember(uiState.friends) {
        uiState.friends.associateBy { it.presence.userId }
    }
    val sortedLinks = remember(uiState.shareLinks, overlayById, nowMillis) {
        sortShareLinksByLiveStatus(
            links = uiState.shareLinks,
            updatedAtByUserId = overlayById.mapValues { it.value.presence.updatedAtMillis },
            nowMillis = nowMillis,
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(14.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.friends_manage_section),
                    style = MaterialTheme.typography.titleSmall,
                    color = tc.TextPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onManageExpandedChange(!manageExpanded) }
                        .padding(vertical = 8.dp),
                )
                FriendsSectionOverflowMenu(
                    onOverlap = {
                        onManageExpandedChange(true)
                        viewModel.setShowOverlapsPanel(!uiState.showOverlapsPanel)
                    },
                )
                IconButton(onClick = { onManageExpandedChange(!manageExpanded) }) {
                    Icon(
                        if (manageExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = stringResource(R.string.friends_manage_section),
                        tint = tc.TextSecondary,
                    )
                }
            }
        }

        if (manageExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (uiState.showOverlapsPanel) {
                    Text(
                        text = stringResource(R.string.friends_overlap_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = tc.TextPrimary,
                    )
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
                Text(
                    text = stringResource(R.string.friends_sharing_list_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary,
                )
                if (sortedLinks.isEmpty()) {
                    Text(
                        text = stringResource(R.string.friends_sharing_list_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = tc.TextSecondary,
                    )
                }
                sortedLinks.forEach { link ->
                    val overlay = overlayById[link.friendUserId]
                    FriendShareRow(
                        link = link,
                        overlay = overlay,
                        liveStatus = FriendLiveStatus.fromUpdatedAt(
                            overlay?.presence?.updatedAtMillis,
                            nowMillis,
                        ),
                        selected = overlay?.presence?.userId == uiState.selectedFriendId,
                        editing = uiState.editingFriendId == link.friendUserId,
                        onEdit = { viewModel.setEditingFriend(link.friendUserId) },
                        onCloseEdit = { viewModel.setEditingFriend(null) },
                        onSavePrefs = { loc, route ->
                            viewModel.updateSharePrefs(link.friendUserId, loc, route)
                        },
                        onDelete = { viewModel.removeFriend(link.friendUserId) },
                        onTogglePath = { viewModel.toggleShowPath(link.friendUserId) },
                        onFocusMap = { onFocusFriend(link.friendUserId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FriendsSectionOverflowMenu(
    onOverlap: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menuOpen = true }) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.friends_section_menu_cd),
                tint = LocalTruckColors.current.TextSecondary,
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.friends_overlap_button)) },
                onClick = {
                    menuOpen = false
                    onOverlap()
                },
            )
        }
    }
}
